/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase;

import static hudson.Util.fixEmpty;
import static hudson.Util.fixEmptyAndTrim;
import hudson.CopyOnWrite;
import hudson.Launcher;
import hudson.Util;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.DynamicCheckoutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.SnapshotCheckoutAction;
import hudson.plugins.clearcase.base.BaseHistoryAction;
import hudson.plugins.clearcase.base.BaseSaveChangeLogAction;
import hudson.plugins.clearcase.base.ClearCaseSCMRevisionState;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.history.LabelFilter;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.plugins.clearcase.viewstorage.ViewStorageFactory;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.framework.io.ByteBuffer;

/**
 * Base ClearCase SCM. This SCM is for base ClearCase repositories.
 *
 * @author Erik Ramfelt
 */

public class ClearCaseSCM extends AbstractClearCaseScm {

    private static final String DEFAULT_VALUE_WIN_DYN_STORAGE_DIR = "\\views\\dynamic";

    private String configSpec;
    private final String branch;
    private final String label;
    private boolean doNotUpdateConfigSpec;
    private boolean useTimeRule;

    @DataBoundConstructor
    public ClearCaseSCM(String branch, String label, String configspec, String viewTag, boolean useupdate, String loadRules, boolean usedynamicview, String viewdrive,
            String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename, String excludedRegions,
            String multiSitePollBuffer, boolean useTimeRule, boolean createDynView, String viewPath, ChangeSetLevel changeset, ViewStorageFactory viewStorageFactory) {
        super(viewTag, mkviewoptionalparam, filterOutDestroySubBranchEvent, (!usedynamicview) && useupdate, rmviewonrename, excludedRegions, usedynamicview,
                viewdrive, loadRules, multiSitePollBuffer, createDynView, false, false, viewPath, changeset, viewStorageFactory);
        this.branch = branch;
        this.label = label;
        this.configSpec = configspec;
        this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
        this.useTimeRule = useTimeRule;
    }

    public ClearCaseSCM(String branch, String label, String configspec, String viewTag, boolean useupdate, String loadRules, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename) {
        this(branch, label, configspec, viewTag, useupdate, loadRules, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, doNotUpdateConfigSpec, rmviewonrename, null, null, false, false, "viewpath", ChangeSetLevel.defaultLevel(), null);
    }

    public String getBranch() {
        return branch;
    }

    public String getLabel() {
        return label;
    }

    public String getConfigSpec() {
        return configSpec;
    }

    public boolean isDoNotUpdateConfigSpec() {
        return doNotUpdateConfigSpec;
    }

    public boolean isUseTimeRule() {
        return useTimeRule;
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
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        super.buildEnvVars(build, env);
        if (isUseDynamicView()) {
            if (getViewDrive() != null) {
                env.put(CLEARCASE_VIEWPATH_ENVSTR, getViewDrive() + File.separator + getNormalizedViewName());
            } else {
                env.remove(CLEARCASE_VIEWPATH_ENVSTR);
            }
        }
    }

    @Override
    protected CheckOutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        CheckOutAction action;
        String effectiveConfigSpec = Util.replaceMacro(configSpec, variableResolver);
        ViewStorage viewStorage = getViewStorageFactory().create(variableResolver, launcher.isUnix(), getViewName(variableResolver));
        if (isUseDynamicView()) {
            action = new DynamicCheckoutAction(createClearTool(variableResolver, launcher), effectiveConfigSpec, doNotUpdateConfigSpec, useTimeRule, isCreateDynView(),
                    viewStorage, build);
        } else {
            action = new SnapshotCheckoutAction(createClearTool(variableResolver, launcher),new ConfigSpec(effectiveConfigSpec, launcher.isUnix()), getViewPaths(variableResolver, build, launcher.getLauncher()),isUseUpdate(), getViewPath(variableResolver), viewStorage);
        }
        return action;
    }

    @Override
    protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        ClearTool ct = createClearTool(variableResolver, launcher);
        BaseHistoryAction action = new BaseHistoryAction(ct, isUseDynamicView(), configureFilters(variableResolver, build, launcher.getLauncher()), getDescriptor().getLogMergeTimeWindow());

        setExtendedViewPath(variableResolver, ct, action);

        return action;
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new BaseSaveChangeLogAction();
    }

    /**
     * Split the branch names into a string array.
     *
     * @param branchString string containing none or several branches
     * @return a string array (never empty)
     */
    @Override
    public String[] getBranchNames(VariableResolver<String> variableResolver) {
        // split by whitespace, except "\ "
        String[] branchArray = branch.split("(?<!\\\\)[ \\r\\n]+");
        // now replace "\ " to " ".
        for (int i = 0; i < branchArray.length; i++) {
            branchArray[i] = Util.replaceMacro(branchArray[i].replaceAll("\\\\ ", " "), variableResolver);
        }
        return branchArray;
    }

    public String[] getLabelNames(VariableResolver<String> variableResolver) {
        return Util.replaceMacro(label, variableResolver).split("\\s+");
    }

    @Override
    public Filter configureFilters(VariableResolver<String> variableResolver, AbstractBuild build, Launcher launcher) throws IOException, InterruptedException {
        Filter filter = super.configureFilters(variableResolver, build, launcher);
        if (StringUtils.isNotBlank(label)) {
            ArrayList<Filter> filters = new ArrayList<Filter>();
            filters.add(filter);
            filters.add(new LabelFilter(getLabelNames(variableResolver)));
            filter = new FilterChain(filters);
        }
        return filter;
    }

    /**
     * ClearCase SCM descriptor
     *
     * @author Erik Ramfelt
     */
    public static class ClearCaseScmDescriptor extends SCMDescriptor<ClearCaseSCM> implements ModelObject {
        private static final int DEFAULT_CHANGE_LOG_MERGE_TIME_WINDOW = 5;

        private int changeLogMergeTimeWindow = DEFAULT_CHANGE_LOG_MERGE_TIME_WINDOW;
        private String defaultViewName;
        private String defaultViewPath;
        private String defaultWinDynStorageDir;
        private String defaultUnixDynStorageDir;

        @CopyOnWrite
        private volatile ClearCaseInstallation[] installations = new ClearCaseInstallation[0];

        public ClearCaseScmDescriptor() {
            super(ClearCaseSCM.class, null);
            load();
        }

        public int getLogMergeTimeWindow() {
            return changeLogMergeTimeWindow;
        }

        public String getCleartoolExe() {
            String cleartoolExe;
            try {
                cleartoolExe = getCleartoolExe(Computer.currentComputer().getNode(), TaskListener.NULL);
            } catch (Exception e) {
                cleartoolExe = "cleartool";
            }
            return cleartoolExe;
        }

        public String getCleartoolExe(Node node, TaskListener listener) throws IOException, InterruptedException {
            return Hudson.getInstance().getDescriptorByType(ClearCaseInstallation.DescriptorImpl.class).getInstallation().getCleartoolExe(node, listener);
        }

        public String getDefaultViewName() {
            return StringUtils.defaultString(defaultViewName, "${USER_NAME}_${NODE_NAME}_${JOB_NAME}_hudson");
        }

        public String getDefaultViewPath() {
            return StringUtils.defaultString(defaultViewPath, "view");
        }

        public String getDefaultWinDynStorageDir() {
            if (defaultWinDynStorageDir == null) {
                return "\\\\${HOST}" + DEFAULT_VALUE_WIN_DYN_STORAGE_DIR;
            } else {
                return defaultWinDynStorageDir;
            }
        }

        public String getDefaultUnixDynStorageDir() {
            return defaultUnixDynStorageDir;
        }

        @Override
        public String getDisplayName() {
            return "Base ClearCase";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            defaultViewName = fixEmpty(req.getParameter("clearcase.defaultViewName").trim());
            defaultViewPath = fixEmpty(req.getParameter("clearcase.defaultViewPath").trim());
            defaultWinDynStorageDir = fixEmpty(req.getParameter("clearcase.defaultWinDynStorageDir").trim());
            defaultUnixDynStorageDir = fixEmpty(req.getParameter("clearcase.defaultUnixDynStorageDir").trim());

            String mergeTimeWindow = fixEmpty(req.getParameter("clearcase.logmergetimewindow"));
            if (mergeTimeWindow != null) {
                try {
                    changeLogMergeTimeWindow = DecimalFormat.getIntegerInstance().parse(mergeTimeWindow).intValue();
                } catch (ParseException e) {
                    changeLogMergeTimeWindow = DEFAULT_CHANGE_LOG_MERGE_TIME_WINDOW;
                }
            } else {
                changeLogMergeTimeWindow = DEFAULT_CHANGE_LOG_MERGE_TIME_WINDOW;
            }
            save();
            return true;
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ViewStorageFactory viewStorageFactory = req.bindJSON(ViewStorageFactory.class, formData.getJSONObject("viewStorage"));
            AbstractClearCaseScm scm = new ClearCaseSCM(
                                                        req.getParameter("cc.branch"),
                                                        req.getParameter("cc.label"),
                                                        req.getParameter("cc.configspec"),
                                                        req.getParameter("cc.viewname"),
                                                        req.getParameter("cc.useupdate") != null,
                                                        req.getParameter("cc.loadrules"),
                                                        req.getParameter("cc.usedynamicview") != null,
                                                        req.getParameter("cc.viewdrive"),
                                                        req.getParameter("cc.mkviewoptionalparam"),
                                                        req.getParameter("cc.filterOutDestroySubBranchEvent") != null,
                                                        req.getParameter("cc.doNotUpdateConfigSpec") != null,
                                                        req.getParameter("cc.rmviewonrename") != null,
                                                        req.getParameter("cc.excludedRegions"),
                                                        fixEmpty(req.getParameter("cc.multiSitePollBuffer")),
                                                        req.getParameter("cc.useTimeRule") != null,
                                                        req.getParameter("cc.createDynView") != null,
                                                        req.getParameter("cc.viewpath"),
                                                        ChangeSetLevel.fromString(req.getParameter("ucm.changeset")),
                                                        viewStorageFactory
                                                        );
            return scm;
        }

        /**
         * Checks if cleartool executable exists.
         */
        public FormValidation doCleartoolExeCheck(@QueryParameter final String value) throws IOException, ServletException {
            return FormValidation.validateExecutable(value);
        }

        /**
         * Validates the excludedRegions Regex
         */
        public FormValidation doExcludedRegionsCheck(@QueryParameter final String value) throws IOException, ServletException {
            String v = fixEmptyAndTrim(value);

            if (v != null) {
                String[] regions = v.split("[\\r\\n]+");
                for (String region : regions) {
                    try {
                        Pattern.compile(region);
                    } catch (PatternSyntaxException e) {
                        return FormValidation.error("Invalid regular expression. " + e.getMessage());
                    }
                }
            }
            return FormValidation.ok();
        }

        public FormValidation doConfigSpecCheck(@QueryParameter final String value) throws IOException, ServletException {
            String v = fixEmpty(value);
            if ((v == null) || (v.length() == 0)) {
                return FormValidation.error("Config spec is mandatory");
            }
            for (String cSpecLine : v.split("[\\r\\n]+")) {
                if (cSpecLine.startsWith("load ")) {
                    return FormValidation.error("Config spec can not contain load rules");
                }
            }
            // all tests passed so far
            return FormValidation.ok();
        }

        /**
         * Raises an error if the parameter value isnt set.
         *
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doMandatoryCheck(@QueryParameter final String value, @QueryParameter final String errorText) throws IOException, ServletException {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(fixEmpty(errorText));
            }
            // all tests passed so far
            return FormValidation.ok();
        }

        /**
         * Displays "cleartool -version" for trouble shooting.
         */
        public void doVersion(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            ByteBuffer baos = new ByteBuffer();
            try {
                Hudson.getInstance().createLauncher(TaskListener.NULL).launch().cmds(getCleartoolExe(), "-version").stdout(baos).join();
                rsp.setContentType("text/plain");
                baos.writeTo(rsp.getOutputStream());
            } catch (IOException e) {
                req.setAttribute("error", e);
                rsp.forward(this, "versionCheckError", req);
            }
        }

        public void doListViews(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Hudson.getInstance().createLauncher(TaskListener.NULL).launch().cmds(getCleartoolExe(), "lsview", "-short").stdout(baos).join();

            rsp.setContentType("text/plain");
            rsp.getOutputStream().println("ClearCase Views found:\n");
            baos.writeTo(rsp.getOutputStream());
        }

        public ClearCaseInstallation[] getInstallations() {
            return this.installations;
        }

        public void setInstallations(ClearCaseInstallation[] installations) {
            this.installations = installations;
            save();
        }
    }

    @Override
    protected boolean isFirstBuild(SCMRevisionState baseline) {
        return baseline == null || !(baseline instanceof ClearCaseSCMRevisionState);
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
        return createRevisionState(build, launcher, taskListener, build.getTime());
    }

    @Override
    public SCMRevisionState calcRevisionsFromPoll(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
        return createRevisionState(build, launcher, taskListener, new Date());
    }

    private AbstractClearCaseSCMRevisionState createRevisionState(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener, Date date) throws IOException, InterruptedException {
        ClearCaseSCMRevisionState revisionState = new ClearCaseSCMRevisionState(date);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        revisionState.setLoadRules(getViewPaths(variableResolver, build, launcher));
        return revisionState;
    }
}
