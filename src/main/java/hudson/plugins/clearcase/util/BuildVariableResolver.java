/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
			/*if ("COMPUTERNAME".equals(key)) {
				return (Util.fixEmpty(StringUtils.isEmpty(launcher.getComputer().getName()) ? "master"
						: launcher.getComputer().getName()));
                                                }*/
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
