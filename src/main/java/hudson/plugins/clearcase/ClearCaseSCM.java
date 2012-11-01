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
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.action.BaseDynamicCheckoutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.BaseSnapshotCheckoutAction;
import hudson.plugins.clearcase.base.BaseHistoryAction;
import hudson.plugins.clearcase.base.BaseSaveChangeLogAction;
import hudson.plugins.clearcase.base.ClearCaseSCMRevisionState;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.history.LabelFilter;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.plugins.clearcase.viewstorage.ViewStorageFactory;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.ArgumentListBuilder;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
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
    public static final String CLEARCASE_CSFILENAME_ENVSTR = "CLEARCASE_CSFILENAME";

    private boolean extractConfigSpec;
    private String configSpecFileName;
    private boolean refreshConfigSpec;
    private String refreshConfigSpecCommand;
    private final String branch;
    private String configSpec;
    private final String label;
    private boolean doNotUpdateConfigSpec;
    private boolean useTimeRule;

    @DataBoundConstructor
    public ClearCaseSCM(String branch, String label, boolean extractConfigSpec, String configSpecFileName, boolean refreshConfigSpec, String refreshConfigSpecCommand, String configSpec,
    		String viewTag, boolean useupdate, boolean extractLoadRules, String loadRules, boolean useOtherLoadRulesForPolling, String loadRulesForPolling, boolean usedynamicview, String viewdrive,
            String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename, String excludedRegions,
            String multiSitePollBuffer, boolean useTimeRule, boolean createDynView, String viewPath, ChangeSetLevel changeset, ViewStorageFactory viewStorageFactory) {
        super(viewTag, mkviewoptionalparam, filterOutDestroySubBranchEvent, (!usedynamicview) && useupdate, rmviewonrename, excludedRegions, usedynamicview,
              viewdrive, extractLoadRules, loadRules, useOtherLoadRulesForPolling, loadRulesForPolling, multiSitePollBuffer, createDynView, false, false, viewPath, changeset, viewStorageFactory);
        this.branch = branch;
        this.label = label;
        this.extractConfigSpec = extractConfigSpec;
        this.configSpecFileName = configSpecFileName;
        this.refreshConfigSpec = refreshConfigSpec;
        this.refreshConfigSpecCommand = refreshConfigSpecCommand;
        this.configSpec = configSpec;
        this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
        this.useTimeRule = useTimeRule;
    }

    public ClearCaseSCM(String branch, String label, String configspec, String viewTag, boolean useupdate, String loadRules, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
    		boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename) {
        this(branch, label, false, null, false, null, configspec, viewTag, useupdate, false, loadRules, false, null, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent,
                doNotUpdateConfigSpec, rmviewonrename, null, null, false, false, "viewpath", ChangeSetLevel.defaultLevel(), null);
    }

    @SuppressWarnings("deprecation")
    private boolean doRefreshConfigSpec(VariableResolver<String> variableResolver, Launcher launcher) {
        int cmdResult = 1;
        // execute refresh command
        if (isRefreshConfigSpec()) {
            ArgumentListBuilder cmd = new ArgumentListBuilder();
            cmd.addTokenized(getRefreshConfigSpecCommand(variableResolver));
            PrintStream logger = launcher.getListener().getLogger();
            try {
                cmdResult = launcher.launch(cmd.toCommandArray(), new String[0], null, logger, null).join();
            } catch (IOException e) {
                e.printStackTrace(logger);
            } catch (InterruptedException e) {
                e.printStackTrace(logger);
            }
        }
        return (cmdResult == 0);
    }

    private boolean doExtractConfigSpec(VariableResolver<String> variableResolver, Launcher launcher) {
        boolean ret = true;
        // get config spec from file
        if (isExtractConfigSpec()) {
            String cs = null;
            cs = getConfigSpecFromFile(getConfigSpecFileName(variableResolver), launcher);
            if (cs != null) {
                configSpec = cs;
            } else {
                launcher.getListener().getLogger().println("Fall back to config spec field...");
                ret = false;
            }
        }
        return ret;
    }

    private boolean doExtractLoadRules(VariableResolver<String> variableResolver, Launcher launcher) {
        boolean ret = true;
        // extract rules from config spec
        if (isExtractLoadRules()) {
            ConfigSpec cfgSpec = new ConfigSpec(configSpec, launcher.isUnix());
            setLoadRules(cfgSpec.getLoadRulesString());
        }
        return ret;
    }

    private String getConfigSpecFromFile(String fileName, Launcher launcher) {
        String cs = null;
        try {
            cs = PathUtil.readFileAsString(fileName);
        } catch (IOException e) {
            launcher.getListener().getLogger().println("ERROR: Cannot open config spec file '" + fileName + "'");
        }
        return cs;
    }

    public String getBranch() {
        return branch;
    }

    public String getLabel() {
        return label;
    }

    public boolean isExtractConfigSpec() {
        return extractConfigSpec;
    }

    public String getConfigSpecFileName() {
        return configSpecFileName;
    }

    public String getConfigSpecFileName(VariableResolver<String> variableResolver) {
        if (variableResolver != null) {
            return Util.replaceMacro(configSpecFileName, variableResolver);
        }
        return configSpecFileName;
    }

    public boolean isRefreshConfigSpec() {
        return refreshConfigSpec;
    }

    public String getRefreshConfigSpecCommand() {
        return refreshConfigSpecCommand;
    }

    public String getRefreshConfigSpecCommand(VariableResolver<String> variableResolver) {
        if (variableResolver != null) {
            return Util.replaceMacro(refreshConfigSpecCommand, variableResolver);
        }
        return refreshConfigSpecCommand;
    }

    public String getConfigSpec() {
        return configSpec;
    }

    public String setConfigSpec(String scpec) {
        return configSpec = scpec;
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

    /**
     * Adds the env variable for the ClearCase SCMs.
     * <ul>
     * <li>CLEARCASE_CSFILENAME - The name of the clearcase config spec file.</li>
     * </ul>
     */
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
        if (isExtractConfigSpec()) {
            @SuppressWarnings("unchecked")
            VariableResolver.Union<String> variableResolver = new VariableResolver.Union<String>(new BuildVariableResolver(build, true),
                                                                                                 new VariableResolver.ByMap<String>(env));
        	env.put(CLEARCASE_CSFILENAME_ENVSTR, getConfigSpecFileName(variableResolver));
        }
    }

    @Override
    protected void inspectConfigAction(VariableResolver<String> variableResolver, ClearToolLauncher cclauncher) throws IOException, InterruptedException {
        Launcher launcher = cclauncher.getLauncher();
        doRefreshConfigSpec(variableResolver, launcher);
        doExtractConfigSpec(variableResolver, launcher);
        doExtractLoadRules(variableResolver, launcher);
        // verify
        PrintStream logger = launcher.getListener().getLogger();
        if (StringUtils.isEmpty(configSpec)) {
            logger.println("[WARNING] config spec is empty!");
        }
        if (StringUtils.isEmpty(getLoadRules())) {
            logger.println("[WARNING] load rules are empty!");
        }
    }

    @Override
    protected boolean hasNewConfigSpec(VariableResolver<String> variableResolver, ClearToolLauncher cclauncher) throws IOException, InterruptedException {
        if (useTimeRule) {
            // Disable this feature if time rule is enabled, since the config spec changes all the time
            return false;
        }
        boolean ret = false;
        Launcher launcher = cclauncher.getLauncher();
        // get user configured config spec
        ConfigSpec actualConfigSpec = new ConfigSpec(configSpec, launcher.isUnix());
        // get real view config spec
        ClearTool cleartool = createClearTool(variableResolver, cclauncher);
        PrintStream logger = launcher.getListener().getLogger();
        logger.println("*************************** get view CSPEC ***********************");
        ConfigSpec viewConfigSpec = new ConfigSpec(cleartool.catcs(getViewName(variableResolver)), launcher.isUnix());
        logger.println("******************************************************************");
        // first check configured (expected or default) config spec with CATCS
        boolean configuredCSEqualsCatcs = actualConfigSpec.stripLoadRules().equals(viewConfigSpec.stripLoadRules());
        if (!configuredCSEqualsCatcs) {
            logger.println("[WARNING] CSPEC configured != catcs (view)");
            ret = true;
        }
        if (isExtractConfigSpec()) {
            // refresh if needed
            doRefreshConfigSpec(variableResolver, launcher);
            // get config spec from file
            String cs = getConfigSpecFromFile(getConfigSpecFileName(variableResolver), launcher);
            if (cs != null) {
                // get new config spec
                ConfigSpec fileConfigSpec = new ConfigSpec(cs, launcher.isUnix());
                ret = !viewConfigSpec.stripLoadRules().equals(fileConfigSpec.stripLoadRules());
                logger.println("[INFO] CSPEC changed = " + ret);
                if (ret) {
                    String viewCS = viewConfigSpec.stripLoadRules().getRaw();
                    String fileCS = fileConfigSpec.stripLoadRules().getRaw();
                    logger.println("*** CATCS CSPEC ***");
                    logger.println(viewCS);
                    logger.println("******************************************************************");
                    logger.println("***  FILE CSPEC ***");
                    logger.println(fileCS);
                    logger.println("******************************************************************");
                }
            } else {
                logger.println("ERROR: extracted CSPEC is empty!");
                ret = false;
            }
        }
        return ret;
    }

    @Override
    protected CheckoutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) throws IOException, InterruptedException {
        CheckoutAction action;
        String effectiveConfigSpec = Util.replaceMacro(configSpec, variableResolver);
        ViewStorage viewStorage = getViewStorageFactory().create(variableResolver, launcher.isUnix(), getViewName(variableResolver));
        if (isUseDynamicView()) {
            action = new BaseDynamicCheckoutAction(createClearTool(variableResolver, launcher), effectiveConfigSpec, doNotUpdateConfigSpec, useTimeRule, isCreateDynView(),
                    viewStorage, build);
        } else {
            action = new BaseSnapshotCheckoutAction(createClearTool(variableResolver, launcher),new ConfigSpec(effectiveConfigSpec, launcher.getLauncher().isUnix()), getViewPaths(variableResolver, build, launcher.getLauncher(), false),isUseUpdate(), getViewPath(variableResolver), viewStorage);
        }
        return action;
    }

    @Override
    protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build, boolean useRecurse) throws IOException, InterruptedException {
        ClearTool ct = createClearTool(variableResolver, launcher);
        BaseHistoryAction action = new BaseHistoryAction(ct, isUseDynamicView(), configureFilters(variableResolver, build, launcher.getLauncher()), getChangeset(), useRecurse, getDescriptor().getLogMergeTimeWindow(), getUpdtFileName());

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
            String instanceId = Hudson.getInstance().getDisplayName();
            return StringUtils.defaultString(defaultViewName, instanceId +"_${USER_NAME}_${NODE_NAME}_${JOB_NAME}${DASH_WORKSPACE_NUMBER}");
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
                                                        req.getParameter("cc.getConfigSpecFromFile") != null,
                                                        req.getParameter("cc.configSpecFileName"),
                                                        req.getParameter("cc.refreshConfigSpec") != null,
                                                        req.getParameter("cc.refreshConfigSpecCommand"),
                                                        req.getParameter("cc.configspec"),
                                                        req.getParameter("cc.viewname"),
                                                        req.getParameter("cc.useupdate") != null,
                                                        req.getParameter("cc.extractLoadRules") != null,
                                                        req.getParameter("cc.loadrules"),
                                                        req.getParameter("cc.useOtherLoadRulesForPolling") != null,
                                                        req.getParameter("cc.loadRulesForPolling"),
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
                                                        ChangeSetLevel.fromString(req.getParameter("cc.changeset")),
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
            if (v == null) {
                return FormValidation.error("Config spec is mandatory");
            }
            /*
            for (String cSpecLine : v.split("[\\r\\n]+")) {
                if (cSpecLine.startsWith("load ")) {
                    return FormValidation.error("Config spec can not contain load rules");
                }
            }
            */
            // all tests passed so far
            return FormValidation.ok();
        }

        /*
        public FormValidation doConfigSpecFileNameCheck(@QueryParameter final String value) throws IOException, ServletException {
            String v = fixEmpty(value);
            if (v == null) {
                return FormValidation.error("Config spec file is mandatory");
            }
            // all tests passed so far
            return FormValidation.ok();
        }

        public FormValidation doLoadRulesCheck(@QueryParameter final String value) throws IOException, ServletException {
            String v = fixEmpty(value);
            if (v == null) {
                return FormValidation.error("Load rules are mandatory");
            }
            // all tests passed so far
            return FormValidation.ok();
        }
        */

        /**
         * Raises an error if the parameter value isnt set.
         *
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doMandatoryCheck(@QueryParameter final String value, @QueryParameter final String errorText) throws IOException, ServletException {
            String v = fixEmpty(value);
            if (v == null) {
                return FormValidation.error(errorText);
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
    	return createRevisionState(build, launcher, taskListener, getBuildTime(build));
    }

    @Override
    public SCMRevisionState calcRevisionsFromPoll(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException {
        return createRevisionState(build, launcher, taskListener, new Date());
    }

    private AbstractClearCaseSCMRevisionState createRevisionState(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener, Date date) throws IOException, InterruptedException {
        ClearCaseSCMRevisionState revisionState = new ClearCaseSCMRevisionState(date);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        revisionState.setLoadRules(getViewPaths(variableResolver, build, launcher, true));
        return revisionState;
    }
}
