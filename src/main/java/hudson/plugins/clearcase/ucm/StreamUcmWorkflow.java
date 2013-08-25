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
import hudson.plugins.clearcase.AbstractClearCaseScm.ChangeSetLevel;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.action.UcmSnapshotCheckoutAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.SCMRevisionState;

public class StreamUcmWorkflow extends UcmWorkflow {
    private ChangeSetLevel changesetLevel;
    private boolean        createDynamicView;
    private boolean        recreateDynamicView;
    private boolean        useDynamicView;

    private boolean        useUpdate;

    private String         viewDrive;

    @Override
    public CheckoutAction createCheckoutAction(ClearTool cleartool, String stream, String[] viewPaths, String viewPath, ViewStorage decoratedViewStorage,
            AbstractBuild<?, ?> build) {
        if (useDynamicView) {
            return new UcmDynamicCheckoutAction(cleartool, stream, createDynamicView, decoratedViewStorage, build, false, recreateDynamicView);
        }
        return new UcmSnapshotCheckoutAction(cleartool, stream, viewPaths, useUpdate, viewPath, decoratedViewStorage, getFacadeService(cleartool));
    }

    @Override
    public UcmHistoryAction createHistoryAction(ClearTool cleartool, Filter filter, String stream, AbstractBuild<?, ?> build, SCMRevisionState oldBaseline,
            SCMRevisionState newBaseline, String extendedViewPath) {
        UcmHistoryAction action;
        action = new UcmHistoryAction(cleartool, useDynamicView, filter, toUcm(oldBaseline), toUcm(newBaseline), changesetLevel, getFacadeService(cleartool));
        action.setExtendedViewPath(extendedViewPath);
        return action;
    }

    public void setChangesetLevel(ChangeSetLevel changesetLevel) {
        this.changesetLevel = changesetLevel;
    }

    public void setCreateDynamicView(boolean createDynamicView) {
        this.createDynamicView = createDynamicView;
    }

    public void setRecreateDynamicView(boolean recreateView) {
        this.recreateDynamicView = recreateView;
    }

    public void setUseDynamicView(boolean useDynamicView) {
        this.useDynamicView = useDynamicView;
    }

    public void setUseUpdate(boolean useUpdate) {
        this.useUpdate = useUpdate;
    }

    public void setViewDrive(String viewDrive) {
        this.viewDrive = viewDrive;
    }

    protected ChangeSetLevel getChangesetLevel() {
        return changesetLevel;
    }

    protected String getViewDrive() {
        return viewDrive;
    }

    protected boolean isCreateDynamicView() {
        return createDynamicView;
    }

    protected boolean isRecreateDynamicView() {
        return recreateDynamicView;
    }

    protected boolean isUseDynamicView() {
        return useDynamicView;
    }

    protected boolean isUseUpdate() {
        return useUpdate;
    }

}
