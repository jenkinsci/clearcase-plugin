package hudson.plugins.clearcase;

import hudson.Plugin;
import hudson.scm.SCMS;

/**
 * Clear case plugin.
 * 
 * @author Erik Ramfelt
 */
public class PluginImpl extends Plugin {

    public static final ClearCaseSCM.ClearCaseScmDescriptor DESCRIPTOR = new ClearCaseSCM.ClearCaseScmDescriptor();

    public static ClearCaseSCM.ClearCaseScmDescriptor getDescriptor() {
        return DESCRIPTOR;
    }

    /**
     * Registers Clear Case SCM.
     */
    @Override
    public void start() throws Exception {
        SCMS.SCMS.add(DESCRIPTOR);
        super.start();
    }
}
