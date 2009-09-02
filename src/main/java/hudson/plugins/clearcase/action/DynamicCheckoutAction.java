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
import hudson.plugins.clearcase.util.PathUtil;

import java.io.IOException;

/**
 * Check out action for dynamic views.
 * This will not update any files from the repository as it is a dynamic view.
 * The class will make sure that the configured config spec is the same as the one
 * for the dynamic view.
 */
public class DynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String configSpec;
    private boolean doNotUpdateConfigSpec;

    public DynamicCheckoutAction(ClearTool cleartool, String configSpec, boolean doNotUpdateConfigSpec) {
        this.cleartool = cleartool;
        this.configSpec = configSpec;
        this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException { 
        cleartool.startView(viewName);
        String currentConfigSpec = cleartool.catcs(viewName).trim();
        String tempConfigSpec = PathUtil.convertPathForOS(configSpec, launcher);
        if (!doNotUpdateConfigSpec && !tempConfigSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
            cleartool.setcs(viewName, tempConfigSpec);
        }
        return true;
    }


}
