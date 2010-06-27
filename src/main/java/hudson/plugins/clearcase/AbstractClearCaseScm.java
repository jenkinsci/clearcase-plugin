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
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.scm.ChangeLogSet;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMRevisionState;
import hudson.scm.PollingResult.Change;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
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

    public static final String CLEARCASE_VIEWTAG_ENVSTR = "CLEARCASE_VIEWTAG";
    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
    public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";

    private String viewName;
    private final String mkviewOptionalParam;
    private final boolean filteringOutDestroySubBranchEvent;
    private transient ThreadLocal<String> normalizedViewName;
    private transient ThreadLocal<String> normalizedViewPath;
    private final boolean useUpdate;
    private final boolean removeViewOnRename;
    private String excludedRegions;
    private final String loadRules;
    private final boolean useDynamicView;
    private final String viewDrive;
    private int multiSitePollBuffer;
    private final boolean createDynView;
    private final String winDynStorageDir;
    private final String unixDynStorageDir;
    private final boolean freezeCode;
    private final boolean recreateView;
    private final String viewPath;

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
            final boolean rmviewonrename, final String excludedRegions, final boolean useDynamicView, final String viewDrive, final String loadRules,
            final String multiSitePollBuffer, final boolean createDynView, final String winDynStorageDir, final String unixDynStorageDir,
            final boolean freezeCode, final boolean recreateView, final String viewPath) {
        Validate.notNull(viewName);
        this.viewName = viewName;
        this.mkviewOptionalParam = mkviewOptionalParam;
        this.filteringOutDestroySubBranchEvent = filterOutDestroySubBranchEvent;
        this.useUpdate = useUpdate;
        this.removeViewOnRename = rmviewonrename;
        this.excludedRegions = excludedRegions;
        this.useDynamicView = useDynamicView;
        this.viewDrive = viewDrive;
        this.loadRules = loadRules;
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
        this.winDynStorageDir = winDynStorageDir;
        this.unixDynStorageDir = unixDynStorageDir;
        this.freezeCode = freezeCode;
        this.recreateView = recreateView;
        this.viewPath = StringUtils.defaultIfEmpty(viewPath, viewName);
    }

    /**
     * Create a CheckOutAction that will be used by the checkout method.
     * 
     * @param launcher the command line launcher
     * @return an action that can check out code from a ClearCase repository.
     */
    protected abstract CheckOutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build);

    /**
     * Create a HistoryAction that will be used by the pollChanges() and checkout() method.
     * 
     * @param variableResolver
     * @param useDynamicView
     * @param launcher the command line launcher
     * @return an action that can poll if there are any changes a ClearCase repository.
     */
    protected abstract HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build);

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
     * 
     * @return string array that will be used by the lshistory command and for constructing the config spec, etc.
     */
    public String[] getViewPaths() {
        if (StringUtils.isBlank(getLoadRules())) {
            return null;
        }

        String[] rules = getLoadRules().split("[\\r\\n]+");
        for (int i = 0; i < rules.length; i++) {
            String rule = rules[i];
            // Remove "load " from the string, just in case.
            if (rule.startsWith("load ")) {
                rule = rule.substring(5);
            }
            // Remove "\\", "\" or "/" from the load rule. (bug#1706) Only if
            // the view is not dynamic
            // the user normally enters a load rule beginning with those chars
            while (rule.startsWith("\\") || rule.startsWith("/")) {
                rule = rule.substring(1);
            }
            rules[i] = rule;
        }
        return rules;
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

    public String getLoadRules() {
        return loadRules;
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
        if (useDynamicView) {
            String normViewName = getNormalizedViewName( );
            return new FilePath(workspace.getChannel(), viewDrive).child(normViewName);
        } else {
            String normViewPath = getNormalizedViewPath();
            if (normViewPath != null) {
                return workspace.child(normViewPath);
            } else {
                // Should never happen, because viewName must not be null, and if viewpath is null, then it is made equal to viewName
                throw new IllegalStateException("View path name cannot be null. There is a bug inside AbstractClearCaseScm.");
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
            normalized = Util.replaceMacro(v.replaceAll("[\\s\\\\\\/:\\?\\*\\|]+", "_"), variableResolver);
            setNormalizedViewName(normalized);
        }
        return normalized;
    }

    public String getWinDynStorageDir() {
        return winDynStorageDir;
    }

    public String getNormalizedWinDynStorageDir(VariableResolver<String> variableResolver) {
        if (variableResolver != null) {
            return Util.replaceMacro(getWinDynStorageDir(), variableResolver);
        } else {
            return getWinDynStorageDir();
        }
    }

    public String getUnixDynStorageDir() {
        return unixDynStorageDir;
    }

    public String getNormalizedUnixDynStorageDir(VariableResolver<String> variableResolver) {
        if (variableResolver != null) {
            return Util.replaceMacro(getUnixDynStorageDir(), variableResolver);
        } else {
            return getUnixDynStorageDir();
        }
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

        generatedNormalizedViewName = generatedNormalizedViewName.replaceAll("[\\s\\\\\\/:\\?\\*\\|]+", "_");

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
                env.put("CLEARCASE_VIEWPATH_ENVSTR", viewDrive + File.separator + normalizedViewPath);
            } else {
                String workspace = env.get("WORKSPACE");
                if (workspace != null) {
                    env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator + normalizedViewPath);
                }
            }
        }
    }

    @Override
    public boolean checkout(@SuppressWarnings("unchecked") AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) throws IOException,
            InterruptedException {
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener, workspace, launcher);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        CheckOutAction checkoutAction = createCheckOutAction(variableResolver, clearToolLauncher, build);
        
        SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);
        
        // Checkout code
        String coNormalizedViewName = getViewName(variableResolver);

        build.addAction(new ClearCaseDataAction());

        // Gather change log
        List<? extends ChangeLogSet.Entry> changelogEntries = null;
        if (build.getPreviousBuild() != null) {
            @SuppressWarnings("unchecked") Run prevBuild = build.getPreviousBuild();
            Date lastBuildTime = getBuildTime(prevBuild);
            HistoryAction historyAction = createHistoryAction(variableResolver, clearToolLauncher, build);
            changelogEntries = historyAction.getChanges(lastBuildTime, getViewPath(variableResolver), coNormalizedViewName, getBranchNames(variableResolver), getViewPaths());
        }

        boolean returnValue = true;
        // Save change log
        if (CollectionUtils.isEmpty(changelogEntries)) {
            // no changes
            returnValue = createEmptyChangeLog(changelogFile, listener, "changelog");
        } else {
            saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
        }

        
        if (!checkoutAction.checkout(launcher, workspace, coNormalizedViewName)) {
            throw new AbortException();
        }

        return returnValue;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener,
            SCMRevisionState baseline) throws IOException, InterruptedException {
        if (isFirstBuild(baseline)) {
            return PollingResult.BUILD_NOW;
        }
        AbstractClearCaseSCMRevisionState ccBaseline = (AbstractClearCaseSCMRevisionState) baseline;
        
        AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) project.getLastBuild();
        if (build == null) {
            return PollingResult.BUILD_NOW;
        }
        
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        HistoryAction historyAction = createHistoryAction(variableResolver, createClearToolLauncher(listener, workspace, launcher), build);
        Change change;
        String viewPath = getViewPath(variableResolver);
        String viewName = getViewName(variableResolver);
        String[] branchNames = getBranchNames(variableResolver);

        if (historyAction.hasChanges(ccBaseline.getBuildTime(), viewPath, viewName, branchNames, getViewPaths())) {
            change = Change.SIGNIFICANT;
        } else {
            change = Change.NONE;
        }
        return new PollingResult(baseline, calcRevisionsFromPoll(build, launcher, listener), change);
    }
    
    protected abstract boolean isFirstBuild(SCMRevisionState baseline);
    
    public abstract SCMRevisionState calcRevisionsFromPoll(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;
    
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
        return new HudsonClearToolLauncher(PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), getDescriptor().getDisplayName(), listener, workspace, launcher);
    }

    protected ClearTool createClearTool(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        return new ClearToolSnapshot(variableResolver, launcher, mkviewOptionalParam);
    }

    @Override
    public boolean processWorkspaceBeforeDeletion(AbstractProject<?, ?> project, FilePath workspace, Node node) throws IOException, InterruptedException {
        StreamTaskListener listener = StreamTaskListener.fromStdout();
        Launcher launcher = node.createLauncher(listener);
        ClearTool ct = createClearTool(null, createClearToolLauncher(listener, project.getSomeWorkspace().getParent().getParent(), launcher));
        try {
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

    public Filter configureFilters(ClearToolLauncher ctLauncher) {
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
        if (getViewPaths() != null) {
            filterRegexp = getViewPathsRegexp(getViewPaths(), ctLauncher.getLauncher().isUnix());
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
                filterRegexp = "^(?:\\W?|\\w\\:\\\\)(" + tempFilterRules.substring(1) + ")";
            }
        }
        return filterRegexp;
    }

    public String getViewPath() {
        return StringUtils.defaultString(viewPath, viewName);
    }
    
    public String getViewPath(VariableResolver<String> variableResolver) {
        String normalized = null;
        String viewPath = getViewPath();
        if (viewPath != null) {
            normalized = Util.replaceMacro(viewPath.replaceAll("[\\s\\\\\\/:\\?\\*\\|]+", "_"), variableResolver);
            setNormalizedViewPath(normalized);
        }
        return normalized;
    }

}
