package hudson.plugins.clearcase;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.FilePath;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.ModelObject;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.DefaultPollAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.TaggingAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.ucm.UcmChangeLogAction;
import hudson.plugins.clearcase.ucm.UcmChangeLogParser;
import hudson.plugins.clearcase.ucm.UcmSaveChangeLogAction;
import hudson.scm.ChangeLogParser;
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
    public ClearCaseUcmSCM(String stream, String loadrules, String viewname, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent) {
        super(viewname, mkviewoptionalparam, filterOutDestroySubBranchEvent);
        this.stream = shortenStreamName(stream);
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
    public ChangeLogParser createChangeLogParser() {
        return new UcmChangeLogParser();
    }

    @Override
    public String[] getBranchNames() {
        String branch = stream;
        if (stream.contains("@")) {
            branch = stream.substring(0, stream.indexOf("@"));
        }
        return new String[]{ branch };
    }

    @Override
    public String[] getViewPaths(FilePath viewPath) throws IOException, InterruptedException {
        String[] rules = loadRules.split("\n");
        for (int i = 0; i < rules.length; i++) {
            String rule = rules[i];
            // Remove "\\", "\" or "/" from the load rule. (bug#1706)
            // the user normally enters a load rule beginning with those chars
            while (rule.startsWith("\\") || rule.startsWith("/")) {
                rule = rule.substring(1);               
            }
            rules[i] = rule;
        }
        return rules;
    }

    @Override
    protected CheckOutAction createCheckOutAction(ClearToolLauncher launcher) {
        return new UcmSnapshotCheckoutAction(createClearTool(launcher), getStream(), getLoadRules());
    }

    @Override
    protected PollAction createPollAction(ClearToolLauncher launcher) {
        return new DefaultPollAction(createClearTool(launcher));
    }

    @Override
    protected ChangeLogAction createChangeLogAction(ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
        return new UcmChangeLogAction(createClearTool(launcher));
    }

    @Override
    protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
        return new UcmSaveChangeLogAction();
    }
    
    @Override
    protected TaggingAction createTaggingAction(ClearToolLauncher clearToolLauncher) {
        return null;
    }
    
    private String shortenStreamName(String longStream) {
        if (longStream.startsWith("stream:")) {
            return longStream.substring("stream:".length());
        }
        return longStream;
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
