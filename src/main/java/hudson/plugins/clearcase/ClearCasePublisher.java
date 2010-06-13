package hudson.plugins.clearcase;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;

import java.io.IOException;
import java.io.Serializable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Display ClearCase information report for build
 * 
 * @author Rinat Ailon
 */
public class ClearCasePublisher extends Notifier implements Serializable {
    @DataBoundConstructor
    public ClearCasePublisher() {

    }

    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        return true;
    }

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

    public BuildStepDescriptor getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }

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
        
        @Override
        public boolean isApplicable(Class jobType) {
            return true;
        }

        public String getDisplayName() {
            return "Create ClearCase report";
        }

        public String getHelpFile() {
            return "/plugin/clearcase/publisher.html";
        }

        /*
         * This method is invoked when the global configuration "save" is pressed
         */
        @Override
        public boolean configure(StaplerRequest req) throws FormException {
            save();
            return true;
        }

    }

    private static final long serialVersionUID = 1L;

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

}
