package hudson.plugins.clearcase;

import hudson.Plugin;
import hudson.scm.SCMS;

/**
 * ClearCase plugin.
 * 
 * @author Erik Ramfelt
 */
public class PluginImpl extends Plugin {

    public static final ClearCaseSCM.ClearCaseScmDescriptor BASE_DESCRIPTOR = new ClearCaseSCM.ClearCaseScmDescriptor();
    public static final ClearCaseUcmSCM.ClearCaseUcmScmDescriptor  UCM_DESCRIPTOR = new ClearCaseUcmSCM.ClearCaseUcmScmDescriptor();

    public static ClearCaseSCM.ClearCaseScmDescriptor getDescriptor() {
        return BASE_DESCRIPTOR;
    }

    /**
     * Registers ClearCase SCM.
     */
    @Override
    public void start() throws Exception {
        SCMS.SCMS.add(BASE_DESCRIPTOR);
        SCMS.SCMS.add(UCM_DESCRIPTOR);
        super.start();
    }
}
