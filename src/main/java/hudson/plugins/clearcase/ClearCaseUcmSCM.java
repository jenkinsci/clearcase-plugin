package hudson.plugins.clearcase;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.model.ModelObject;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.DefaultPollAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;

/**
 * SCM for ClearCaseUCM.
 * This SCM will create a UCM view from a stream and apply a list of load rules to it.
 */
public class ClearCaseUcmSCM extends AbstractClearCaseScm {

    private final String stream;
    private final String loadRules;

    @DataBoundConstructor
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, String vobpaths,
            String mkviewoptionalparam) {
        super(viewname, vobpaths, mkviewoptionalparam);
        this.stream = stream;
        this.loadRules = loadrules;
    }

    /**
     * Return the load rules for the UCM view.
     * @return string containing the load rules.
     */
    public String getLoadRules() {
        return loadRules;
    }

    /**
     * Return the stream that is used to create the UCM view.
     * @return string containing the stream selector.
     */
    public String getStream() {
        return stream;
    }

    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return PluginImpl.UCM_DESCRIPTOR;
    }

    @Override
    public String[] getBranchNames() {
        return new String[]{ stream };
    }

    @Override
    protected CheckOutAction createCheckOutAction(ClearToolLauncher launcher) {
        return new UcmSnapshotCheckoutAction(createClearTool(launcher), getViewName(), getStream(), getLoadRules());
    }

    @Override
    protected PollAction createPollAction(ClearToolLauncher launcher) {
        return new DefaultPollAction(createClearTool(launcher));
    }
    
    private ClearTool createClearTool(ClearToolLauncher launcher) {
        return new ClearToolSnapshot(launcher, PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), getMkviewOptionalParam());
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
            return "UCM ClearCase";
        }

        @Override
        public boolean configure(StaplerRequest req) {
            return true;
        }

        @Override
        public SCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            return req.bindJSON(ClearCaseUcmSCM.class, formData);
        }
    }
}
