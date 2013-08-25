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

import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.SCMRevisionState;

import java.io.IOException;

public class FoundationBaselineUcmWorkflow extends UcmWorkflow {
    private boolean useDynamicView;
    private boolean useUpdate;

    @Override
    public CheckoutAction createCheckoutAction(ClearTool cleartool, String stream, String[] viewPaths, String viewPath, ViewStorage decoratedViewStorage,
            AbstractBuild<?, ?> abstractBuild) throws IOException, InterruptedException {
        return new FoundationBaselineUcmSnapshotCheckoutAction(cleartool, stream, viewPaths, useUpdate, viewPath, decoratedViewStorage,
                getFacadeService(cleartool));
    }

    @Override
    public UcmHistoryAction createHistoryAction(ClearTool cleartool, Filter filter, String stream, AbstractBuild<?, ?> build, SCMRevisionState oldBaseline,
            SCMRevisionState newBaseline, String extendedViewPath) {
        UcmRevisionState oldUcm = toUcm(oldBaseline);
        UcmRevisionState newUcm = toUcm(newBaseline);
        return new FoundationBaselineUcmHistoryAction(cleartool, useDynamicView, filter, oldUcm, newUcm, getFacadeService(cleartool));
    }

    public void setUseDynamicView(boolean useDynamicView) {
        this.useDynamicView = useDynamicView;
    }

    public void setUseUpdate(boolean useUpdate) {
        this.useUpdate = useUpdate;
    }

}
