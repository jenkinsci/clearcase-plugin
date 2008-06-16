package hudson.plugins.clearcase;

import java.io.File;
import java.io.IOException;
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
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.TaggingAction;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
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
     * Create a SaveChangeLog action that is used to save a change log
     * @param launcher the command line launcher
     * @return an action that can save a change log to the Hudson changlog file
     */
    protected abstract SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher);

    /**
     * Create a ChangeLogAction that will be used to get the change logs for a CC repository
     * @param launcher the command line launcher
     * @return an action that returns the change logs for a CC repository
     */
    protected abstract ChangeLogAction createChangeLogAction(ClearToolLauncher launcher);
    
    /**
     * Create a TaggingAction that will be used at the end of the build to tag the CC repository
     * @param launcher the command line launcher
     * @return an action that can tag the CC repository; can be null.
     */
    protected abstract TaggingAction createTaggingAction(ClearToolLauncher clearToolLauncher);
    
    /**
     * Return string array containing the branch names that should be used when polling for changes.
     * @return a string array, can not be empty
     */
    public abstract String[] getBranchNames();
    
    /**
     * Return string array containing the paths in the view that should be used when polling for changes.
     * @param viewPath the file path for the view
     * @return string array that will be used by the lshistory command
     */
    public abstract String[] getViewPaths(FilePath viewPath) throws IOException, InterruptedException;

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
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
        
        // Create actions
        CheckOutAction checkoutAction = createCheckOutAction(clearToolLauncher);
        ChangeLogAction changeLogAction = createChangeLogAction(clearToolLauncher);
        SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);
        TaggingAction taggingAction = createTaggingAction(clearToolLauncher);
        
        // Checkout code
        checkoutAction.checkout(launcher, workspace);
        
        // Gather change log
        List<? extends ChangeLogSet.Entry> changelogEntries = null;        
        if (build.getPreviousBuild() != null) {
            Date lastBuildTime = build.getPreviousBuild().getTimestamp().getTime();
            changelogEntries = changeLogAction.getChanges(lastBuildTime, viewName, getBranchNames(), getViewPaths(workspace.child(viewName)));
        }        

        // Save change log
        if ((changelogEntries == null) || (changelogEntries.isEmpty())) {
            // no changes
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
        }        
        
        // Tag the build
        if (taggingAction != null) {
            // taggingAction.tag("lbl", "comment");
        }
        
        return true;
    }

    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) throws IOException, InterruptedException {
        Run lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            return true;
        } else {
            Date buildTime = lastBuild.getTimestamp().getTime();
            PollAction pollAction = createPollAction(createClearToolLauncher(listener, workspace, launcher));
            return pollAction.getChanges(buildTime, viewName, getBranchNames(), getViewPaths(workspace.child(viewName)));
        }
    }
    
    /**
     * Creates a Hudson clear tool launcher.
     * @param listener listener to write command output to 
     * @param workspace the workspace for the job
     * @param launcher actual launcher to launch commands with
     * @return a clear tool launcher that uses Hudson for launching commands 
     */
    protected ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
        return new HudsonClearToolLauncher(PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), 
                getDescriptor().getDisplayName(), listener, workspace, launcher);
    }
    
    protected ClearTool createClearTool(ClearToolLauncher launcher) {
        return new ClearToolSnapshot(launcher, mkviewOptionalParam);
    }
}
