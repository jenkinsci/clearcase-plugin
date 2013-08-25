/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.action.BaseSnapshotCheckoutAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.model.UcmSelector;
import hudson.plugins.clearcase.ucm.service.FacadeService;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;

public class FoundationBaselineUcmSnapshotCheckoutAction extends UcmSnapshotCheckoutAction {
    private BaseSnapshotCheckoutAction baseCheckoutAction;

    public FoundationBaselineUcmSnapshotCheckoutAction(ClearTool cleartool, String streamSelector, String[] loadRules, boolean useUpdate, String viewPath,
            ViewStorage viewStorage, FacadeService facadeService) throws IOException, InterruptedException {
        super(cleartool, streamSelector, loadRules, useUpdate, viewPath, viewStorage, facadeService);
        baseCheckoutAction = new BaseSnapshotCheckoutAction(cleartool, getConfigSpecOfFoundationBaseline(streamSelector), loadRules, useUpdate, viewPath,
                viewStorage);
    }

    @Override
    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        return baseCheckoutAction.checkout(launcher, workspace, viewTag);
    }

    @Override
    public boolean isViewValid(FilePath workspace, String viewTag) throws IOException, InterruptedException {
        return baseCheckoutAction.isViewValid(workspace, viewTag);
    }

    private ConfigSpec getConfigSpecOfFoundationBaseline(String streamSelector) throws IOException, InterruptedException {
        Stream stream = UcmSelector.parse("stream:" + streamSelector, Stream.class);
        Baseline[] foundationBaselines = getFacadeService().getStreamService().getFoundationBaselines(stream);
        ConfigSpec configSpec = getFacadeService().getBaselineService().generateConfigSpec(foundationBaselines);
        configSpec.setLoadRules(loadRules);
        return configSpec;
    }

}
