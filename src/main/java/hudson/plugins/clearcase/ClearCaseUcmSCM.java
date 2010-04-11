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
import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.ucm.UcmChangeLogParser;
import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmHistoryAction;
import hudson.plugins.clearcase.ucm.UcmSaveChangeLogAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.VariableResolver;

import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * SCM for ClearCaseUCM. This SCM will create a UCM view from a stream and apply a list of load rules to it.
 */
public class ClearCaseUcmSCM extends AbstractClearCaseScm {

    private static final String STREAM_PREFIX = "stream:";

    private final static String AUTO_ALLOCATE_VIEW_NAME = "${STREAM}_${JOB_NAME}_bs_hudson_view";

    private final String stream;
    private String paramStream;
    private final String overrideBranchName;
    private boolean allocateViewName;

    @DataBoundConstructor
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename, String excludedRegions, String multiSitePollBuffer,
            String overrideBranchName, boolean createDynView, String winDynStorageDir, String unixDynStorageDir, boolean freezeCode, boolean recreateView,
            boolean allocateViewName) {
        super(viewname, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, excludedRegions, usedynamicview, viewdrive, loadrules,
                multiSitePollBuffer, createDynView, winDynStorageDir, unixDynStorageDir, freezeCode, recreateView);
        this.stream = shortenStreamName(stream);
        this.allocateViewName = allocateViewName;
        this.paramStream = "";
        this.overrideBranchName = overrideBranchName;
    }

    @Deprecated
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean useUpdate, boolean rmviewonrename) {
        this(stream, loadrules, viewname, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, "", null,
                "", false, null, null, false, false, false);
    }

    /**
     * Return the stream that is used to create the UCM view.
     * 
     * @return string containing the stream selector.
     */
    public String getStream() {
        return StringUtils.defaultIfEmpty(paramStream, stream);
    }

    public boolean isAllocateViewName() {
        return allocateViewName;
    }

    public void setAllocateViewName(boolean allocateViewName) {
        this.allocateViewName = allocateViewName;
    }

    /**
     * Return the branch type used for changelog and polling. By default this will be the empty string, and the stream
     * will be split to get the branch.
     * 
     * @return string containing the branch type.
     */
    public String getOverrideBranchName() {
        return overrideBranchName;
    }

    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return PluginImpl.UCM_DESCRIPTOR;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new UcmChangeLogParser();
    }

    @Override
    public String[] getBranchNames() {
        if (StringUtils.isNotEmpty(overrideBranchName)) {
            return new String[] { overrideBranchName };
        } else {
            String branch = getStream();
            int indexOfAt = branch.indexOf("@");
            if (indexOfAt > -1) {
                branch = branch.substring(0, indexOfAt);
            }
            return new String[] { branch };
        }
    }

    @Override
    public String generateNormalizedViewName(VariableResolver<String> variableResolver, String modViewName) {
        // Modify the view name in order to support concurrent builds
        if (allocateViewName) {
            modViewName = AUTO_ALLOCATE_VIEW_NAME.replace("${STREAM}", UcmCommon.getNoVob(getStream()));
        }
        return super.generateNormalizedViewName(variableResolver, modViewName);
    }

    @Override
    protected CheckOutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        // set value in paramStream (if build is parametrized, support changing the build stream)
        paramStream = (String) build.getBuildVariables().get("STREAM");

        CheckOutAction action;
        if (isUseDynamicView()) {
            action = new UcmDynamicCheckoutAction(createClearTool(variableResolver, launcher), getStream(), isCreateDynView(),
                    getNormalizedWinDynStorageDir(variableResolver), getNormalizedUnixDynStorageDir(variableResolver), build, isFreezeCode(), isRecreateView());
        } else {
            action = new UcmSnapshotCheckoutAction(createClearTool(variableResolver, launcher),getStream(), getViewPaths(), isUseUpdate());
        }
        return action;
    }

    @Override
    protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        ClearTool ct = createClearTool(variableResolver, launcher);
        // String viewName, String stream, String unixDynStorageDir, String winDynStorageDir, String viewDrive
        UcmHistoryAction action = new UcmHistoryAction(ct, isUseDynamicView(), configureFilters(launcher), getStream(), getViewDrive(), build, isFreezeCode());

        try {
            String pwv = ct.pwv(generateNormalizedViewName((BuildVariableResolver) variableResolver));

            if (pwv != null) {
                if (pwv.contains("/")) {
                    pwv += "/";
                } else {
                    pwv += "\\";
                }
                action.setExtendedViewPath(pwv);
            }
        } catch (Exception e) {
            Logger.getLogger(ClearCaseUcmSCM.class.getName()).log(Level.WARNING, "Exception when running 'cleartool pwv'", e);
        }

        return action;
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new UcmSaveChangeLogAction();
    }

    @Override
    protected ClearTool createClearTool(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        if (isUseDynamicView()) {
            return new ClearToolDynamicUCM(variableResolver, launcher, getViewDrive(), getMkviewOptionalParam());
        } else {
            return super.createClearTool(variableResolver, launcher);
        }
    }

    private String shortenStreamName(String longStream) {
        if (longStream.startsWith(STREAM_PREFIX)) {
            return longStream.substring(STREAM_PREFIX.length());
        } else {
            return longStream;
        }
    }

    /**
     * ClearCase UCM SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static class ClearCaseUcmScmDescriptor extends SCMDescriptor<ClearCaseUcmSCM> implements ModelObject {
        
        private ClearCaseScmDescriptor baseDescriptor;

        public ClearCaseUcmScmDescriptor(ClearCaseScmDescriptor baseDescriptor) {
            super(ClearCaseUcmSCM.class, null);
            this.baseDescriptor = baseDescriptor;
            load();
        }

        public String getDefaultViewName() {
            return baseDescriptor.getDefaultViewName();
        }

        @Override
        public String getDisplayName() {
            return "UCM ClearCase";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) {
            return true;
        }

        public String getDefaultWinDynStorageDir() {
            return baseDescriptor.getDefaultWinDynStorageDir();
        }

        public String getDefaultUnixDynStorageDir() {
            return baseDescriptor.getDefaultUnixDynStorageDir();
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            ClearCaseUcmSCM scm = new ClearCaseUcmSCM(
                                                      req.getParameter("ucm.stream"),
                                                      req.getParameter("ucm.loadrules"),
                                                      req.getParameter("ucm.viewname"),
                                                      req.getParameter("ucm.usedynamicview") != null,
                                                      req.getParameter("ucm.viewdrive"),
                                                      req.getParameter("ucm.mkviewoptionalparam"),
                                                      req.getParameter("ucm.filterOutDestroySubBranchEvent") != null,
                                                      req.getParameter("ucm.useupdate") != null,
                                                      req.getParameter("ucm.rmviewonrename") != null,
                                                      req.getParameter("ucm.excludedRegions"),
                                                      fixEmpty(req.getParameter("ucm.multiSitePollBuffer")),
                                                      req.getParameter("ucm.overrideBranchName"),
                                                      req.getParameter("ucm.createDynView") != null,
                                                      req.getParameter("ucm.winDynStorageDir"),
                                                      req.getParameter("ucm.unixDynStorageDir"),
                                                      req.getParameter("ucm.freezeCode") != null,
                                                      req.getParameter("ucm.recreateView") != null,
                                                      req.getParameter("ucm.allocateViewName") != null
                                                      );
            return scm;
        }
    }
}
