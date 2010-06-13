/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.ClearTool.SetcsOption;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

/**
 * Check out action that will check out files into a snapshot view.
 */
public class SnapshotCheckoutAction extends AbstractCheckoutAction {

    private final ConfigSpec configSpec;
    
    private final String viewPath;

    public SnapshotCheckoutAction(ClearTool cleartool, ConfigSpec configSpec, String[] loadRules, boolean useUpdate, String viewPath) {
        super(cleartool, loadRules, useUpdate);
        this.configSpec = configSpec;
        this.viewPath = viewPath;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        boolean viewCreated = cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, null);

        // At this stage, we have a valid view and a valid path
        boolean needSetCs = true;
        AbstractCheckoutAction.LoadRulesDelta loadRulesDelta = null;
        if (!viewCreated) {
            ConfigSpec viewConfigSpec = new ConfigSpec(cleartool.catcs(viewTag), launcher.isUnix());
            loadRulesDelta = getLoadRulesDelta(viewConfigSpec.getLoadRules(), launcher);
            needSetCs = !configSpec.stripLoadRules().equals(viewConfigSpec.stripLoadRules()) || !ArrayUtils.isEmpty(loadRulesDelta.getRemoved());
        }

        if (needSetCs) {
            try {
                cleartool.setcs(viewPath, SetcsOption.CONFIGSPEC, configSpec.setLoadRules(loadRules).getRaw());
            } catch (IOException e) {
                launcher.getListener().fatalError(e.toString());
                return false;
            }
        } else {
            String[] addedLoadRules = loadRulesDelta.getAdded();
            if (!ArrayUtils.isEmpty(addedLoadRules)) {
                // Config spec haven't changed, but there are new load rules
                try {
                    cleartool.update(viewPath, addedLoadRules);
                } catch (IOException e) {
                    launcher.getListener().fatalError(e.toString());
                    return false;
                }
            }
        }
        // Perform a full update of the view. to reevaluate config spec
        if (!viewCreated) {
            try {
                cleartool.update(viewPath, null);
            } catch (IOException e) {
                launcher.getListener().fatalError(e.toString());
                return false;
            }
        }
        return true;
    }

    public ConfigSpec getConfigSpec() {
        return configSpec;
    }

}
