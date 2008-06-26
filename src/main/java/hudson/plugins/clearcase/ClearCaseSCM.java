package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Proc;
import hudson.Util;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.DefaultPollAction;
import hudson.plugins.clearcase.action.DynamicCheckoutAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.SnapshotCheckoutAction;
import hudson.plugins.clearcase.action.TaggingAction;
import hudson.plugins.clearcase.base.BaseChangeLogAction;
import hudson.plugins.clearcase.base.BaseSaveChangeLogAction;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ByteBuffer;
import hudson.util.FormFieldValidator;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import javax.servlet.ServletException;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Base ClearCase SCM.
 * 
 * This SCM is for base ClearCase repositories.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseSCM extends AbstractClearCaseScm {

    private boolean useUpdate;
    private String configSpec;
    private boolean useDynamicView;
    private String viewDrive;
    private final String branch;
    private final String vobPaths;

    @DataBoundConstructor
    public ClearCaseSCM(String branch, String configspec, String viewname, boolean useupdate, String vobpaths,
            boolean usedynamicview, String viewdrive, String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent) {
        super(viewname, mkviewoptionalparam, filterOutDestroySubBranchEvent);
        this.branch = branch;
        this.configSpec = configspec;
        this.useUpdate = useupdate;
        this.vobPaths = vobpaths;
        this.useDynamicView = usedynamicview;
        this.viewDrive = viewdrive;

        if (this.useDynamicView) {
            this.useUpdate = false;
        }
    }

    public String getBranch() {
        return branch;
    }

    public String getConfigSpec() {
        return configSpec;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public boolean isUseDynamicView() {
        return useDynamicView;
    }

    public String getViewDrive() {
        return viewDrive;
    }

    public Object getVobPaths() {
        return vobPaths;
    }

    /**
     * Return the view paths that will be used when getting changes for a view.
     * If the user configured vob paths field is empty, then the folder within the view will be used
     * as view paths.
     * @return the view paths that will be used when getting changes for a view.
     */
    public String[] getViewPaths(FilePath viewPath) throws IOException, InterruptedException {
        String[] vobNameArray;
        if (Util.fixEmpty(vobPaths.trim()) == null) {
            List<String> vobList = new ArrayList<String>();
            List<FilePath> subFilePaths = viewPath.list((FileFilter) null);
            if ((subFilePaths != null) && (subFilePaths.size() > 0)) {

                for (int i = 0; i < subFilePaths.size(); i++) {
                    if (subFilePaths.get(i).isDirectory()) {
                        vobList.add(subFilePaths.get(i).getName());
                    }
                }
            }
            vobNameArray = vobList.toArray(new String[0]);
        } else {
            // split by whitespace, except "\ "
            vobNameArray = vobPaths.split("(?<!\\\\)[ \\r\\n]+");
            // now replace "\ " to " ".
            for (int i = 0; i < vobNameArray.length; i++)
                vobNameArray[i] = vobNameArray[i].replaceAll("\\\\ ", " ");
        }
        return vobNameArray;
    }
   

    @Override
    public ClearCaseScmDescriptor getDescriptor() {
        return PluginImpl.BASE_DESCRIPTOR;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ClearCaseChangeLogParser();
    }

    @Override
    public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
        super.buildEnvVars(build, env);

        if (useDynamicView) {
            if (viewDrive != null) {
                env.put(CLEARCASE_VIEWPATH_ENVSTR, viewDrive + File.separator + getNormalizedViewName(build.getProject()));
            } else {
                env.remove(CLEARCASE_VIEWPATH_ENVSTR);
            }
        } 
    }

    @Override
    protected CheckOutAction createCheckOutAction(ClearToolLauncher launcher) {
        CheckOutAction action;
        if (useDynamicView) {
            action = new DynamicCheckoutAction(createClearTool(launcher), configSpec);
        } else {
            action = new SnapshotCheckoutAction(createClearTool(launcher), configSpec, useUpdate);
        }
        return action;
    }

    @Override
    protected PollAction createPollAction(ClearToolLauncher launcher) {
        return new DefaultPollAction(createClearTool(launcher));
    }

    @Override
    protected BaseChangeLogAction createChangeLogAction(ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        return createChangeLogAction(launcher, build, getDescriptor().getLogMergeTimeWindow());
    }
    
    protected BaseChangeLogAction createChangeLogAction(ClearToolLauncher launcher, AbstractBuild<?, ?> build, int logMergeTimeWindow) {
        BaseChangeLogAction action = new BaseChangeLogAction(createClearTool(launcher), logMergeTimeWindow);
        if (useDynamicView) {
            String extendedViewPath = viewDrive;
            if (! (viewDrive.endsWith("\\") && viewDrive.endsWith("/"))) {
                // Need to deteremine what kind of char to add in between
                if (viewDrive.contains("/")) {
                    extendedViewPath += "/";
                } else {
                    extendedViewPath += "\\";
                }                
            }
            extendedViewPath += getNormalizedViewName(build.getProject());
            action.setExtendedViewPath(extendedViewPath);
        }
        return action;
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new BaseSaveChangeLogAction();
    }

    @Override
    protected TaggingAction createTaggingAction(ClearToolLauncher clearToolLauncher) {
        return null;
    }

    /**
     * Split the branch names into a string array.
     * @param branchString string containing none or several branches
     * @return a string array (never empty)
     */
    @Override
    public String[] getBranchNames() {
        // split by whitespace, except "\ "
        String[] branchArray = branch.split("(?<!\\\\)[ \\r\\n]+");
        // now replace "\ " to " ".
        for (int i = 0; i < branchArray.length; i++)
            branchArray[i] = branchArray[i].replaceAll("\\\\ ", " ");
        return branchArray;
    }

    protected ClearTool createClearTool(ClearToolLauncher launcher) {
        if (useDynamicView) {
            return new ClearToolDynamic(launcher, viewDrive);
        } else {
            return super.createClearTool(launcher);
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
            return "Base ClearCase";
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
            ClearCaseSCM scm = new ClearCaseSCM(
                    req.getParameter("cc.branch"), 
                    req.getParameter("cc.configspec"), 
                    req.getParameter("cc.viewname"), 
                    req.getParameter("cc.useupdate") != null, 
                    req.getParameter("cc.vobpaths"), 
                    req.getParameter("cc.usedynamicview") != null, 
                    req.getParameter("cc.viewdrive"),
                    req.getParameter("cc.mkviewoptionalparam"),
                    req.getParameter("cc.filterOutDestroySubBranchEvent") != null);
            return scm;
        }

        /**
         * Checks if cleartool executable exists.
         */
        public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req, rsp).process();
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
         * Raises an error if the parameter value isnt set.
         * @param req containing the parameter value and the errorText to display if the value isnt set
         * @param rsp
         * @throws IOException
         * @throws ServletException
         */
        public void doMandatoryCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                @Override
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if (v == null) {
                        error(fixEmpty(request.getParameter("errorText")));
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
