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
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.SCMRevisionState;

public class FreezeCodeStreamUcmWorkflow extends StreamUcmWorkflow {

    @Override
    public CheckoutAction createCheckoutAction(ClearTool cleartool, String stream, String[] viewPaths, String viewPath, ViewStorage decoratedViewStorage,
            AbstractBuild<?, ?> build) {
        if (isUseDynamicView()) {
            return new UcmDynamicCheckoutAction(cleartool, stream, isCreateDynamicView(), decoratedViewStorage, build, true, isRecreateDynamicView());
        }
        return new UcmSnapshotCheckoutAction(cleartool, stream, viewPaths, isUseUpdate(), viewPath, decoratedViewStorage, getFacadeService(cleartool));
    }

    @Override
    public UcmHistoryAction createHistoryAction(ClearTool cleartool, Filter filter, String stream, AbstractBuild<?, ?> build, SCMRevisionState oldBaseline,
            SCMRevisionState newBaseline, String extendedViewPath) {
        UcmHistoryAction action = new FreezeCodeUcmHistoryAction(cleartool, isUseDynamicView(), filter, stream, getViewDrive(), build, toUcm(oldBaseline),
                toUcm(newBaseline), getFacadeService(cleartool));
        action.setExtendedViewPath(extendedViewPath);
        return action;
    }

}
