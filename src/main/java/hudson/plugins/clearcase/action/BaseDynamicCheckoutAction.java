/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
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
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.MkViewParameters;
import hudson.plugins.clearcase.ViewType;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Check out action for dynamic views. This will not update any files from the
 * repository as it is a dynamic view. The class will make sure that the
 * configured config spec is the same as the one for the dynamic view.
 */
public class BaseDynamicCheckoutAction extends CheckoutAction {

    private String              configSpec;
    private boolean             updateConfigSpec;
    private boolean             useTimeRule;
    private boolean             createView;
    private AbstractBuild<?, ?> build;

    public BaseDynamicCheckoutAction(ClearTool cleartool, String configSpec, boolean doNotUpdateConfigSpec,
            boolean useTimeRule, boolean createView, ViewStorage viewStorage,
            AbstractBuild<?, ?> build) {
        super(cleartool, viewStorage);
        this.configSpec = configSpec;
        this.updateConfigSpec = !doNotUpdateConfigSpec;
        this.useTimeRule = useTimeRule;
        this.createView = createView;
        this.build = build;
    }

    @Override
    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException,
            InterruptedException {
        if (createView) {
            createView(viewTag);
        }
        startView(viewTag);

        String currentConfigSpec = getCleartool().catcs(viewTag).trim();

        if (updateConfigSpec) {
            currentConfigSpec = updateConfigSpec(launcher, viewTag, currentConfigSpec);
        }

        // add config spec to dataAction
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null) {
            dataAction.setCspec(currentConfigSpec);
        }

        return true;
    }

    private String updateConfigSpec(Launcher launcher, String viewTag, String currentConfigSpec) throws IOException,
            InterruptedException {
        String futureConfigSpec = PathUtil.convertPathForOS(getConfigSpec(), launcher);
        if (currentAndFutureConfigSpecAreEquals(currentConfigSpec, futureConfigSpec)) {
            getCleartool().setcs2(viewTag, SetcsOption.CURRENT, null);
        } else {
            getCleartool().setcs2(viewTag, SetcsOption.CONFIGSPEC, futureConfigSpec);
        }
        return futureConfigSpec;
    }

    private boolean currentAndFutureConfigSpecAreEquals(String current, String future) {
        return future.trim().replaceAll("\r\n", "\n").equals(current);
    }

    private String getConfigSpec() {
        if (useTimeRule) {
            return "time " + getTimeRule() + "\n" + configSpec + "\nend time\n";
        } else {
            return configSpec;
        }
    }

    private void startView(String viewTag) throws IOException, InterruptedException {
        getCleartool().startView(viewTag);
    }

    private void createView(String viewTag) throws IOException, InterruptedException {
        // Remove current view
        if (getCleartool().doesViewExist(viewTag)) {
            getCleartool().rmviewtag(viewTag);
        }
        // Now, make the view.
        MkViewParameters params = new MkViewParameters();
        params.setType(ViewType.Dynamic);
        params.setViewPath(viewTag);
        params.setViewTag(viewTag);
        params.setViewStorage(getViewStorage());
        getCleartool().mkview(params);
    }

    public String getTimeRule() {
        return getTimeRule(new Date());
    }

    public String getTimeRule(Date nowDate) {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'Z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        return formatter.format(nowDate).toLowerCase();
    }

    /**
     * @deprecated Use {@link #isViewValid(FilePath,String)} instead
     */
    @Deprecated
    @Override
    public boolean isViewValid(Launcher launcher, FilePath workspace, String viewTag) throws IOException,
            InterruptedException {
                return isViewValid(workspace, viewTag);
            }

    @Override
    public boolean isViewValid(FilePath workspace, String viewTag) throws IOException,
            InterruptedException {
        if (getCleartool().doesViewExist(viewTag)) {
            startView(viewTag);
            return true;
        }
        return false;
    }
}
