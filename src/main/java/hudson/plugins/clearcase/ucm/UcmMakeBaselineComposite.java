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
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.ClearTool.DefaultPromotionLevel;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.SCM;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

/**
 * UcmMakeBaselineComposite creates a composite baseline and extracting composite baseline information is a file.
 * 
 * @author Gregory BOISSINOT - Zenika
 */
public class UcmMakeBaselineComposite extends Notifier {

    private final String compositeNamePattern;
    private final String compositeStreamSelector;
    private final String compositeComponentName;
    private final boolean extractInfoFile;
    private final String fileName;

    public String getCompositeNamePattern() {
        return compositeNamePattern;
    }

    public String getCompositeStreamSelector() {
        return compositeStreamSelector;
    }

    public String getCompositeComponentName() {
        return this.compositeComponentName;
    }

    public boolean isExtractInfoFile() {
        return this.extractInfoFile;
    }

    public String getFileName() {
        return this.fileName;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        // see Descriptor javadoc for more about what a descriptor is.
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            super(UcmMakeBaselineComposite.class);
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Makebaseline Composite";
        }

        @Override
        public Notifier newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            Notifier p = new UcmMakeBaselineComposite(
                                                      req.getParameter("mkbl.compositenamepattern"),
                                                      req.getParameter("mkbl.compositestreamselector"),
                                                      req.getParameter("mkbl.compositecomponentname"),
                                                      req.getParameter("mkbl.extractinfofile")!=null,
                                                      req.getParameter("mkbl.filename")
                                                      );
            return p;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clearcase/ucm/mkbl/composite/help.html";
        }

        @Override
        public boolean isApplicable(Class clazz) {
            return true;
        }
    }

    private UcmMakeBaselineComposite(
                                     final String compositeNamePattern,
                                     final String compositeStreamSelector,
                                     final String compositeComponentName,
                                     final boolean extractInfoFile,
                                     final String fileName) {

        this.compositeNamePattern = compositeNamePattern.trim();
        this.compositeStreamSelector = compositeStreamSelector.trim();
        this.compositeComponentName = compositeComponentName.trim();
        this.extractInfoFile = extractInfoFile;
        this.fileName = fileName.trim();

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

        return true;
    }

    @Override
    public boolean perform(@SuppressWarnings("unchecked") AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        SCM scm = build.getProject().getScm();
        if (scm instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM ucm = (ClearCaseUcmSCM) scm;
            FilePath workspace = build.getWorkspace();
            ClearToolLauncher clearToolLauncher = ucm.createClearToolLauncher(listener, workspace, launcher);
            VariableResolver<String> variableResolver = new BuildVariableResolver(build);
            ClearTool clearTool = ucm.createClearTool(variableResolver, clearToolLauncher);
            if (build.getResult().equals(Result.SUCCESS)) {
                try {
                    String compositeBaselineName = Util.replaceMacro(compositeNamePattern, new BuildVariableResolver(build));
                    String pvob = UcmCommon.getVob(compositeStreamSelector);
                    makeCompositeBaseline(clearTool, compositeBaselineName, compositeStreamSelector, compositeComponentName, pvob);
                    promoteCompositeBaselineToBuiltLevel(clearTool, compositeBaselineName, pvob);
                    if (extractInfoFile) {
                        processExtractInfoFile(clearTool, this.compositeComponentName, pvob, compositeBaselineName, this.fileName);
                    }
                } catch (Exception ex) {
                    listener.getLogger().println("Failed to create baseline: " + ex);
                    return false;
                }
            } else {
                listener.getLogger().println("Build has failed, cannot create baseline composite");
                return false;
            }
        } else {
            listener.getLogger().println("Not a UCM clearcase SCM, cannot create baseline composite");
            return false;
        }
        return true;
    }

    /**
     * Retrieve all Clearcase UCM component (with pvob suffix) for a stream
     * 
     * @param stream the stream name like 'P_EngDesk_Product_3.2_int@\P_ORC'
     * @param clearTool the clearcase launcher
     * @return component list attached to the stream like ['DocGen_PapeeteDoc@\P_ORC','DocMgt_Modulo@\P_ORC']
     * @throws IOException
     * @throws InterruptedException
     */
    private List<String> getComponentList(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        String output = clearTool.lsstream(stream, null, "\"%[components]XCp\"");
        String comp[] = output.split(",\\s");
        List<String> result = new ArrayList<String>();
        final String prefix = "component:";
        for (String c : comp) {
            if (StringUtils.startsWith(c, prefix)) {
                result.add(StringUtils.difference(prefix, c));
            } else {
                throw new IOException("Invalid format for component in output. Must starts with 'component:' : " + c);
            }
        }

        return result;
    }

    /**
     * Pick up a view from a stream
     * 
     * @return a view attached to the stream
     * @throws InterruptedException 
     * @throws IOException 
     */
    private String getOneViewFromStream(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        String output = clearTool.lsstream(stream, null, "\"%[views]p\"");
        String resultLines[] = output.split("\n");
        if (resultLines.length == 0) {
            throw new IOException("There is no view attached to the stream '" + stream + "'");
        }
        String viewsLines = resultLines[resultLines.length - 1];
        return viewsLines.split(" ")[0];

    }

    /**
     * Make a composite baseline
     * 
     * @param compositeBaselineName the composite baseline name
     * @param compositeStream the composite UCM Clearcase stream with Pvob like : 'P_EngDesk_Product_3.2_int@\P_ORC'
     * @param compositeComponent the composite UCM Clearcase component name like 'C_ Build_EngDesk'
     * @param clearToolLauncher the ClearCase launcher
     * @param filePath the filepath
     * @throws Exception
     */
    private void makeCompositeBaseline(ClearTool clearTool, String compositeBaselineName, String compositeStream, String compositeComponent, String pvob) throws Exception {

        // Get a view containing the composite component
        String compositeView = getOneViewFromStream(clearTool, this.compositeStreamSelector);

        // Get the component list (with pvob suffix) for the stream
        List<String> componentList = getComponentList(clearTool, this.compositeStreamSelector);
        
        StringBuffer sb = new StringBuffer();
        for (String comp : componentList) {
            // Exclude the composite component
            if (!comp.contains(compositeComponent)) {
                sb.append(",").append(comp);
            }
        }
        sb.delete(0, 1);
        String dependsOn = sb.toString(); 

        clearTool.mkbl(compositeBaselineName, compositeView, null, true, false, Arrays.asList(compositeComponent), dependsOn, dependsOn);

    }

    /**
     * Promote the composite baseline
     * 
     * @param compositeBaselineName the composite baseline name
     * @param pvob the vob name
     * @param clearToolLauncher the clearcase launcher
     * @param filePath the filepath
     * @throws InterruptedException
     * @throws IOException
     */
    private void promoteCompositeBaselineToBuiltLevel(ClearTool clearTool, String compositeBaselineName, String pvob)
            throws InterruptedException, IOException {
        clearTool.setBaselinePromotionLevel(compositeBaselineName + "@" + pvob, DefaultPromotionLevel.BUILT);
    }

    /**
     * Retrieve the binding component for the current baseline
     * 
     * @param baseline the current baseline
     * @param clearToolLauncher
     * @param filePath
     * @return
     * @throws InterruptedException 
     * @throws IOException 
     * @throws Exception
     */
    private String getComponent(ClearTool clearTool, String baseline) throws IOException, InterruptedException {
        String output = clearTool.lsbl(baseline, "\"%[component]p\"");
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to make baseline, reason: " + output);
        }
        return output;
    }

    /**
     * Extract Composite baseline information in an external file
     * 
     * @param compositeComponnentName
     * @param pvob
     * @param compositeBaselineName
     * @param fileName
     * @param clearToolLauncher
     * @param filePath
     * @throws Exception
     */
    private void processExtractInfoFile(ClearTool clearTool, 
                                        String compositeComponnentName, 
                                        String pvob, 
                                        String compositeBaselineName,
                                        String fileName) 
        throws Exception {
        String output = clearTool.lsbl(compositeBaselineName + "@" + pvob, "\"%[depends_on]p\"");
        if (output.contains("cleartool: Error")) {
            throw new Exception("Failed to make baseline, reason: " + output);
        }

        String baselinesComp[] = output.split(" ");
        List<String> baselineList = Arrays.asList(baselinesComp);
        Collections.sort(baselineList);

        Writer writer = null;
        try {
            FilePath fp = new FilePath(clearTool.getLauncher().getLauncher().getChannel(), fileName);
            OutputStream outputStream = fp.write();
            writer = new OutputStreamWriter(outputStream);
            writer.write("The composite baseline is '" + compositeBaselineName + "'");
            for (String baseLine : baselineList) {
                writer.write("\nThe  baseline of component '" + getComponent(clearTool, baseLine) + "' is :" + baseLine);
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

    }

}
