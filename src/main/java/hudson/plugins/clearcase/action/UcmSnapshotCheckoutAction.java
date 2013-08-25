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
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.ucm.service.FacadeService;
import hudson.plugins.clearcase.ucm.service.StreamService;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

public class UcmSnapshotCheckoutAction extends SnapshotCheckoutAction {

    private FacadeService facadeService;

    private final String  streamSelector;

    public UcmSnapshotCheckoutAction(ClearTool cleartool, String streamSelector, String[] loadRules, boolean useUpdate, String viewPath,
            ViewStorage viewStorage, FacadeService facadeService) {
        super(cleartool, loadRules, useUpdate, viewPath, viewStorage);
        this.streamSelector = streamSelector;
        this.facadeService = facadeService;
    }

    @Override
    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        boolean viewCreated = cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        // At this stage, we have a valid view and a valid path
        ClearTool ct = getCleartool();
        if (viewCreated) {
            // If the view is brand new, we just have to add the load rules
            try {
                ct.update2(viewPath, loadRules);
            } catch (IOException e) {
                launcher.getListener().fatalError(e.toString());
                return false;
            }
        } else {
            ConfigSpec viewConfigSpec = new ConfigSpec(ct.catcs(viewTag), launcher.isUnix());
            SnapshotCheckoutAction.LoadRulesDelta loadRulesDelta = getLoadRulesDelta(viewConfigSpec.getLoadRules(), launcher);
            if (!ArrayUtils.isEmpty(loadRulesDelta.getRemoved())) {
                try {
                    ct.setcs2(viewPath, SetcsOption.CONFIGSPEC, viewConfigSpec.setLoadRules(loadRules).getRaw());
                } catch (IOException e) {
                    launcher.getListener().fatalError(e.toString());
                    return false;
                }
            } else {
                String[] addedLoadRules = loadRulesDelta.getAdded();
                if (!ArrayUtils.isEmpty(addedLoadRules)) {
                    // Config spec haven't changed, but there are new load rules
                    try {
                        ct.update2(viewPath, addedLoadRules);
                    } catch (IOException e) {
                        launcher.getListener().fatalError(e.toString());
                        return false;
                    }
                }
            }

            // Perform a full update of the view to get changes due to rebase for instance.
            try {
                if (ct.doesSetcsSupportOverride()) {
                    ct.setcs2(viewPath, SetcsOption.STREAM, null);
                } else {
                    ct.update2(viewPath, null);
                }
            } catch (IOException e) {
                launcher.getListener().fatalError(e.toString());
                return false;
            }
        }
        return true;
    }

    public FacadeService getFacadeService() {
        return facadeService;
    }

    public void setFacadeService(FacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @Override
    protected boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException,
    InterruptedException {
        boolean viewCreated = super.cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        ClearTool ct = getCleartool();
        if (!viewCreated) {
            StreamService streamService = facadeService.getStreamService();
            if (!streamService.isViewAttachedTo(viewTag, streamService.parse(streamSelector))) {
                ct.getLauncher().getListener().getLogger().println("Deleting current view as it is attached to a different stream than " + streamSelector);
                ct.rmview(viewPath);
                createView(ct, viewTag, viewPath, streamSelector);
                viewCreated = true;
            }
        }
        return viewCreated;
    }
}
