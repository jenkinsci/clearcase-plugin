/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.plugins.clearcase.viewstorage.ViewStorageFactory;
import hudson.scm.ChangeLogSet;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Abstract class for ClearCase SCM. The class contains the logic around checkout and polling, the deriving classes only
 * have to implement the specific checkout and polling logic.
 */
public abstract class AbstractClearCaseScm extends SCM {

    /**
	 * The change set level describes which level of details will be in the changeset
	 */
    public enum ChangeSetLevel {
    	/**
    	 * No changeset will be generated
    	 */
		NONE("no"),
		/**
		 * Changeset will be generated based only on changes done in current branch
		 */
		BRANCH("branch"),
		/**
		 * Changeset will be generated based only on changes logged in updt file (snapshot views)
		 */
		UPDT("updt"),
		/**
		 * Changeset will be generated based on changes done in current branch, and changes due to rebase
		 */
		ALL("all");

		private String name;
		private ChangeSetLevel(String name) {
			this.name = name;
		}
		public static ChangeSetLevel fromString(String str) {
			for (ChangeSetLevel csl : values()) {
				if (csl.name.equals(str)) {
					return csl;
				}
			}
			return ChangeSetLevel.defaultLevel();
		}

		public String getName() {
		    return name;
		}
		public static ChangeSetLevel defaultLevel() {
			return ChangeSetLevel.BRANCH;
		}
	}
    /**
     * Regular Expression Search String for Whitespace.
     */
    protected static final String REGEX_WHITESPACE = "[\\s\\\\\\/:\\?\\*\\|]+";


	public static final String CLEARCASE_VIEWTAG_ENVSTR = "CLEARCASE_VIEWTAG";
    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
    public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";

    private String viewName;
    private String mkviewOptionalParam;
    private boolean filteringOutDestroySubBranchEvent;
    private transient ThreadLocal<String> normalizedViewName;
    private transient ThreadLocal<String> normalizedViewPath;
    private boolean useUpdate;
    private boolean removeViewOnRename;
    private String excludedRegions;
    private boolean extractLoadRules;
    private String loadRules;
    private boolean useOtherLoadRulesForPolling;
    private String loadRulesForPolling;
    private boolean useDynamicView;
    private String viewDrive;
    private int multiSitePollBuffer;
    private boolean createDynView;
    /**
     * Deprecated.
     * @see ViewStorageFactory
     */
    @Deprecated
    private String winDynStorageDir;
    /**
     * Deprecated.
     * @see ViewStorageFactory
     */
    @Deprecated
    private String unixDynStorageDir;
    private boolean freezeCode;
    private boolean recreateView;
    private String viewPath;
    private ChangeSetLevel changeset;
    private String updtFileName;
    private ViewStorageFactory viewStorageFactory;

	private synchronized ThreadLocal<String> getNormalizedViewNameThreadLocalWrapper() {
        if (null == normalizedViewName) {
            this.normalizedViewName = new ThreadLocal<String>();
        }
        return this.normalizedViewName;
    }

    private synchronized ThreadLocal<String> getNormalizedViewPathThreadLocalWrapper() {
        if (normalizedViewPath == null) {
            normalizedViewPath = new ThreadLocal<String>();
        }
        return normalizedViewPath;
    }

    protected void setNormalizedViewName(String normalizedViewName) {
        getNormalizedViewNameThreadLocalWrapper().set(normalizedViewName);
    }

    protected String getNormalizedViewName() {
        return getNormalizedViewNameThreadLocalWrapper().get();
    }

    protected void setNormalizedViewPath(String normalizedViewPath) {
        getNormalizedViewPathThreadLocalWrapper().set(normalizedViewPath);
    }

    protected String getNormalizedViewPath() {
        return getNormalizedViewPathThreadLocalWrapper().get();
    }


    public AbstractClearCaseScm(final String viewName, final String mkviewOptionalParam, final boolean filterOutDestroySubBranchEvent, final boolean useUpdate,
            final boolean rmviewonrename, final String excludedRegions, final boolean useDynamicView, final String viewDrive, boolean extractLoadRules,
            final String loadRules, final boolean useOtherLoadRulesForPolling, final String loadRulesForPolling, final String multiSitePollBuffer,
            final boolean createDynView, final boolean freezeCode, final boolean recreateView, final String viewPath, ChangeSetLevel changeset, ViewStorageFactory viewStorageFactory) {
        Validate.notNull(viewName);
        this.viewName = viewName;
        this.mkviewOptionalParam = mkviewOptionalParam;
        this.filteringOutDestroySubBranchEvent = filterOutDestroySubBranchEvent;
        this.useUpdate = useUpdate;
        this.removeViewOnRename = rmviewonrename;
        this.excludedRegions = excludedRegions;
        this.useDynamicView = useDynamicView;
        this.viewDrive = viewDrive;
        this.extractLoadRules = extractLoadRules;
        this.loadRules = loadRules;
        this.useOtherLoadRulesForPolling = useOtherLoadRulesForPolling;
        this.loadRulesForPolling = loadRulesForPolling;
        if (multiSitePollBuffer != null) {
            try {
                this.multiSitePollBuffer = DecimalFormat.getIntegerInstance().parse(multiSitePollBuffer).intValue();
            } catch (ParseException e) {
                this.multiSitePollBuffer = 0;
            }
        } else {
            this.multiSitePollBuffer = 0;
        }
        this.createDynView = createDynView;
        this.freezeCode = freezeCode;
        this.recreateView = recreateView;
        this.viewPath = StringUtils.defaultIfEmpty(viewPath, viewName);
        this.changeset = changeset;
        this.viewStorageFactory = viewStorageFactory;
    }

    /**
     * Create a CheckOutAction that will be used by the checkout method.
     *
     * @param launcher the command line launcher
     * @return an action that can check out code from a ClearCase repository.
     * @throws InterruptedException
     * @throws IOException
     */
    protected abstract CheckoutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) throws IOException, InterruptedException;

    /**
     * inspect config action that will be used by the checkout method.
     *
     * @param variableResolver
     * @param launcher
     * @return void.
     * @throws InterruptedException
     * @throws IOException
     */
    protected void inspectConfigAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher)
			throws IOException, InterruptedException {
	  }

    /**
     * Create a HistoryAction that will be used by the pollChanges() and checkout() method.
     *
     * @param variableResolver
     * @param useDynamicView
     * @param launcher the command line launcher
     * @return an action that can poll if there are any changes a ClearCase repository. Never null.
     * @throws InterruptedException
     * @throws IOException
     */
    protected abstract HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build, boolean useRecurse) throws IOException, InterruptedException;

    /**
     * Create a SaveChangeLog action that is used to save a change log
     *
     * @param launcher the command line launcher
     * @return an action that can save a change log to the Hudson changlog file
     */
    protected abstract SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher);

    /**
     * Return string array containing the branch names that should be used when polling for changes.
     *
     * @return a string array, can not be empty
     * @deprecated use {@link #getBranchNames(VariableResolver)} instead
     */
    @Deprecated
    public String[] getBranchNames(){
        return getBranchNames(new VariableResolver.ByMap<String>(new HashMap<String, String>()));
    }

    /**
     * Return string array containing the branch names that should be used when polling for changes.
     *
     * @return a string array, can not be empty
     */
    public abstract String[] getBranchNames(VariableResolver<String> variableResolver);

    /**
     * Return string array containing the paths in the view that should be used when polling for changes.
     * @param variableResolver [anb0s: HUDSON-8497]
     * @param build TODO
     * @param launcher TODO
     * @return string array that will be used by the lshistory command and for constructing the config spec, etc.
     * @throws InterruptedException
     * @throws IOException
     */
    public String[] getViewPaths(VariableResolver<String> variableResolver, AbstractBuild build, Launcher launcher) throws IOException, InterruptedException {
    	return getViewPaths(variableResolver, build, launcher, false);
    }

    /**
     * Return string array containing the paths in the view that should be used when polling for changes.
     * @param variableResolver [anb0s: HUDSON-8497]
     * @param build TODO
     * @param launcher TODO
     * @param forPolling used for polling?
     * @return string array that will be used by the lshistory command and for constructing the config spec, etc.
     * @throws InterruptedException
     * @throws IOException
     */
    public String[] getViewPaths(VariableResolver<String> variableResolver, AbstractBuild build, Launcher launcher, boolean forPolling) throws IOException, InterruptedException {
        String loadRules = getLoadRules(variableResolver, forPolling);
        if (StringUtils.isBlank(loadRules)) {
            return null;
        }

        String[] rules = loadRules.split("[\\r\\n]+");
        for (int i = 0; i < rules.length; i++) {
            String rule = rules[i];
            // Remove "load " from the string, just in case.
            rule = StringUtils.removeStart(rule, "load ");
            // Remove "\\", "\" or "/" from the load rule. (bug#1706) Only if
            // the view is not dynamic
            // the user normally enters a load rule beginning with those chars
            rule = StringUtils.stripStart(rule, "\\/");
            rules[i] = rule;
        }
        return rules;
    }

    public ViewStorageFactory getViewStorageFactory() {
        if (viewStorageFactory == null) {
            viewStorageFactory = ViewStorageFactory.getDefault();
        }
        return viewStorageFactory;
    }

    public boolean isUseDynamicView() {
        return useDynamicView;
    }

    public int getMultiSitePollBuffer() {

        return multiSitePollBuffer;
    }

    public String getViewDrive() {
        return viewDrive;
    }

    public boolean isExtractLoadRules() {
        return extractLoadRules;
    }

    public String getLoadRules() {
        return loadRules;
    }

    public String getLoadRules(VariableResolver<String> variableResolver) {
        return Util.replaceMacro(loadRules, variableResolver);
    }

    public void setLoadRules(String ldRls) {
        loadRules = ldRls;
    }

    public boolean isUseOtherLoadRulesForPolling() {
    	return useOtherLoadRulesForPolling;
    }

    public String getLoadRulesForPolling() {
    	return loadRulesForPolling;
    }

    public String getLoadRules(VariableResolver<String> variableResolver, boolean forPolling) {
    	if (useOtherLoadRulesForPolling && forPolling)
    		return Util.replaceMacro(loadRulesForPolling, variableResolver);
    	else
    		return Util.replaceMacro(loadRules, variableResolver);
    }

    public boolean isCreateDynView() {
        return createDynView;
    }

    @Override
    public boolean supportsPolling() {
        return true;
    }

    @Override
    public boolean requiresWorkspaceForPolling() {
        return true;
    }

    @Override
    public FilePath getModuleRoot(FilePath workspace) {
        return getModuleRoot(workspace,null);
    }

    public FilePath getModuleRoot(FilePath workspace, AbstractBuild build) {
        if (useDynamicView) {
            String normViewName = getNormalizedViewName( );
            return new FilePath(workspace.getChannel(), viewDrive).child(normViewName);
        } else {
            String normViewPath = getNormalizedViewPath();
            if (normViewPath != null) {
                return workspace.child(normViewPath);
            } else {
                if(build == null) {
                    normViewPath = getViewPath();
                } else {
                    normViewPath = getViewPath(new BuildVariableResolver(build));
                }
                if (normViewPath != null) {
                    return workspace.child(normViewPath);
                } else {
                    // Should never happen, because viewName must not be null, and if viewpath is null, then it is made equal to viewName
                    throw new IllegalStateException("View path name cannot be null. There is a bug inside AbstractClearCaseScm.");
                }
            }
        }
    }

    public String getViewName() {
        return viewName;
    }

    public String getViewName(VariableResolver<String> variableResolver) {
        String normalized = null;
        String v = getViewName();
        if (v != null) {
            normalized = Util.replaceMacro(v, variableResolver).replaceAll(REGEX_WHITESPACE, "_");
            setNormalizedViewName(normalized);
        }
        return normalized;
    }

    public boolean isFreezeCode() {
        return freezeCode;
    }

    public boolean isRecreateView() {
        return recreateView;
    }

    /**
     * Returns the current computer - used in constructor for BuildVariableResolver in place of direct call to
     * Computer.currentComputer() so we can mock it in unit tests.
     */
    public Computer getCurrentComputer() {
        return Computer.currentComputer();
    }

    /**
     * Returns the computer a given build ran on. We wrap this here for mocking purposes.
     */
    public Computer getBuildComputer(AbstractBuild<?, ?> build) {
        return build.getBuiltOn().toComputer();
    }

    /**
     * @see AbstractClearCaseScm#generateNormalizedViewName(BuildVariableResolver, String)
     * @param build the project to get the name from
     * @return a string containing no invalid chars.
     */
    public String generateNormalizedViewName(AbstractBuild<?, ?> build) {
        return generateNormalizedViewName(new BuildVariableResolver(build));
    }

    /**
     * <p>
     * Returns a normalized view name that will be used in cleartool commands.
     * </p>
     * It will replace :
     * <ul>
     * <li>${NODE_NAME} with the name of the node</li>
     * <li>${JOB_NAME} with the name of the job</li>
     * <li>${USER_NAME} with the name of the user</li>
     * </ul>
     * This way it will be easier to add new jobs without trying to find an unique view name. It will also replace
     * invalid chars from a view name.
     *
     * @param build the project to get the name from
     * @return a string containing no invalid chars.
     */
    public String generateNormalizedViewName(VariableResolver<String> variableResolver, String modViewName) {
        String generatedNormalizedViewName = Util.replaceMacro(modViewName, variableResolver);

        generatedNormalizedViewName = generatedNormalizedViewName.replaceAll(REGEX_WHITESPACE, "_");

        setNormalizedViewName(generatedNormalizedViewName);
        return generatedNormalizedViewName;
    }

    /**
     * @see AbstractClearCaseScm#generateNormalizedViewName(BuildVariableResolver, String)
     * @param variableResolver An initialized build variable resolver.
     * @return a string containing no invalid chars.
     */

    public String generateNormalizedViewName(VariableResolver<String> variableResolver) {
        return generateNormalizedViewName(variableResolver, viewName);
    }

    /**
     * Returns the user configured optional params that will be used in when creating a new view.
     *
     * @return string containing optional mkview parameters.
     */
    public String getMkviewOptionalParam() {
        return mkviewOptionalParam;
    }

    /**
     * <p>
     * Returns if the "Destroyed branch" event should be filtered out or not.<br/>
     * For more information about the boolean, see the full discussion at <a
     * href="http://www.nabble.com/ClearCase-build-triggering-td17507838i20.html">Nabble</a>.
     * </p>
     * <p>
     * "Usually, CC admins have a CC trigger, fires on an uncheckout event, that destroys empty branches."
     * </p>
     *
     * @return true if the "Destroyed branch" event should be filtered out or not; false otherwise
     */
    public boolean isFilteringOutDestroySubBranchEvent() {
        return filteringOutDestroySubBranchEvent;
    }

    /**
     * Adds the env variable for the ClearCase SCMs.
     * <ul>
     * <li>CLEARCASE_VIEWTAG  - The clearcase view tag.</li>
     * <li>CLEARCASE_VIEWNAME - The name of the clearcase view folder (relative to workspace).</li>
     * <li>CLEARCASE_VIEWPATH - The absolute path to the clearcase view.</li>
     * </ul>
     */
    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, Map<String, String> env) {
        @SuppressWarnings("unchecked")
        VariableResolver.Union<String> variableResolver = new VariableResolver.Union<String>(new BuildVariableResolver(build, true),
                                                                                             new VariableResolver.ByMap<String>(env));
        String normalizedViewName = getViewName(variableResolver);
        String normalizedViewPath = getViewPath(variableResolver);
        if (normalizedViewName != null) {
            env.put(CLEARCASE_VIEWTAG_ENVSTR, normalizedViewName);
        }
        if (normalizedViewPath != null) {
            env.put(CLEARCASE_VIEWNAME_ENVSTR, normalizedViewPath);
            if(isUseDynamicView()) {
                env.put(CLEARCASE_VIEWPATH_ENVSTR, viewDrive + File.separator + normalizedViewPath);
            } else {
                String workspace = env.get("WORKSPACE");
                if (workspace != null) {
                    env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator + normalizedViewPath);
                }
            }
        }
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException,
            InterruptedException {
        boolean returnValue = true;
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        // inspect config spec
        inspectConfigAction(variableResolver, clearToolLauncher);

        // Calculate revision state from the beginning, it will enable to reuse load rules
        build.addAction(calcRevisionsFromBuild(build, launcher, listener));

        // Create actions
        CheckoutAction checkoutAction = createCheckOutAction(variableResolver, clearToolLauncher, build);
        SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);
        build.addAction(new ClearCaseDataAction());

        // get normalized view name
        String coNormalizedViewName = getViewName(variableResolver);

        // changelog actions
        boolean computeChangeLogBeforeCheckout = false;
        boolean computeChangeLogAfterCheckout = false;
        // check if previous build there
        if (build.getPreviousBuild() != null) {
            // We need a valid view to determine the change log. For instance, on a new slave the view won't exist
            boolean isViewValid = checkoutAction.isViewValid(workspace, coNormalizedViewName);
            if (isViewValid) {
                if (!ChangeSetLevel.UPDT.equals(changeset)) {
                    computeChangeLogBeforeCheckout = true;
                } else {
                    computeChangeLogAfterCheckout = true;
                }
            } else {
                computeChangeLogAfterCheckout = !ChangeSetLevel.UPDT.equals(changeset);
            }
        }

    	PrintStream logger = listener.getLogger();
       	logger.println("[INFO] computeChangeLogBeforeCheckout = " + computeChangeLogBeforeCheckout);
       	logger.println("[INFO] computeChangeLogAfterCheckout  = " + computeChangeLogAfterCheckout);

        if (computeChangeLogBeforeCheckout) {
            returnValue = saveChangeLog(build, launcher, listener, changelogFile, clearToolLauncher, variableResolver, saveChangeLogAction,
                    coNormalizedViewName, returnValue);
        }
        // --- CHECKOUT ---
        if (!checkoutAction.checkout(launcher, workspace, coNormalizedViewName)) {
            throw new AbortException();
        }
        if (computeChangeLogAfterCheckout) {
        	setUpdtFileName(checkoutAction.getUpdtFileName());
            returnValue = saveChangeLog(build, launcher, listener, changelogFile, clearToolLauncher, variableResolver, saveChangeLogAction,
                    coNormalizedViewName, returnValue);
        }

        return returnValue;
    }

    private boolean saveChangeLog(AbstractBuild build, Launcher launcher, BuildListener listener, File changelogFile, ClearToolLauncher clearToolLauncher,
            VariableResolver<String> variableResolver, SaveChangeLogAction saveChangeLogAction, String coNormalizedViewName, boolean returnValue)
            throws IOException, InterruptedException {
        List<? extends ChangeLogSet.Entry> changelogEntries;
        @SuppressWarnings("unchecked") Run prevBuild = build.getPreviousBuild();
        Date lastBuildTime = getBuildTime(prevBuild);
        HistoryAction historyAction = createHistoryAction(variableResolver, clearToolLauncher, build, /*getUseRecurseForChangelog()*/ false);
        changelogEntries = historyAction.getChanges(lastBuildTime, getViewPath(variableResolver), coNormalizedViewName, getBranchNames(variableResolver), getViewPaths(variableResolver, build, launcher, false));
        // Save change log
        if (CollectionUtils.isEmpty(changelogEntries)) {
            // no changes
            returnValue = createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
        }
        return returnValue;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener,
            SCMRevisionState baseline) throws IOException, InterruptedException {
    	PrintStream logger = listener.getLogger();

    	// check if build is running
        if (project.isBuilding() && !project.isConcurrentBuild()) {
        	logger.println("REASON: Build is running.");
        	return new PollingResult(baseline, baseline, Change.NONE);
        }

        // is first build
        if (isFirstBuild(baseline)) {
        	logger.println("REASON: First build.");
            return PollingResult.BUILD_NOW;
        }

        AbstractClearCaseSCMRevisionState ccBaseline = (AbstractClearCaseSCMRevisionState) baseline;

        AbstractBuild<?, ?> build = project.getSomeBuildWithWorkspace();
        if (build == null) {
        	logger.println("REASON: Build and workspace not valid.");
            return PollingResult.BUILD_NOW;
        }

        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);

        // check if config spec was updated
        boolean hasNewCS = hasNewConfigSpec(variableResolver, clearToolLauncher);
        if (hasNewCS) {
        	logger.println("REASON: New config spec detected.");
        	return PollingResult.BUILD_NOW;
        }

        HistoryAction historyAction = createHistoryAction(variableResolver, clearToolLauncher, build, /*getUseRecurseForPolling()*/ useOtherLoadRulesForPolling);
        Change change = Change.NONE;
        if (historyAction != null) {
            String viewPath = getViewPath(variableResolver);
            String viewTag = getViewName(variableResolver);
            String[] branchNames = getBranchNames(variableResolver);
            // path names to check (= load rules)
            // first get load rules from last baseline (polling)
        	String[] pathNames = ccBaseline.getLoadRules();
        	//logger.println("load rules from last baseline: " + pathNames.toString());
        	// if load rules are atomatically extracted or other rules are used or load rules from last baseline are empty -> use from load rules field or from other rules
        	if (extractLoadRules || useOtherLoadRulesForPolling || pathNames == null || pathNames.length == 0) {
        		pathNames = getViewPaths(variableResolver, build, launcher, true);
        		//logger.println("load rules from config: " + pathNames.toString());
        	}
	        if (isViewInvalid(build, variableResolver, clearToolLauncher, workspace, viewTag)) {
	            // View became invalid (tag removed...)
	            change = Change.INCOMPARABLE;
	        } else if (historyAction.hasChanges(ccBaseline.getBuildTime(), viewPath, viewTag, branchNames, pathNames)) {
        		change = Change.SIGNIFICANT;
        	} else {
        		change = Change.NONE;
        	}
        } else {
            // Error when calculating the new baseline => Probably clearcase server error, not launching the build
        	logger.println("WARNING: cannot createHistoryAction!");
            change = Change.NONE;
        }
        return new PollingResult(baseline, calcRevisionsFromPoll(build, launcher, listener), change);
    }

    private boolean isViewInvalid(AbstractBuild<?,?> build, VariableResolver<String> variableResolver, ClearToolLauncher clearToolLauncher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        CheckoutAction checkoutAction = createCheckOutAction(variableResolver, clearToolLauncher, build);
        return !checkoutAction.isViewValid(workspace, viewTag);
    }

    protected abstract boolean isFirstBuild(SCMRevisionState baseline);

    public abstract SCMRevisionState calcRevisionsFromPoll(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;

    protected boolean hasNewConfigSpec(VariableResolver<String> variableResolver, ClearToolLauncher cclauncher) throws IOException, InterruptedException {
        return false;
    }

    protected Date getBuildTime(Run<?, ?> lastBuild) {
        Date buildTime = lastBuild.getTimestamp().getTime();
        if (getMultiSitePollBuffer() != 0) {
            long lastBuildMilliSecs = lastBuild.getTimestamp().getTimeInMillis();
            buildTime = new Date(lastBuildMilliSecs - (1000L * 60 * getMultiSitePollBuffer()));
        }
        return buildTime;
    }

    /**
     * Creates a Hudson clear tool launcher.
     *
     * @param listener listener to write command output to
     * @param workspace the workspace for the job
     * @param launcher actual launcher to launch commands with
     * @return a clear tool launcher that uses Hudson for launching commands
     */
    public ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
        String cleartoolExe = PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(launcher.getComputer().getNode(), listener);
        return new HudsonClearToolLauncher(cleartoolExe, getDescriptor().getDisplayName(), listener, workspace, launcher);
    }

    protected ClearTool createClearTool(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        if (isUseDynamicView()) {
            return new ClearToolDynamic(variableResolver, launcher, getViewDrive(), getMkviewOptionalParam());
        } else {
            return new ClearToolSnapshot(variableResolver, launcher, mkviewOptionalParam);
        }
    }

    @Override
    public boolean processWorkspaceBeforeDeletion(AbstractProject<?, ?> project, FilePath workspace, Node node) throws IOException, InterruptedException {
        if (node == null) {
            // HUDSON-7663 : deleting a job that has never run
            return true;
        }
        StreamTaskListener listener = StreamTaskListener.fromStdout();
        Launcher launcher = node.createLauncher(listener);
        ClearTool ct = createClearTool(null, createClearToolLauncher(listener, project.getSomeWorkspace().getParent().getParent(), launcher));
        try {
            if (isUseDynamicView() && !isCreateDynView()) {
                return true;
            }
            AbstractBuild<?, ?> latestBuildOnNode = null;
            for(AbstractBuild<?, ?> build : project.getBuilds()) {
                if (node.equals(build.getBuiltOn())) {
                    latestBuildOnNode = build;
                    break;
                }
            }
            if (latestBuildOnNode == null) {
                latestBuildOnNode = project.getLastBuild();
            }
            BuildVariableResolver buildVariableResolver = new BuildVariableResolver(latestBuildOnNode);
            ct.rmviewtag(generateNormalizedViewName(buildVariableResolver));
        } catch (Exception e) {
            Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.WARNING, "Failed to remove ClearCase view", e);
        }
        return true;

    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public boolean isRemoveViewOnRename() {
        return removeViewOnRename;
    }

    public String getExcludedRegions() {
        return excludedRegions;
    }

    public String[] getExcludedRegionsNormalized() {
        return excludedRegions == null ? null : excludedRegions.split("[\\r\\n]+");
    }

    public Filter configureFilters(VariableResolver<String> variableResolver, AbstractBuild build, Launcher launcher) throws IOException, InterruptedException {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());

        String[] excludedStrings = getExcludedRegionsNormalized();

        if (excludedStrings != null && excludedStrings.length > 0) {
            for (String s : excludedStrings) {
                if (!s.equals("")) {
                    filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, s));
                }
            }
        }

        String filterRegexp = "";
        String[] viewPaths = getViewPaths(variableResolver, build, launcher, false);
        if (viewPaths != null) {
            filterRegexp = getViewPathsRegexp(viewPaths, launcher.isUnix());
        }
        if (StringUtils.isNotEmpty(filterRegexp)) {
            filters.add(new FileFilter(FileFilter.Type.ContainsRegxp, filterRegexp));
        }

        if (isFilteringOutDestroySubBranchEvent()) {
            filters.add(new DestroySubBranchFilter());
        }
        return new FilterChain(filters);
    }

    public static String getViewPathsRegexp(String[] loadRules, boolean isUnix) {
        // Note - the logic here to do ORing to match against *any* of the load rules is, quite frankly,
        // hackishly ugly. I'm embarassed by it. But it's what I've got for right now.
        String filterRegexp = "";
        if (loadRules != null) {
            String tempFilterRules = "";
            for (String loadRule : loadRules) {
                if (StringUtils.isNotEmpty(loadRule)) {
                    if (loadRule.endsWith("/") || loadRule.endsWith("\\")) {
                        loadRule = loadRule.substring(0, loadRule.length()-1);
                    }
                    loadRule = PathUtil.convertPathForOS(loadRule, isUnix);
                    tempFilterRules += "|" +Pattern.quote(loadRule + PathUtil.fileSepForOS(isUnix));
                    tempFilterRules += "|" + Pattern.quote(loadRule) + "$";
                }
            }

            // Adding tweak for ignoring leading slashes or Windows drives in case of strange situations using setview.
            if (StringUtils.isNotEmpty(tempFilterRules)) {
                filterRegexp = tempFilterRules.substring(1);
            }
        }
        return filterRegexp;
    }

    public String getViewPath() {
        return StringUtils.defaultString(viewPath, viewName);
    }

    public String getViewPath(VariableResolver<String> variableResolver) {
        String normalized = null;
        String viewPath = StringUtils.defaultIfEmpty(getViewPath(), getViewName());
        if (viewPath != null) {
            normalized = Util.replaceMacro(viewPath, variableResolver).replaceAll(REGEX_WHITESPACE, "_");
            setNormalizedViewPath(normalized);
        }
        return normalized;
    }

    public ChangeSetLevel getChangeset() {
        return changeset;
    }

    protected void setChangeset(ChangeSetLevel changeset) {
        this.changeset = changeset;
    }

    // compatibility with earlier plugins
    public Object readResolve() {
        if (viewStorageFactory == null) {
            viewStorageFactory = new ViewStorageFactory(null, winDynStorageDir, unixDynStorageDir);
        }
        return this;
    }

    protected void setExtendedViewPath(VariableResolver<String> variableResolver, ClearTool ct, AbstractHistoryAction action) {
        try {
            String viewPath = getViewPath(variableResolver);
            String pwv = ct.pwv(viewPath);
            if (pwv != null) {
                if (pwv.contains("/")) {
                    pwv += "/";
                } else {
                    pwv += "\\";
                }
                action.setExtendedViewPath(pwv);
            }
        } catch (Exception e) {
            Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.WARNING, "Exception when running 'cleartool pwv'", e);
        }
    }

    public String getUpdtFileName() {
        return updtFileName;
    }

    public void setUpdtFileName(String updtFileName) {
        this.updtFileName = updtFileName;
    }
}
