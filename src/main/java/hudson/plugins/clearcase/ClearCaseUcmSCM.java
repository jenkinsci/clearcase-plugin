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

import hudson.Launcher;
import hudson.Util;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.ucm.FoundationBaselineUcmWorkflow;
import hudson.plugins.clearcase.ucm.FreezeCodeStreamUcmWorkflow;
import hudson.plugins.clearcase.ucm.StreamUcmWorkflow;
import hudson.plugins.clearcase.ucm.UcmChangeLogParser;
import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmHistoryAction;
import hudson.plugins.clearcase.ucm.UcmRevisionState;
import hudson.plugins.clearcase.ucm.UcmSaveChangeLogAction;
import hudson.plugins.clearcase.ucm.UcmWorkflow;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class ClearCaseUcmSCM extends AbstractClearCaseScm {

    /**
     * ClearCase UCM SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static class ClearCaseUcmScmDescriptor extends AbstractClearCaseScmDescriptor<ClearCaseUcmSCM> implements ModelObject {

        private ClearCaseScmDescriptor baseDescriptor;

        public ClearCaseUcmScmDescriptor(ClearCaseScmDescriptor baseDescriptor) {
            super(ClearCaseUcmSCM.class, null);
            this.baseDescriptor = baseDescriptor;
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            return true;
        }

        public String getDefaultUnixDynStorageDir() {
            return baseDescriptor.getDefaultUnixDynStorageDir();
        }

        public String getDefaultViewName() {
            return baseDescriptor.getDefaultViewName();
        }

        public String getDefaultViewPath() {
            return baseDescriptor.getDefaultViewPath();
        }

        public ViewStorage getDefaultViewStorage() {
            return baseDescriptor.getDefaultViewStorage();
        }

        public String getDefaultWinDynStorageDir() {
            return baseDescriptor.getDefaultWinDynStorageDir();
        }

        @Override
        public String getDisplayName() {
            return "UCM ClearCase";
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            AbstractClearCaseScm scm = new ClearCaseUcmSCM(req.getParameter("ucm.stream"), req.getParameter("ucm.loadrules"), req.getParameter("ucm.viewname"),
                    req.getParameter("ucm.usedynamicview") != null, req.getParameter("ucm.viewdrive"), req.getParameter("ucm.mkviewoptionalparam"),
                    req.getParameter("ucm.filterOutDestroySubBranchEvent") != null, req.getParameter("ucm.useupdate") != null,
                    req.getParameter("ucm.rmviewonrename") != null, req.getParameter("ucm.excludedRegions"), Util.fixEmpty(req
                            .getParameter("ucm.multiSitePollBuffer")), req.getParameter("ucm.overrideBranchName"),
                            req.getParameter("ucm.createDynView") != null, req.getParameter("ucm.freezeCode") != null, req.getParameter("ucm.recreateView") != null,
                            req.getParameter("ucm.allocateViewName") != null, req.getParameter("ucm.viewpath"), req.getParameter("ucm.useManualLoadRules") != null,
                            ChangeSetLevel.fromString(req.getParameter("ucm.changeset")), extractViewStorage(req, formData),
                            formData.getBoolean("buildFoundationBaseline"));
            return scm;
        }
    }

    private final static String AUTO_ALLOCATE_VIEW_NAME = "${STREAM}_${JOB_NAME}_bs_hudson_view";

    private final static Logger LOGGER                  = Logger.getLogger(ClearCaseUcmSCM.class.getName());

    private static final String STREAM_PREFIX           = "stream:";
    private boolean             allocateViewName;
    private boolean             buildFoundationBaseline;
    private final String        overrideBranchName;
    private final String        stream;
    private boolean             useManualLoadRules;

    @Deprecated
    public ClearCaseUcmSCM(String stream, String loadrules, String viewTag, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename) {
        this(stream, loadrules, viewTag, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, "", null,
                "", false, false, false, false, viewTag, StringUtils.isBlank(loadrules), ChangeSetLevel.defaultLevel(), null, false);
    }

    @DataBoundConstructor
    public ClearCaseUcmSCM(String stream, String loadrules, String viewTag, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename, String excludedRegions, String multiSitePollBuffer,
            String overrideBranchName, boolean createDynView, boolean freezeCode, boolean recreateView, boolean allocateViewName, String viewPath,
            boolean useManualLoadRules, ChangeSetLevel changeset, ViewStorage viewStorage, boolean buildFoundationBaseline) {
        super(viewTag, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, excludedRegions, usedynamicview, viewdrive, false,
                useManualLoadRules ? loadrules : null, false, null, multiSitePollBuffer, createDynView, freezeCode, recreateView, viewPath, changeset,
                        viewStorage);
        this.stream = shortenStreamName(stream);
        this.allocateViewName = allocateViewName;
        this.overrideBranchName = overrideBranchName;
        this.useManualLoadRules = useManualLoadRules ? useManualLoadRules : StringUtils.isNotBlank(loadrules); // Default to keep backward compat
        this.buildFoundationBaseline = buildFoundationBaseline;
        Validate.notEmpty(this.stream, "The stream selector cannot be empty");
        setWorkflow(this.createWorkflow());
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new UcmChangeLogParser();
    }

    public ClearTool createClearTool(AbstractBuild<?, ?> build, Launcher launcher) throws IOException, InterruptedException {
        BuildVariableResolver variableResolver = new BuildVariableResolver(build);
        ClearToolLauncher clearToolLauncher = createClearToolLauncher(launcher.getListener(), build.getWorkspace(), launcher);
        return createClearTool(variableResolver, clearToolLauncher);
    }

    @Override
    public ClearTool createClearTool(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        return super.createClearTool(variableResolver, launcher);
    }

    @Override
    public String generateNormalizedViewName(VariableResolver<String> variableResolver, String modViewName) {
        // Modify the view name in order to support concurrent builds
        if (allocateViewName) {
            modViewName = AUTO_ALLOCATE_VIEW_NAME.replace("${STREAM}", UcmCommon.getNoVob(getStream(variableResolver)));
        }
        return super.generateNormalizedViewName(variableResolver, modViewName);
    }

    @Override
    public String[] getBranchNames(VariableResolver<String> variableResolver) {
        String override = Util.replaceMacro(overrideBranchName, variableResolver);
        if (StringUtils.isNotEmpty(override)) {
            return new String[] { override };
        }
        return new String[] { UcmCommon.getNoVob(getStream(variableResolver)) };
    }

    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return PluginImpl.UCM_DESCRIPTOR;
    }

    /**
     * Return the branch type used for changelog and polling. By default this will be the empty string, and the stream will be split to get the branch.
     * 
     * @return string containing the branch type.
     */
    public String getOverrideBranchName() {
        return overrideBranchName;
    }

    /**
     * Return the stream that is used to create the UCM view.
     * 
     * @return string containing the stream selector.
     */
    public String getStream() {
        return stream;
    }

    public String getStream(VariableResolver<String> variableResolver) {
        return Util.replaceMacro(stream, variableResolver);
    }

    @Override
    public String[] getViewPaths(VariableResolver<String> variableResolver, AbstractBuild build, Launcher launcher, boolean forPolling) throws IOException,
    InterruptedException {
        if (!useManualLoadRules) {
            // If the revision state is already available for this build, just use the value
            SCMRevisionState revisionState = build.getAction(SCMRevisionState.class);
            if (revisionState instanceof LoadRulesAware) {
                String[] lr = ((LoadRulesAware) revisionState).getLoadRules();
                if (lr != null) {
                    return lr;
                }
            }
            String streamSelector = getStream(variableResolver);
            ClearTool clearTool = createClearTool(build, launcher);
            String[] rootDirs = getWorkflow().getAllRootDirsFor(clearTool, streamSelector);
            for (int i = 0; i < rootDirs.length; i++) {
                rootDirs[i] = StringUtils.stripStart(rootDirs[i], "\\/");
            }
            return rootDirs;
        }
        return super.getViewPaths(variableResolver, build, launcher, forPolling);
    }

    public boolean isAllocateViewName() {
        return allocateViewName;
    }

    public boolean isBuildFoundationBaseline() {
        return buildFoundationBaseline;
    }

    public boolean isUseManualLoadRules() {
        return useManualLoadRules;
    }

    @Override
    public Object readResolve() {
        Object o = super.readResolve();
        if (o.getClass().isAssignableFrom(getClass())) {
            ClearCaseUcmSCM ucm = (ClearCaseUcmSCM) o;
            ucm.setWorkflow(createWorkflow());
            return ucm;
        }
        return o;
    }

    public void setAllocateViewName(boolean allocateViewName) {
        this.allocateViewName = allocateViewName;
    }

    public void setBuildFoundationBaseline(boolean buildFoundationBaseline) {
        this.buildFoundationBaseline = buildFoundationBaseline;
    }

    @Override
    protected CheckoutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build)
            throws IOException, InterruptedException {
        ClearTool clearTool = createClearTool(variableResolver, launcher);
        String stream2 = getStream(variableResolver);
        ViewStorage decoratedViewStorage = getViewStorageOrDefault().decorate(variableResolver);
        String[] loadRules = getViewPaths(variableResolver, build, launcher.getLauncher());
        String viewTag = getViewPath(variableResolver);
        return getWorkflow().createCheckoutAction(clearTool, stream2, loadRules, viewTag, decoratedViewStorage, build);
    }

    @Override
    protected UcmHistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build,
            SCMRevisionState baseline, boolean useRecurse) throws IOException, InterruptedException {
        ClearTool ct = createClearTool(variableResolver, launcher);
        SCMRevisionState oldBaseline = baseline;
        SCMRevisionState newBaseline = null;
        if (build != null) {
            newBaseline = calcRevisionsFromBuild(build, launcher.getLauncher(), launcher.getListener());
        }
        launcher.getListener().getLogger().println("oldBaseline : " + oldBaseline);
        launcher.getListener().getLogger().println("newBaseline : " + newBaseline);
        String extendedViewPath = computeExtendedViewPath(variableResolver, ct);
        return getWorkflow().createHistoryAction(ct, configureFilters(variableResolver, build, launcher.getLauncher()), getStream(variableResolver), build,
                oldBaseline, newBaseline, extendedViewPath);
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new UcmSaveChangeLogAction();
    }

    @Override
    protected boolean invalidRevisionState(SCMRevisionState baseline) {
        return !(baseline instanceof UcmRevisionState);
    }

    @Override
    protected boolean isFirstBuild(SCMRevisionState baseline) {
        return baseline == null;
    }

    @Override
    protected SCMRevisionState createRevisionState(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener, Date date) throws IOException,
    InterruptedException {
        ClearTool clearTool = createClearTool(build, launcher);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        String resolvedStream = getStream(variableResolver);
        String[] viewPaths = getViewPaths(variableResolver, build, launcher);
        return getWorkflow().createRevisionState(clearTool, taskListener, date, resolvedStream, viewPaths);
    }

    private UcmWorkflow createWorkflow() {
        if (buildFoundationBaseline) {
            FoundationBaselineUcmWorkflow foundationBaselineWorkflow = new FoundationBaselineUcmWorkflow();
            foundationBaselineWorkflow.setUseUpdate(isUseUpdate());
            return foundationBaselineWorkflow;
        }
        StreamUcmWorkflow streamWorkflow;
        if (isFreezeCode()) {
            streamWorkflow = new FreezeCodeStreamUcmWorkflow();
        } else {
            streamWorkflow = new StreamUcmWorkflow();
        }
        streamWorkflow.setChangesetLevel(getChangeset());
        streamWorkflow.setUseUpdate(isUseUpdate());
        streamWorkflow.setUseDynamicView(isUseDynamicView());
        streamWorkflow.setRecreateDynamicView(isRecreateView());
        streamWorkflow.setViewDrive(getViewDrive());
        streamWorkflow.setCreateDynamicView(isCreateDynView());
        return streamWorkflow;
    }

    private String shortenStreamName(String longStream) {
        if (StringUtils.startsWith(longStream, STREAM_PREFIX)) {
            return StringUtils.substringAfter(longStream, STREAM_PREFIX);
        }
        return longStream;
    }
}
