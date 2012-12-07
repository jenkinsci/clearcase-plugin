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
package hudson.plugins.clearcase.ucm;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.ParameterValue;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.ParametersAction;
import hudson.model.StringParameterValue;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.DefaultPromotionLevel;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

/**
 * UcmMakeBaseline creates baselines on a ClearCase stream after a successful build. The name and comment of the
 * baseline can be changed using the namePattern and commentPattern variables.
 * 
 * @author Peter Liljenberg
 * @author Gregory Boissinot
 *         <ul>
 *         <li>2008-10-11 Add the rebase dynamic view feature</li>
 *         <li>2008-11-21 Restrict the baseline creation on read/write components</li>
 *         <li>2009-03-02 Add the dynamic view support for the make baseline</li>
 *         <li>2009-03-22 'The createdBaselines' follow now the same model of the 'latestBaselines' and
 *         'readWriteComponents' fields.</li>
 *         </ul>
 */
public class UcmMakeBaseline extends Notifier {

    private static final String ENV_CC_BASELINE_NAME = "CC_BASELINE_NAME";
    private static final Logger LOGGER = Logger.getLogger(UcmMakeBaseline.class.getName());

    private transient List<String> readWriteComponents = null;

    private transient List<String> latestBaselines = new ArrayList<String>();

    private transient List<Baseline> createdBaselines = null;

    private final String namePattern;

    private final String commentPattern;

    private final boolean promote;

    private final String promotionLevel;

    private final boolean demote;

    private final String demotionLevel;

    private final boolean lockStream;

    private final boolean recommend;

    private transient boolean streamSuccessfullyLocked;

    private final boolean fullBaseline;

    private final boolean identical;

    private final String dynamicViewName;

    private final boolean rebaseDynamicView;

    public String getCommentPattern() {
        return this.commentPattern;
    }

    public boolean isPromote() {
        return this.promote;
    }

    public String getPromotionLevel() {
        return this.promotionLevel;
    }

    public boolean isDemote() {
        return this.demote;
    }

    public String getDemotionLevel() {
        return this.demotionLevel;
    }

    public boolean isLockStream() {
        return this.lockStream;
    }

    public String getNamePattern() {
        return this.namePattern;
    }

    public boolean isRecommend() {
        return this.recommend;
    }

    public boolean isFullBaseline() {
        return this.fullBaseline;
    }

    public boolean isIdentical() {
        return this.identical;
    }

    public String getDynamicViewName() {
        return this.dynamicViewName;
    }

    public boolean isRebaseDynamicView() {
        return this.rebaseDynamicView;
    }

    public List<String> getReadWriteComponents() {
        return readWriteComponents;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(UcmMakeBaseline.class);
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Makebaseline";
        }

        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            Notifier n = new UcmMakeBaseline
                (req.getParameter("mkbl.namepattern"), 
                 req.getParameter("mkbl.commentpattern"),
                 req.getParameter("mkbl.lock") != null, 
                 req.getParameter("mkbl.recommend") != null, 
                 req.getParameter("mkbl.fullBaseline") != null, 
                 req.getParameter("mkbl.identical") != null, 
                 req.getParameter("mkbl.rebaseDynamicView") != null,
                 req.getParameter("mkbl.dynamicViewName"),
                 req.getParameter("mkbl.promote") != null,
                 req.getParameter("mkbl.promotionLevel"),
                 req.getParameter("mkbl.demote") != null,
                 req.getParameter("mkbl.demotionLevel"));
            return n;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clearcase/ucm/mkbl/help.html";
        }

        @Override
        public boolean isApplicable(Class clazz) {
            return true;
        }
    }

    public UcmMakeBaseline(final String namePattern, final String commentPattern, final boolean lock, final boolean recommend, final boolean fullBaseline,
                           final boolean identical, final boolean rebaseDynamicView, final String dynamicViewName,
                           final boolean promote, final String promotionLevel, final boolean demote, final String demotionLevel) {
        this.namePattern = namePattern;
        this.commentPattern = commentPattern;
        this.lockStream = lock;
        this.recommend = recommend;
        this.fullBaseline = fullBaseline;
        this.identical = identical;
        this.rebaseDynamicView = rebaseDynamicView;
        this.dynamicViewName = dynamicViewName;
        this.promote = promote;
        this.promotionLevel = promotionLevel;
        this.demote = demote;
        this.demotionLevel = demotionLevel;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        SCM scm = build.getProject().getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM ucm = (ClearCaseUcmSCM) scm;
            Launcher launcher = Executor.currentExecutor().getOwner().getNode().createLauncher(listener);
            VariableResolver<String> variableResolver = new BuildVariableResolver(build);
            ClearTool clearTool = ucm.createClearTool(variableResolver, ucm.createClearToolLauncher(listener, build.getWorkspace(), launcher));
            if (this.lockStream) {
                try {
                    this.streamSuccessfullyLocked = lockStream(clearTool, ucm.getStream());
                    if (!this.streamSuccessfullyLocked) {
                        listener.fatalError("Failed to lock stream");
                    }
                } catch (Exception ex) {
                    listener.fatalError("Failed to lock stream: " + ex);
                    return false;
                }
            }
            try {
                // Get read/write component
                String viewTag = ucm.getViewName(variableResolver);
                this.readWriteComponents = getReadWriteComponent(clearTool, viewTag);
                if (!readWriteComponents.isEmpty()) {
                    this.createdBaselines = makeBaseline(clearTool, viewTag, variableResolver);
                    this.latestBaselines = getLatestBaselineNames(clearTool, viewTag);
                    addBuildParameter(build);
                }

            } catch (Exception ex) {
                listener.getLogger().println("Failed to create baseline: " + ex);
                return false;
            }
            return true;
        } else {
            listener.fatalError("Not a UCM clearcase SCM, cannot create baseline");
            return false;
        }

    }

    private void addBuildParameter(AbstractBuild<?, ?> build) {
        if (!CollectionUtils.isEmpty(this.latestBaselines)) {
            ArrayList<ParameterValue> parameters = new ArrayList<ParameterValue>();
            String baselineName = latestBaselines.get(0);
            parameters.add(new StringParameterValue(ENV_CC_BASELINE_NAME, baselineName));
            build.addAction(new ParametersAction(parameters));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        SCM scm = build.getProject().getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM ucm = (ClearCaseUcmSCM) scm;
            VariableResolver<String> variableResolver = new BuildVariableResolver(build);
            ClearTool clearTool = ucm.createClearTool(variableResolver, ucm.createClearToolLauncher(listener, build.getWorkspace(), launcher));

            Result result = build.getResult();
            if (result.equals(Result.SUCCESS)) {
                if (this.promote) {
                    // On success, promote all current baselines in stream
                    for (String baselineName : this.latestBaselines) {
                        promoteBaseline(clearTool, baselineName);
                    }
                }
                if (this.recommend) {
                    recommendBaseline(clearTool, ucm.getStream(variableResolver));
                }

                // Rebase a dynamic view
                if (this.rebaseDynamicView) {
                    for (String baseline : this.latestBaselines) {
                        rebaseDynamicView(clearTool, Util.replaceMacro(this.dynamicViewName, variableResolver), baseline);
                    }
                }
            } else if (result.equals(Result.FAILURE) && this.demote) {

                List<String> alreadyRejected = new ArrayList<String>();

                // On failure, demote only baselines created in this build
                for (Baseline baseline : this.createdBaselines) {

                    // Find full baseline name from latest baselines
                    String realBaselineName = null;
                    for (String fullBaselineName : this.latestBaselines) {
                        if (fullBaselineName.startsWith(baseline.getBaselineName())) {
                            if (!alreadyRejected.contains(fullBaselineName)) {
                                realBaselineName = fullBaselineName;
                            }
                        }
                    }
                    if (realBaselineName == null) {
                        listener.getLogger().println("Couldn't find baseline name for " + baseline.getBaselineName());
                    } else {
                        demoteBaseline(clearTool, realBaselineName);
                        alreadyRejected.add(realBaselineName);
                    }
                }
            }

            if (this.lockStream && this.streamSuccessfullyLocked) {
                unlockStream(clearTool, ucm.getStream());
            }
        } else {
            listener.fatalError("Not a UCM clearcase SCM, cannot create baseline");
            return false;
        }
        return true;
    }

    private void rebaseDynamicView(ClearTool clearTool, String viewTag, String baselineName)
            throws InterruptedException, IOException {
        clearTool.rebaseDynamic(viewTag, baselineName);
    }

    private void unlockStream(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        clearTool.unlock("Unlocked by Hudson", "stream:" + stream);
    }

    /**
     * Locks the stream used during build to ensure the streams integrity during the whole build process, i.e. we want
     * to make sure that no DELIVERs are made to the stream during build.
     * 
     * @return true if the stream was locked
     */
    private boolean lockStream(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        return clearTool.lock("Locked by Hudson", "stream:" + stream);
    }

    private List<Baseline> makeBaseline(ClearTool clearTool, String viewTag, VariableResolver<String> variableResolver) throws Exception {
        String baselineName = Util.replaceMacro(namePattern, variableResolver);
        String baselineComment = Util.replaceMacro(commentPattern, variableResolver);
        return clearTool.mkbl(baselineName, viewTag, baselineComment, fullBaseline, identical, this.readWriteComponents, null, null);

    }

    private void recommendBaseline(ClearTool clearTool, String stream) throws InterruptedException,
            IOException {
        clearTool.recommendBaseline(stream);
    }

    private void promoteBaseline(ClearTool clearTool, String baselineName)
            throws InterruptedException, IOException {
        final String promotionLevel = 
            StringUtils.isNotEmpty(this.promotionLevel) ?
            this.promotionLevel: DefaultPromotionLevel.BUILT.toString();
        clearTool.setBaselinePromotionLevel(baselineName, promotionLevel);
    }

    private void demoteBaseline(ClearTool clearTool, String baselineName)
            throws InterruptedException, IOException {
        final String demotionLevel = 
            StringUtils.isNotEmpty(this.demotionLevel) ?
	    this.demotionLevel: DefaultPromotionLevel.REJECTED.toString();
        clearTool.setBaselinePromotionLevel(baselineName, demotionLevel);
    }

    /**
     * Retrieve the read/write component list with PVOB
     * 
     * @return the read/write component like 'DeskCore@\P_ORC DeskShared@\P_ORC build_Product@\P_ORC'
     * @throws IOException
     * @throws InterruptedException
     * @throws Exception
     */
    private List<String> getReadWriteComponent(ClearTool clearTool, String viewTag) throws IOException, InterruptedException {
        String output = clearTool.lsproject(viewTag, "%[mod_comps]Xp");

        final String prefix = "component:";
        if (StringUtils.startsWith(output, prefix)) {
            List<String> componentNames = new ArrayList<String>();
            String[] componentNamesSplit = output.split(" ");
            for (String componentName : componentNamesSplit) {
                String componentNameTrimmed = StringUtils.difference(prefix, componentName).trim();
                if (StringUtils.isNotEmpty(componentNameTrimmed)) {
                    componentNames.add(componentNameTrimmed);
                }
            }
            return componentNames;
        }
        throw new IOException(output);
    }

    /**
     * Get the component binding to the baseline
     * 
     * @param baselineName the baseline name like 'deskCore_3.2-146_2008-11-14_18-07-22.3543@\P_ORC'
     * @return the component name like 'Desk_Core@\P_ORC'
     * @throws InterruptedException
     * @throws IOException
     */
    private String getComponentforBaseline(ClearTool clearTool, String baselineName) throws InterruptedException, IOException {
        String output = clearTool.lsbl(baselineName, "%[component]Xp");
        String prefix = "component:";
        if (StringUtils.startsWith(output, prefix)) {
            return StringUtils.difference(prefix, output);
        }
        throw new IOException("Incorrect output. Received " + output);
    }

    private List<String> getLatestBaselineNames(ClearTool clearTool, String viewTag) throws Exception {

        String output = clearTool.lsstream(null, viewTag, "%[latest_bls]Xp");
        String prefix = "baseline:";
        if (StringUtils.startsWith(output, prefix)) {
            List<String> baselineNames = new ArrayList<String>();
            String[] baselineNamesSplit = output.split(prefix);
            for (String baselineName : baselineNamesSplit) {
                String baselineNameTrimmed = baselineName.trim();
                if (StringUtils.isNotEmpty(baselineNameTrimmed)) {
                    // Retrict to baseline bind to read/write component
                    String blComp = getComponentforBaseline(clearTool, baselineNameTrimmed);
                    if (this.readWriteComponents.contains(blComp))
                        baselineNames.add(baselineNameTrimmed);
                }
            }
            return baselineNames;
        }
        throw new Exception("Failed to get baselinename, reason: " + output);

    }

}
