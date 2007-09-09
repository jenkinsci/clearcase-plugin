package hudson.plugins.clearcase;

import hudson.Plugin;
import hudson.scm.SCMS;

/**
 * Clear case plugin.
 * 
 * @author Erik Ramfelt
 */
public class PluginImpl extends Plugin {

	/**
	 * Registers Clear Case SCM.
	 */
	@Override
	public void start() throws Exception {
		SCMS.SCMS.add(ClearCaseSCM.DESCRIPTOR);
		super.start();
	}
}
