package hudson.plugins.clearcase;

import static hudson.Util.fixEmpty;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormFieldValidator;

public class ClearCaseUcmSCM extends SCM {

    private String stream;
    private String branch;
    private boolean useUpdate;
    private String viewName;
    private String vobPaths;
    private String mkviewOptionalParam;

    @DataBoundConstructor
    public ClearCaseUcmSCM(String stream, String branch, String viewname, boolean useupdate, String vobpaths,
            String mkviewoptionalparam) {
        this.stream = stream;
        this.branch = branch;
        this.viewName = viewname;
        this.useUpdate = useupdate;
        this.vobPaths = vobpaths;
        this.mkviewOptionalParam = mkviewoptionalparam;
    }
    
    public String getBranch() {
        return branch;
    }

    public boolean isUseUpdate() {
        return useUpdate;
    }

    public String getViewName() {
        return viewName;
    }

    public String getVobPaths() {
        return vobPaths;
    }

    public String getMkviewOptionalParam() {
        return mkviewOptionalParam;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return PluginImpl.UCM_DESCRIPTOR;
    }

    @Override
    public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
            File changelogFile) throws IOException, InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener)
            throws IOException, InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ClearCaseChangeLogParser();
    }

    /**
     * ClearCase UCM SCM descriptor
     * 
     * @author Erik Ramfelt
     */
    public static final class ClearCaseUcmScmDescriptor extends SCMDescriptor<ClearCaseUcmSCM> 
            implements ModelObject {

        protected ClearCaseUcmScmDescriptor() {
            super(ClearCaseUcmSCM.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM";
        }

        @Override
        public boolean configure(StaplerRequest req) {
            return true;
        }


        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {
            return req.bindJSON(ClearCaseUcmSCM.class, formData);
        }

        public void doStreamCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req, rsp, false) {
                @Override
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if (v == null) {
                        error("Stream selector is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }
    }
}
