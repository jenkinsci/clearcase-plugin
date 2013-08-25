/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe, Rinat Ailon
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
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.io.Serializable;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Display ClearCase information report for build
 * 
 * @author Rinat Ailon
 */
public class ClearCasePublisher extends Notifier implements Serializable {
    /**
     * All global configurations in global.jelly are done from the DescriptorImpl class below
     * 
     * @author rgoren
     */
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

        /*
         * This initializes the global configuration when loaded
         */

        public DescriptorImpl() {
            super(ClearCasePublisher.class);
            // This makes sure any existing global configuration is read from the persistence file <Hudson work
            // dir>/hudson.plugins.logparser.LogParserPublisher.xml
            load();
        }

        /*
         * This method is invoked when the global configuration "save" is pressed
         */
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            save();
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Create ClearCase report";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clearcase/publisher.html";
        }

        @Override
        public boolean isApplicable(Class jobType) {
            return true;
        }

    }

    private static final long serialVersionUID = 1L;

    @DataBoundConstructor
    public ClearCasePublisher() {

    }

    @Override
    public BuildStepDescriptor getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        try {
            ClearCaseReportAction action = new ClearCaseReportAction(build);
            build.getActions().add(action);

        } catch (Exception e) {
            // failure to parse should not fail the build
            e.printStackTrace();
        }

        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

}
