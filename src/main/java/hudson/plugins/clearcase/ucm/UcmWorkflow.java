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

import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.service.FacadeService;
import hudson.plugins.clearcase.ucm.service.StreamService;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.SCMRevisionState;

import java.io.IOException;
import java.util.Date;

public abstract class UcmWorkflow {
    public abstract CheckoutAction createCheckoutAction(ClearTool cleartool, String stream, String[] viewPaths, String viewPath,
            ViewStorage decoratedViewStorage, AbstractBuild<?, ?> abstractBuild) throws IOException, InterruptedException;

    public abstract UcmHistoryAction createHistoryAction(ClearTool cleartool, Filter filter, String stream, AbstractBuild<?, ?> build,
            SCMRevisionState oldBaseline, SCMRevisionState newBaseline, String extendedViewPath);

    public SCMRevisionState createRevisionState(ClearTool clearTool, TaskListener taskListener, Date date, String streamSelector, String[] loadRules)
            throws IOException, InterruptedException {
        StreamService streamService = getFacadeService(clearTool).getStreamService();
        Stream stream = streamService.parse(streamSelector);
        Baseline[] foundationBaselines = streamService.getFoundationBaselines(stream);
        return new UcmRevisionState(foundationBaselines, loadRules, date.getTime());
    }

    public String[] getAllRootDirsFor(ClearTool clearTool, String streamSelector) throws IOException, InterruptedException {
        return getFacadeService(clearTool).getAllRootDirsFor(streamSelector);
    }

    protected FacadeService getFacadeService(ClearTool clearTool) {
        return new FacadeService(clearTool);
    }

    protected UcmRevisionState toUcm(SCMRevisionState oldBaseline) {
        if (oldBaseline instanceof UcmRevisionState) {
            return (UcmRevisionState) oldBaseline;
        }
        return null;
    }

}
