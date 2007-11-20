package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Clear case SCM.
 * 
 * This SCM uses the cleartool to update and get the change log.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseSCM extends SCM {

    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";

    private String branch;
    private boolean useUpdate;
    private String configSpec;
    private String viewName;
    private String vobPaths;
    private boolean useDynamicView;
    private String viewDrive;

    private transient ClearTool clearTool;

    public ClearCaseSCM(ClearTool clearTool, String branch, String configSpec, String viewName, boolean useUpdate,
            String vobPaths, boolean useDynamicView, String viewDrive) {
        this.clearTool = clearTool;
        this.branch = branch;
        this.configSpec = configSpec;
        this.viewName = viewName;
        this.useUpdate = useUpdate;
        this.useDynamicView = useDynamicView;
        this.viewDrive = viewDrive;
        this.vobPaths = vobPaths;

        if (this.useDynamicView) {
            this.useUpdate = false;
        }
    }

    public ClearCaseSCM(String branch, String configSpec, String viewName, boolean useUpdate, String vobPaths,
            boolean useDynamicView, String viewDrive) {
        this(null, branch, configSpec, viewName, useUpdate, vobPaths, useDynamicView, viewDrive);
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

    @Override
    public ClearCaseScmDescriptor getDescriptor() {
        return PluginImpl.DESCRIPTOR;
    }

    @Override
    public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
        if (viewName != null)
            env.put(CLEARCASE_VIEWNAME_ENVSTR, viewName);
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
            File changelogFile) throws IOException, InterruptedException {

        getClearTool(listener).setVobPaths(vobPaths);

        ClearToolLauncher ctLauncher = new ClearToolLauncherImpl(listener, workspace, launcher);

        boolean updateView = useUpdate;
        if (!useDynamicView) {
            boolean localViewPathExists = new FilePath(workspace, viewName).exists();
            if ((!updateView) && localViewPathExists) {
                getClearTool(listener).rmview(ctLauncher, viewName);
                localViewPathExists = false;
            }

            if (!localViewPathExists) {
                getClearTool(listener).mkview(ctLauncher, viewName);
                getClearTool(listener).setcs(ctLauncher, viewName, configSpec);
                updateView = false;
            }

            if (updateView) {
                getClearTool(listener).update(ctLauncher, viewName);
            }
        } else {
            getClearTool(listener).setcs(ctLauncher, viewName, configSpec);
        }

        List<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();
        if (build.getPreviousBuild() != null) {
            history.addAll(getClearTool(listener).lshistory(ctLauncher,
                    build.getPreviousBuild().getTimestamp().getTime(), viewName, branch));
        }

        if (history.isEmpty()) {
            // nothing to compare against, or no changes
            return createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            FileOutputStream fileOutputStream = new FileOutputStream(changelogFile);
            ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, history);
            return true;
        }
    }

    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException {

        Build lastBuild = (Build) project.getLastBuild();
        if (lastBuild == null) {
            return true;
        } else {
            getClearTool(listener).setVobPaths(vobPaths);
            ClearToolLauncher ctLauncher = new ClearToolLauncherImpl(listener, workspace, launcher);
            Date buildTime = lastBuild.getTimestamp().getTime();
            List<ClearCaseChangeLogEntry> data = getClearTool(listener).lshistory(ctLauncher, buildTime, viewName,
                    branch);
            return !data.isEmpty();
        }
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ClearCaseChangeLogParser();
    }

    public ClearTool getClearTool(TaskListener listener) {
        if (clearTool == null) {
            if (useDynamicView) {
                clearTool = new ClearToolDynamic(getDescriptor().cleartoolExe, viewDrive);
                listener.getLogger().println("Creating a dynamic clear tool");
            } else {
                clearTool = new ClearToolSnapshot(getDescriptor().cleartoolExe);
                listener.getLogger().println("Creating a snapshot clear tool");
            }
        }
        return clearTool;
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

        public boolean run(String[] cmd, InputStream in, OutputStream out, FilePath path) throws IOException,
                InterruptedException {
            String[] env = new String[0];

            if (path == null) {
                path = workspace;
            }

            if (out == null) {
                out = listener.getLogger();
            } else {
                out = new ForkOutputStream(out, listener.getLogger());
            }

            int r = launcher.launch(cmd, env, in, out, path).join();
            if (r != 0) {
                StringBuilder builder = new StringBuilder();
                for (String cmdParam : cmd) {
                    if (builder.length() > 0) {
                        builder.append(" ");
                    }
                    builder.append(cmdParam);
                }
                listener.fatalError(PluginImpl.getDescriptor().getDisplayName() + " failed. exit code=" + r);
                throw new IOException("Clear tool did not return the expected exit code. Command line=\""
                        + builder.toString() + "\", actual exit code=" + r);
            }
            return r == 0;
        }
    }

    /**
     * Clear case SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static final class ClearCaseScmDescriptor extends SCMDescriptor<ClearCaseSCM> implements ModelObject {
        private String cleartoolExe;

        protected ClearCaseScmDescriptor() {
            super(ClearCaseSCM.class, null);
            load();
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
            return "Clear Case";
        }

        @Override
        public boolean configure(StaplerRequest req) {
            cleartoolExe = fixEmpty(req.getParameter("clearcase.cleartoolExe").trim());
            save();
            return true;
        }

        @Override
        public SCM newInstance(StaplerRequest req) throws FormException {
            ClearCaseSCM scm = new ClearCaseSCM(req.getParameter("clearcase.branch"), req
                    .getParameter("clearcase.configspec"), req.getParameter("clearcase.viewname"), req
                    .getParameter("clearcase.useupdate") != null, req.getParameter("clearcase.vobpaths"), req
                    .getParameter("clearcase.usedynamicview") != null, req.getParameter("clearcase.viewdrive"));
            return scm;
        }

        /**
         * Checks if clear tool executable exists.
         */
        public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req, rsp).process();
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
            System.out.println("doConfigSpecCheck");
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
}
