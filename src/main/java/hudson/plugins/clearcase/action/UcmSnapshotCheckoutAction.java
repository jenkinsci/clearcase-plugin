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
package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.PathUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Check out action that will check out files into a UCM snapshot view. Checking
 * out the files will also update the load rules in the view.
 */
public class UcmSnapshotCheckoutAction implements CheckOutAction {

	private ClearTool cleartool;

	private String stream;

	private String loadRules;

	private boolean useUpdate;

	public UcmSnapshotCheckoutAction(ClearTool cleartool, String stream,
			String loadRules, boolean useUpdate) {
		super();
		this.cleartool = cleartool;
		this.stream = stream;
		this.loadRules = loadRules;
		this.useUpdate = useUpdate;
	}

	public boolean checkout(Launcher launcher, FilePath workspace,
			String viewName) throws IOException, InterruptedException {
		boolean localViewPathExists = new FilePath(workspace, viewName)
				.exists();
		if (this.useUpdate) {
			if (localViewPathExists) {
                                String configSpec = PathUtil.convertPathsBetweenUnixAndWindows(cleartool.catcs(viewName), launcher);
				Set<String> configSpecLoadRules = extractLoadRules(configSpec);
				boolean recreate = currentConfigSpecUptodate(configSpecLoadRules);
				if (recreate) {
					cleartool.rmview(viewName);
					cleartool.mkview(viewName, stream);
				}
			} else {
				cleartool.mkview(viewName, stream);
			}

		} else {
			if (localViewPathExists) {
				cleartool.rmview(viewName); 
			}
			cleartool.mkview(viewName, stream);
		}
		for (String loadRule : loadRules.split("\n")) {
			cleartool.update(viewName, loadRule.trim());
		}
		return true;
	}

	private boolean currentConfigSpecUptodate(Set<String> configSpecLoadRules) {
		boolean recreate = false;
		for (String loadRule : loadRules.split("\n")) {
			if (!configSpecLoadRules.contains(loadRule.replace("\r",""))) {
				System.out
						.println("Load rule: "
								+ loadRule
								+ " not found in current config spec, forcing recreation of view");
				recreate = true;
			}
		}
		return recreate;
	}

	private Set<String> extractLoadRules(String configSpec) {
		Set<String> rules = new HashSet<String>();
		for (String row : configSpec.split("\n")) {
			String trimmedRow = row.toLowerCase().trim();
			if (trimmedRow.startsWith("load")) {
				String rule = row.trim().substring("load".length()).trim();
				rules.add(rule);
				if ((!rule.startsWith("/")) && (!rule.startsWith("\\"))) {
                                    rules.add(rule);
                                } else {
					rules.add(rule.substring(1));
                                }
			}
		}
		return rules;
	}

}
