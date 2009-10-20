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

import java.io.IOException;

/**
 * Check out action for dynamic views.
 * This will not update any files from the repository as it is a dynamic view.
 * It only makes sure the view is started as config specs don't exist in UCM
 */
public class UcmDynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String stream;
    private boolean createDynView;
    
    public UcmDynamicCheckoutAction(ClearTool cleartool, String stream, boolean createDynView) {
        super();
        this.cleartool = cleartool;
        this.stream = stream;
        this.createDynView = createDynView;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {        
        if (createDynView) {
            // Clean out the workspace first - deleting the files will probably be faster than 
            workspace.deleteContents();
            // Mount all VOBs before we get started.
            cleartool.mountVobs();
            // Get the view UUID.
            String uuid = cleartool.getViewUuid(viewName);
            // If we don't find a UUID, then the view tag must not exist, in which case we don't
            // have to delete it anyway.
            if (!uuid.equals("")) {
                cleartool.rmviewUuid(uuid);
                cleartool.unregisterView(uuid);
                cleartool.rmviewtag(viewName);
            }
            // Now, make the view.
            cleartool.mkview(viewName, stream);
        }
        cleartool.startView(viewName);
        cleartool.syncronizeViewWithStream(viewName, stream);
        
        return true;
    }

}
