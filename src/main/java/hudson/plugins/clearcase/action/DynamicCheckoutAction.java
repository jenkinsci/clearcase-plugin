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

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

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
    private boolean useTimeRule;
    private boolean createDynView;
    private String winDynStorageDir;
    private String unixDynStorageDir;

    public DynamicCheckoutAction(ClearTool cleartool, String configSpec, boolean doNotUpdateConfigSpec, boolean useTimeRule,
                                 boolean createDynView, String winDynStorageDir,String unixDynStorageDir) {
        this.cleartool = cleartool;
        this.configSpec = configSpec;
        this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
        this.useTimeRule = useTimeRule;
        this.createDynView = createDynView;
        this.winDynStorageDir = winDynStorageDir;
        this.unixDynStorageDir = unixDynStorageDir;
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
            	try {
            		cleartool.rmviewUuid(uuid);	
            	}
            	catch (Exception ex) {
            		cleartool.logRedundantCleartoolError(null, ex);
            	}
            	
            	try {
            		cleartool.unregisterView(uuid);	
            	}
            	catch (Exception ex) {
            		cleartool.logRedundantCleartoolError(null, ex);
            	}            	               
                
                cleartool.rmviewtag(viewName);
            }
            // Now, make the view.
            String dynStorageDir = cleartool.getLauncher().getLauncher().isUnix() ? unixDynStorageDir : winDynStorageDir; 
            cleartool.mkview(viewName, null, dynStorageDir);
        }
        
        cleartool.startView(viewName);
        String currentConfigSpec = cleartool.catcs(viewName).trim();
        String tempConfigSpec;

        if (useTimeRule) {
            tempConfigSpec = PathUtil.convertPathForOS("time " + getTimeRule() + "\n" + configSpec + "\nend time\n",
                                                       launcher);
        }
        else {
            tempConfigSpec = PathUtil.convertPathForOS(configSpec, launcher);
        }
        
        if (!doNotUpdateConfigSpec) {
            if (!tempConfigSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
                cleartool.setcs(viewName, tempConfigSpec);
            }
            else {
                cleartool.setcs(viewName, null);
            }
        }
        return true;
    }

    public String getTimeRule() {
        return getTimeRule(new Date());
    }

    public String getTimeRule(Date nowDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'Z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(nowDate).toLowerCase();
    }
}
