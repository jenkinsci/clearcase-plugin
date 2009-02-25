package hudson.plugins.clearcase.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.util.VariableResolver;

/**
 * A {@link VariableResolver} that resolves certain Build variables.
 * <p>
 * The build variable resolver will resolve the following:
 * <ul>
 * <li>JOB_NAME - The name of the job</li>
 * <li>USER_NAME - The system property "user.name" on the Node that the Launcher
 * is being executed on (slave or master)</li>
 * <li>NODE_NAME - The name of the node that the Launcher is being executed on</li>
 * <li>Any environment variable that is set on the Node that the Launcher is
 * being executed on (slave or master)</li>
 * </ul>
 * 
 * Implementation note: This class is modelled after Erik Ramfelt's work in the
 * Team Foundation Server Plugin. Maybe they should be merged and moved to the
 * hudson core
 * 
 * @author Henrik Lynggaard Hansen
 */
public class BuildVariableResolver implements VariableResolver<String> {

	private static final Logger LOGGER = Logger
			.getLogger(BuildVariableResolver.class.getName());

	private final Launcher launcher;


	private AbstractBuild<?, ?> build;

	public BuildVariableResolver(final AbstractBuild<?, ?> build,
			final Launcher launcher) {
		this.build = build;
	
		this.launcher = launcher;
	}

	@Override
	public String resolve(String key) {
		try {
			if ("JOB_NAME".equals(key) && build != null && build.getProject() != null) {
				return build.getProject().getName();
			}
			if ("COMPUTERNAME".equals(key)) {
				return (Util.fixEmpty(StringUtils.isEmpty(launcher.getComputer().getName()) ? "master"
						: launcher.getComputer().getName()));
			}
			if ("NODE_NAME".equals(key)) {
				return (Util.fixEmpty(StringUtils.isEmpty(launcher.getComputer().getName()) ? "master"
						: launcher.getComputer().getName()));
			}

			if ("USER_NAME".equals(key)) {
				return (String) launcher.getComputer().getSystemProperties()
						.get("user.name");
			}
			if (build.getEnvVars().containsKey(key)) {
				return build.getEnvVars().get(key);
			}
		
			
		} catch (Exception e) {
			LOGGER.warning("Variable name '" + key
					+ "' look up failed because of " + e);
		}
		return null;
	}
}
