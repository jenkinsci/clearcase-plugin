package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.Run;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ByteBuffer;
import hudson.util.FormFieldValidator;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import hudson.util.ForkOutputStream;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ClearCase SCM.
 * 
 * This SCM uses the cleartool to update and get the change log.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseSCM extends SCM {

    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
    public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";

    private String branch;
    private boolean useUpdate;
    private String configSpec;
    private String viewName;
    private String vobPaths;
    private boolean useDynamicView;
    private String viewDrive;
    private String mkviewOptionalParam;

    private transient ClearToolFactory clearToolFactory;

    public ClearCaseSCM(ClearToolFactory clearToolFactory, String branch, String configSpec, String viewName, boolean useUpdate,
            String vobPaths, boolean useDynamicView, String viewDrive, String mkviewOptionalParam) {
        this.clearToolFactory = clearToolFactory;
        this.branch = branch;
        this.configSpec = configSpec;
        this.viewName = viewName;
        this.useUpdate = useUpdate;
        this.useDynamicView = useDynamicView;
        this.viewDrive = viewDrive;
        this.vobPaths = vobPaths;
        this.mkviewOptionalParam = mkviewOptionalParam;

        if (this.useDynamicView) {
            this.useUpdate = false;
        }
    }

    public ClearCaseSCM(String branch, String configSpec, String viewName, boolean useUpdate, String vobPaths,
            boolean useDynamicView, String viewDrive, String mkviewOptionalParam) {
        this(null, branch, configSpec, viewName, useUpdate, vobPaths, useDynamicView, viewDrive, mkviewOptionalParam);
    }

    // Get methods
    public String getBranch() {
        return branch;
    }

    public String getConfigSpec() {
        return configSpec;
    }

    public String getViewName() {
        if (viewName == null) {
            return "hudson_view";
        } else {
            return viewName;
        }
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public String getVobPaths() {
        return vobPaths;
    }

    public boolean isUseDynamicView() {
        return useDynamicView;
    }

    public String getViewDrive() {
        return viewDrive;
    }
    
    public String getMkviewOptionalParam() {
        return mkviewOptionalParam;
    }

    @Override
    public ClearCaseScmDescriptor getDescriptor() {
        return PluginImpl.DESCRIPTOR;
    }

    @Override
    public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
        if (viewName != null)
            env.put(CLEARCASE_VIEWNAME_ENVSTR, viewName);
        
        if (useDynamicView) {
            if (viewDrive != null)
                env.put(CLEARCASE_VIEWPATH_ENVSTR, viewDrive + File.separator + viewName);
        } else {
            String workspace = env.get("WORKSPACE");
            if (workspace != null) {
                env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator + viewName);
            }
        }
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
            File changelogFile) throws IOException, InterruptedException {
        if (clearToolFactory == null) {
            clearToolFactory = new ClearToolFactoryImpl();
        }
        ChangeLogEntryMerger merger = clearToolFactory.createChangeLogEntryMerger(this);
        ClearTool cleartool = clearToolFactory.create(this, listener);
        if (cleartool == null) {
            return false;
        }
        
        cleartool.setVobPaths(vobPaths);

        ClearToolLauncher ctLauncher = new ClearToolLauncherImpl(listener, workspace, launcher);

        boolean updateView = useUpdate;
        if (!useDynamicView) {
            boolean localViewPathExists = new FilePath(workspace, viewName).exists();
            
            if (localViewPathExists) {
                if (updateView) {
                    String currentConfigSpec = cleartool.catcs(ctLauncher, viewName).trim();
                    if (!configSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
                        updateView = false;
                    }
                }
                if (!updateView) {
                    cleartool.rmview(ctLauncher, viewName);
                    localViewPathExists = false;
                }                
            }

            if (!localViewPathExists) {
                cleartool.mkview(ctLauncher, viewName);
                String tempConfigSpec = configSpec;
                if (launcher.isUnix()) {
                    tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
                }
                cleartool.setcs(ctLauncher, viewName, tempConfigSpec);
                updateView = false;
            }

            if (updateView) {
                if (updateView) {
                    cleartool.update(ctLauncher, viewName);
                }
            }
        } else {
            String currentConfigSpec = cleartool.catcs(ctLauncher, viewName).trim();
            if (!configSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
                String tempConfigSpec = configSpec;
                if (launcher.isUnix()) {
                    tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
                }
                cleartool.setcs(ctLauncher, viewName, tempConfigSpec);
            }
        }

        List<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();
        if (build.getPreviousBuild() != null) {
            Date time = build.getPreviousBuild().getTimestamp().getTime();
            for (String branchName : getBranchNames(branch)) {
                history.addAll(cleartool.lshistory(ctLauncher,
                        time, viewName, branchName));
            }
        }

        if (history.isEmpty()) {
            // nothing to compare against, or no changes
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            FileOutputStream fileOutputStream = new FileOutputStream(changelogFile);
            ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, merger.getMergedList(history));
            return true;
        }
    }
    
    private String[] getBranchNames(String branchString) {
        // split by whitespace, except "\ "
        String[] branchArray = branchString.split("(?<!\\\\)[ \\r\\n]+");
        // now replace "\ " to " ".
        for (int i = 0; i < branchArray.length; i++)
            branchArray[i] = branchArray[i].replaceAll("\\\\ ", " ");
        return branchArray;
    }

    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException {

        if (clearToolFactory == null) {
            clearToolFactory = new ClearToolFactoryImpl();
        }
        ClearTool cleartool = clearToolFactory.create(this, listener);
        if (cleartool == null) {
            return false;
        }

        Run lastBuild = project.getLastBuild();
        if (lastBuild == null) {
            return true;
        } else {
            cleartool.setVobPaths(vobPaths);
            ClearToolLauncher ctLauncher = new ClearToolLauncherImpl(listener, workspace, launcher);
            Date buildTime = lastBuild.getTimestamp().getTime();
            for (String branchName : getBranchNames(branch)) {
                List<ClearCaseChangeLogEntry> data = cleartool.lshistory(ctLauncher, buildTime, viewName,
                        branchName);
                if (!data.isEmpty()) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ClearCaseChangeLogParser();
    }

    final static class ClearToolLauncherImpl implements ClearToolLauncher {

        private final TaskListener listener;
        private final FilePath workspace;
        private final Launcher launcher;

        public ClearToolLauncherImpl(TaskListener listener, FilePath workspace, Launcher launcher) {
            this.listener = listener;
            this.workspace = workspace;
            this.launcher = launcher;
        }

        public TaskListener getListener() {
            return listener;
        }

        public FilePath getWorkspace() {
            return workspace;
        }

        public boolean run(String[] cmd, InputStream inputStream, OutputStream outputStream, FilePath filePath) throws IOException,
                InterruptedException {
            OutputStream out = outputStream;
            FilePath path = filePath;
            String[] env = new String[0];

            if (path == null) {
                path = workspace;
            }

            if (out == null) {
                out = listener.getLogger();
            } else {
                out = new ForkOutputStream(out, listener.getLogger());
            }

            int r = launcher.launch(cmd, env, inputStream, out, path).join();
            if (r != 0) {
                StringBuilder builder = new StringBuilder();
                for (String cmdParam : cmd) {
                    if (builder.length() > 0) {
                        builder.append(" ");
                    }
                    builder.append(cmdParam);
                }
                listener.fatalError(PluginImpl.getDescriptor().getDisplayName() + " failed. exit code=" + r);
                throw new IOException("cleartool did not return the expected exit code. Command line=\""
                        + builder.toString() + "\", actual exit code=" + r);
            }
            return r == 0;
        }
    }

    /**
     * ClearCase SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static final class ClearCaseScmDescriptor extends SCMDescriptor<ClearCaseSCM> 
            implements ModelObject {
        private String cleartoolExe;
        private int changeLogMergeTimeWindow = 5;

        protected ClearCaseScmDescriptor() {
            super(ClearCaseSCM.class, null);
            load();
        }

        public int getLogMergeTimeWindow() {
            return changeLogMergeTimeWindow;
        }

        public String getCleartoolExe() {
            if (cleartoolExe == null) {
                return "cleartool";
            } else {
                return cleartoolExe;
            }
        }

        @Override
        public String getDisplayName() {
            return "ClearCase";
        }

        @Override
        public boolean configure(StaplerRequest req) {
            cleartoolExe = fixEmpty(req.getParameter("clearcase.cleartoolExe").trim());
            String mergeTimeWindow = fixEmpty(req.getParameter("clearcase.logmergetimewindow"));
            if (mergeTimeWindow != null) {
                try {
                    changeLogMergeTimeWindow = DecimalFormat.getIntegerInstance().parse(mergeTimeWindow).intValue();
                } catch (ParseException e) {
                    changeLogMergeTimeWindow = 5;
                }
            } else {
                changeLogMergeTimeWindow = 5;
            }
            save();
            return true;
        }

        @Override
        public SCM newInstance(StaplerRequest req) throws FormException {
            ClearCaseSCM scm = new ClearCaseSCM(req.getParameter("clearcase.branch"), req
                    .getParameter("clearcase.configspec"), req.getParameter("clearcase.viewname"), req
                    .getParameter("clearcase.useupdate") != null, req.getParameter("clearcase.vobpaths"), req
                    .getParameter("clearcase.usedynamicview") != null, req.getParameter("clearcase.viewdrive"),
                    req.getParameter("clearcase.mkviewoptionalparam"));
            return scm;
        }

        /**
         * Checks if cleartool executable exists.
         */
        public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req, rsp).process();
        }

        public void dologMergeTimeWindowCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                @Override
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if (v == null) {
                        error("Merge time window is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }
        
        public void doViewNameCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                @Override
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if (v == null) {
                        error("View name is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }

        public void doConfigSpecCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                @Override
                protected void check() throws IOException, ServletException {
                    
                    String v = fixEmpty(request.getParameter("value"));
                    if ((v == null) || (v.length() == 0)) {
                        error("Config spec is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }

        /**
         * Displays "cleartool -version" for trouble shooting.
         */
        public void doVersion(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException,
                InterruptedException {
            ByteBuffer baos = new ByteBuffer();
            try {
                Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL).launch(
                        new String[] { getCleartoolExe(), "-version" }, new String[0], baos, null);
                proc.join();
                rsp.setContentType("text/plain");
                baos.writeTo(rsp.getOutputStream());
            } catch (IOException e) {
                req.setAttribute("error", e);
                rsp.forward(this, "versionCheckError", req);
            }
        }
    }
    private static class ClearToolFactoryImpl implements ClearToolFactory {
        public ClearTool create(ClearCaseSCM scm, TaskListener listener) {
            ClearTool clearTool = null;
            String clearToolStr = scm.getDescriptor().getCleartoolExe();
            if ((clearToolStr == null) || (clearToolStr.length() == 0)) {
                listener.fatalError("No cleartool executable is configured.");
            } else {
                if (scm.useDynamicView) {
                    clearTool = new ClearToolDynamic(clearToolStr, scm.viewDrive);
                    listener.getLogger().println("Creating a dynamic cleartool");
                } else {
                    clearTool = new ClearToolSnapshot(clearToolStr, scm.mkviewOptionalParam);
                    listener.getLogger().println("Creating a snapshot cleartool");
                }
            }
            return clearTool;
        }

        public ChangeLogEntryMerger createChangeLogEntryMerger(ClearCaseSCM scm) {
            return new ChangeLogEntryMerger(scm.getDescriptor().getLogMergeTimeWindow() * 1000);
        }        
    }
}
