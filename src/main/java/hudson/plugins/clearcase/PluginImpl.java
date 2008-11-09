package hudson.plugins.clearcase;

import hudson.Plugin;
import hudson.plugins.clearcase.ucm.UcmMakeBaseline;
import hudson.plugins.clearcase.ucm.UcmMakeBaselineComposite;
import hudson.scm.SCMS;
import hudson.tasks.Publisher;

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
        Publisher.PUBLISHERS.add(UcmMakeBaseline.DESCRIPTOR);
        Publisher.PUBLISHERS.add(UcmMakeBaselineComposite.DESCRIPTOR);
        
        super.start();
    }
}
