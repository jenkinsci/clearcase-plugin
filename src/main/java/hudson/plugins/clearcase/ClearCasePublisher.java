package hudson.plugins.clearcase;

import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.*;
import hudson.Util;
import static hudson.Util.fixEmpty;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Action;
import hudson.remoting.RemoteOutputStream;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormFieldValidator;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.types.FileSet;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.kohsuke.stapler.StaplerRequest;

//import hudson.plugins.logparser.util.BuildResult;


/**
 * Display ClearCase information report  for build
 * 
 *  @author Rinat Ailon
 */
public class ClearCasePublisher extends Publisher implements Serializable {
    @DataBoundConstructor
    public ClearCasePublisher() {

   }

    public boolean prebuild(AbstractBuild<?,?> build, BuildListener listener) {
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

    /**
     * Indicates an orderly abortion of the processing.
     */
    private static final class AbortException extends RuntimeException {
    }



    public Descriptor<Publisher> getDescriptor() {
        return DescriptorImpl.DESCRIPTOR;
    }
    
    /**
     * All global configurations in global.jelly are done from the DescriptorImpl class below
     * @author rgoren
     *
     */
    public static final class DescriptorImpl extends Descriptor<Publisher> {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
        /*
         * This initializes the global configuration when loaded
         */

        private ClearCaseSCM.ClearCaseScmDescriptor scmDescriptor;
        
        public DescriptorImpl() {
            super(ClearCasePublisher.class);
            // This makes sure any existing global configuration is read from the persistence file <Hudson work dir>/hudson.plugins.logparser.LogParserPublisher.xml
			load();
        }
        
        public DescriptorImpl(ClearCaseSCM.ClearCaseScmDescriptor scmDescriptor) {
            super(ClearCasePublisher.class);
            // This makes sure any existing global configuration is read from the persistence file <Hudson work dir>/hudson.plugins.logparser.LogParserPublisher.xml
			load();
        	this.scmDescriptor = scmDescriptor;
        }
        
        public String getDisplayName() {
            return "Create ClearCase report";
        }

        public String getHelpFile() {
            return "/plugin/clearcase/publisher.html";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
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
		return BuildStepMonitor.NONE ;
	}
	
	
}

