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

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.ucm.service.FacadeService;
import hudson.scm.ChangeLogSet.Entry;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class FoundationBaselineUcmHistoryAction extends UcmHistoryAction {

    static final Logger LOG = Logger.getLogger(FoundationBaselineUcmHistoryAction.class.getName());

    public FoundationBaselineUcmHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, UcmRevisionState oldBaseline,
            UcmRevisionState newBaseline, FacadeService facadeService) {
        super(cleartool, useDynamicView, filter, oldBaseline, newBaseline, null, facadeService);
    }

    @Override
    public List<Entry> getChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
    InterruptedException {
        return getChangesOnBaseline(time, viewPath, viewTag, branchNames, viewPaths);
    }

    @Override
    public boolean hasChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        return hasChangesOnBaseline(time, viewPath, viewTag, branchNames, viewPaths);
    }

}
