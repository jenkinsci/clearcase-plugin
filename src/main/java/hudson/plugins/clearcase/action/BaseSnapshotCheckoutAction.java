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
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.CleartoolUpdateResult;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

/**
 * Check out action that will check out files into a snapshot view.
 */
public class BaseSnapshotCheckoutAction extends SnapshotCheckoutAction {

    private AbstractBuild    build;
    private final ConfigSpec configSpec;
    private FilePath         updtFile;

    public BaseSnapshotCheckoutAction(ClearTool cleartool, ConfigSpec configSpec, String[] loadRules, boolean useUpdate, String viewPath,
            ViewStorage viewStorage) {
        this(cleartool, configSpec, loadRules, useUpdate, viewPath, viewStorage, null);
    }

    public BaseSnapshotCheckoutAction(ClearTool cleartool, ConfigSpec configSpec, String[] loadRules, boolean useUpdate, String viewPath,
            ViewStorage viewStorage, AbstractBuild build) {
        super(cleartool, loadRules, useUpdate, viewPath, viewStorage);
        this.configSpec = configSpec;
        this.build = build;
    }

    @Override
    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        boolean viewCreated = cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, null);

        // At this stage, we have a valid view and a valid path
        boolean needSetCs = true;
        SnapshotCheckoutAction.LoadRulesDelta loadRulesDelta = null;
        if (!viewCreated) {
            ConfigSpec viewConfigSpec = new ConfigSpec(getCleartool().catcs(viewTag), launcher.isUnix());
            loadRulesDelta = getLoadRulesDelta(viewConfigSpec.getLoadRules(), launcher);
            needSetCs = !configSpec.stripLoadRules().equals(viewConfigSpec.stripLoadRules()) || !ArrayUtils.isEmpty(loadRulesDelta.getRemoved());
        }
        try {
            CleartoolUpdateResult result = null;
            if (needSetCs) {
                result = getCleartool().setcs2(viewPath, SetcsOption.CONFIGSPEC, configSpec.setLoadRules(loadRules).getRaw());
            } else {
                int updateRetries = 2;
                while (updateRetries-- > 0) {
                    try {
                        // Perform a full update of the view to reevaluate
                        // config spec
                        result = getCleartool().setcs2(viewPath,SetcsOption.CURRENT, null);
                        break;
                    } catch (IOException ioExc) {
                        try {
                            // probably the reason is previous updated was interrupted, calling update will clean it
                            getCleartool().update2(viewPath, loadRules);
                        } catch (IOException ioExc2) {
                            // skip silently - update may throw an exception but it will clear the view and allow setcs to proceed
                        }
                    }
                }
                String[] addedLoadRules = loadRulesDelta.getAdded();
                if (!ArrayUtils.isEmpty(addedLoadRules)) {
                    // Config spec haven't changed, but there are new load rules
                    result = getCleartool().update2(viewPath, addedLoadRules);
                }
            }
            if (result.hasUpdateFile()) {
                updtFile = result.getUpdateFile();
                launcher.getListener().getLogger().println("[INFO] updt file name: '" + updtFile.getRemote() + "'");
            }
        } catch (IOException e) {
            launcher.getListener().fatalError(e.toString());
            return false;
        }

        if (build != null) {
            // add config spec to dataAction
            ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
            if (dataAction != null) {
                dataAction.setCspec(getCleartool().catcs(viewTag).trim());
            }
        }

        return true;
    }

    public ConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public FilePath getUpdtFile() {
        return updtFile;
    }

}
