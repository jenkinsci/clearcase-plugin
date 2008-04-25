package hudson.plugins.clearcase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;

/**
 * Abstract class for ClearCase SCM.
 * The class contains the logic around checkout and polling, the deriving
 * classes only have to implement the specific checkout and polling logic.
 */
public abstract class AbstractClearCaseScm extends SCM {
    
    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
    public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";

    private final String viewName;
    private final String mkviewOptionalParam;
    
    public AbstractClearCaseScm(String viewName, String mkviewOptionalParam) {
        this.viewName = viewName;
        this.mkviewOptionalParam = mkviewOptionalParam;
    }
    
    /**
     * Create a CheckOutAction that will be used by the checkout method.
     * @param launcher the command line launcher
     * @return an action that can check out code from a ClearCase repository.
     */
    protected abstract CheckOutAction createCheckOutAction(ClearToolLauncher launcher);
    
    /**
     * Create a PollAction that will be used by the pollChanges() method.
     * @param launcher the command line launcher
     * @return an action that can poll if there are any changes a ClearCase repository.
     */
    protected abstract PollAction createPollAction(ClearToolLauncher launcher);
    
    /**
     * Return string array containing the branch names that should be used when polling for changes.
     * @return a string array, can not be empty
     */
    public abstract String[] getBranchNames();
    
    /**
     * Return string containing the vob paths that should be used when polling for changes.
     * @return string that will be appended at the end of the lshistory command
     */
    public abstract String getVobPaths();

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ClearCaseChangeLogParser();
    }

    @Override
    public boolean supportsPolling() {
        return true;
    }

    public String getViewName() {
        if (viewName == null) {
            return "hudson_view";
        } else {
            return viewName;
        }
    }

    /**
     * Returns the user configured optional params that will be used in when creating a new view.
     * @return string containing optional mkview parameters.
     */
    public String getMkviewOptionalParam() {
        return mkviewOptionalParam;
    }
    
    /**
     * Adds the env variable for the ClearCase SCMs.
     * CLEARCASE_VIEWNAME - The name of the clearcase view.
     * CLEARCASE_VIEWPATH - The absolute path to the clearcase view.
     */
    @Override
    public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
        if (viewName != null) {
            env.put(CLEARCASE_VIEWNAME_ENVSTR, viewName);
        
            String workspace = env.get("WORKSPACE");
            if (workspace != null) {
                env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator + viewName);
            }
        }
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException, InterruptedException {
        return checkout(build, launcher, workspace, listener, changelogFile, PluginImpl.BASE_DESCRIPTOR.getLogMergeTimeWindow());
    }
    
    /**
     * Creates a Hudson clear tool launcher.
     * @param listener listener to write command output to 
     * @param workspace the workspace for the job
     * @param launcher actual launcher to launch commands with
     * @return a clear tool launcher that uses Hudson for launching commands 
     */
    protected ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
        return new HudsonClearToolLauncher(getDescriptor().getDisplayName(), listener, workspace, launcher);
    }

    protected boolean checkout(AbstractBuild<?,?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile, int mergeTimeWindow) throws IOException, InterruptedException {
         
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
        CheckOutAction checkOutAction = createCheckOutAction(clearToolLauncher);
        PollAction pollAction = createPollAction(clearToolLauncher);
        
        checkOutAction.checkout(launcher, workspace);
        
        List<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();
        if (build.getPreviousBuild() != null) {
            Date time = build.getPreviousBuild().getTimestamp().getTime();
            for (String branchName : getBranchNames()) {
                history.addAll(pollAction.getChanges(time, viewName, branchName, getVobPaths()));
            }
        }

        if (history.isEmpty()) {
            // nothing to compare against, or no changes
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            FileOutputStream fileOutputStream = new FileOutputStream(changelogFile);
            ChangeLogEntryMerger entryMerger = new ChangeLogEntryMerger(mergeTimeWindow * 1000);
            ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, entryMerger.getMergedList(history));
            return true;
        }
    }

    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        Run lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            return true;
        } else {
            Date buildTime = lastBuild.getTimestamp().getTime();
            PollAction pollAction = createPollAction(createClearToolLauncher(listener, workspace, launcher));
            for (String branchName : getBranchNames()) {
                List<ClearCaseChangeLogEntry> data = pollAction.getChanges(buildTime, viewName, branchName, getVobPaths());
                if (!data.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }
}
