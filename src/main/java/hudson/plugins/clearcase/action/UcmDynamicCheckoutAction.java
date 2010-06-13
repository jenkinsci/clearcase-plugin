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
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.ucm.UcmCommon;
import hudson.plugins.clearcase.ucm.UcmCommon.BaselineDesc;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Check out action for dynamic views. This will not update any files from the repository as it is a dynamic view. It
 * only makes sure the view is started as config specs don't exist in UCM
 */
public class UcmDynamicCheckoutAction implements CheckOutAction {
    private static final String CONFIGURED_STREAM_VIEW_SUFFIX = "_hudson_view";
    private static final String BUILD_STREAM_PREFIX = "hudson_stream.";
    private static final String BASELINE_NAME = "hudson_co_";
    private static final String BASELINE_COMMENT = "hudson_co_";

    private ClearTool cleartool;
    private String stream;
    private boolean createDynView;
    private String winDynStorageDir;
    private String unixDynStorageDir;
    private AbstractBuild build;
    private boolean freezeCode;
    private boolean recreateView;

    public UcmDynamicCheckoutAction(ClearTool cleartool, String stream, boolean createDynView, String winDynStorageDir, String unixDynStorageDir,
            AbstractBuild build, boolean freezeCode, boolean recreateView) {
        super();
        this.cleartool = cleartool;
        this.stream = stream;
        this.createDynView = createDynView;
        this.winDynStorageDir = winDynStorageDir;
        this.unixDynStorageDir = unixDynStorageDir;
        this.build = build;
        this.freezeCode = freezeCode;
        this.recreateView = recreateView;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        // add stream to data action (to be used by ClearCase report)
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null) {
            // sync the project in order to allow other builds to safely check if there is
            // already a build running on the same stream
            synchronized (build.getProject()) {
                dataAction.setStream(stream);
            }
        }
        PrintStream logger = launcher.getListener().getLogger();
        logger.println("createDynView = " + createDynView);
        logger.println("freezeCode = " + freezeCode);
        logger.println("recreateView = " + recreateView);
        if (createDynView) {
            if (freezeCode) {
                checkoutCodeFreeze(viewTag);
            } else {
                prepareView(viewTag, stream);
                cleartool.startView(viewTag);
                cleartool.setcsTag(viewTag, SetcsOption.STREAM, null);
            }
        } else {
            cleartool.startView(viewTag);
            cleartool.setcsTag(viewTag, SetcsOption.STREAM, null);
        }

        return true;
    }

    public boolean checkoutCodeFreeze(String viewName) throws IOException, InterruptedException {
        // validate no other build is running on the same stream
        synchronized (build.getProject()) {
            ClearCaseDataAction clearcaseDataAction = null;
            Run previousBuild = build.getPreviousBuild();
            while (previousBuild != null) {
                clearcaseDataAction = previousBuild.getAction(ClearCaseDataAction.class);

                if (previousBuild.isBuilding() && clearcaseDataAction != null && clearcaseDataAction.getStream().equals(stream))
                    throw new IOException("Can't run build on stream " + stream + " when build " + previousBuild.getNumber()
                            + " is currently running on the same stream.");

                previousBuild = previousBuild.getPreviousBuild();
            }
        }

        // prepare stream and views
        prepareBuildStreamAndViews(viewName, stream);

        // make baselines
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy_HH_mm_ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = formatter.format(build.getTimestamp().getTime()).toLowerCase();

        UcmCommon.makeBaseline(cleartool.getLauncher(), true, getConfiguredStreamViewName(), null, BASELINE_NAME + dateStr, BASELINE_COMMENT + dateStr, false,
                false, null);

        // get latest baselines on the configured stream
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = UcmCommon.getLatestBlsWithCompOnStream(cleartool.getLauncher(), stream,
                getConfiguredStreamViewName());

        // fix Not labeled baselines
        for (BaselineDesc baseLineDesc : latestBlsOnConfgiuredStream) {
            if (baseLineDesc.isNotLabeled() && baseLineDesc.getComponentDesc().isModifiable()) {
                // if the base is not labeled create identical one
                List<String> readWriteCompList = new ArrayList<String>();
                readWriteCompList.add(baseLineDesc.getComponentDesc().getName());

                List<BaselineDesc> baseLineDescList = UcmCommon.makeBaseline(cleartool.getLauncher(), true, getConfiguredStreamViewName(), null, BASELINE_NAME
                        + dateStr, BASELINE_COMMENT + dateStr, true, false, readWriteCompList);

                String newBaseline = baseLineDescList.get(0).getBaselineName() + "@" + UcmCommon.getVob(baseLineDesc.getComponentDesc().getName());

                baseLineDesc.setBaselineName(newBaseline);
            }
        }

        // rebase build stream
        UcmCommon.rebase(cleartool.getLauncher(), viewName, latestBlsOnConfgiuredStream);

        // add baselines to build - to be later used by getChange
        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            dataAction.setLatestBlsOnConfiguredStream(latestBlsOnConfgiuredStream);

        return true;
    }

    private void prepareBuildStreamAndViews(String viewTag, String stream) throws IOException, InterruptedException {
        // verify that view exists on the configured stream and start it
        if (!cleartool.doesViewExist(getConfiguredStreamViewName())) {
            String dynStorageDir = cleartool.getLauncher().getLauncher().isUnix() ? unixDynStorageDir : winDynStorageDir;
            cleartool.mkview(null, getConfiguredStreamViewName(), stream, dynStorageDir);
        }
        cleartool.startView(getConfiguredStreamViewName());

        // do we have build stream? if not create it
        if (!UcmCommon.isStreamExists(cleartool.getLauncher(), getBuildStream())) {
            UcmCommon.mkstream(cleartool.getLauncher(), stream, getBuildStream());
        }

        // create view on build stream
        prepareView(viewTag, getBuildStream());

    }

    private void prepareView(String viewTag, String stream) throws IOException, InterruptedException {
        String dynStorageDir = cleartool.getLauncher().getLauncher().isUnix() ? unixDynStorageDir : winDynStorageDir;
        if (cleartool.doesViewExist(viewTag)) {
            if (recreateView) {
                cleartool.rmviewtag(viewTag);
                cleartool.mkview(null, viewTag, stream, dynStorageDir);
            }
        } else {
            cleartool.mkview(null, viewTag, stream, dynStorageDir);
        }
    }

    public static String getConfiguredStreamViewName(String jobName, String stream) {
        jobName = jobName.replace(" ", "");
        return UcmCommon.getNoVob(stream) + "_" + jobName + "_" + CONFIGURED_STREAM_VIEW_SUFFIX;
    }

    private String getConfiguredStreamViewName() {
        return getConfiguredStreamViewName(build.getProject().getName(), stream);
    }

    /**
     * @return unique build stream name
     */
    private String getBuildStream() {
        String jobName = build.getProject().getName().replace(" ", "");
        return BUILD_STREAM_PREFIX + jobName + "." + stream;
    }

}
