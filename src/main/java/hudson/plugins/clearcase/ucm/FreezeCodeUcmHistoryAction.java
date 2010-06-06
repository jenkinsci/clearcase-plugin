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
package hudson.plugins.clearcase.ucm;

import static hudson.plugins.clearcase.util.OutputFormat.COMMENT;
import static hudson.plugins.clearcase.util.OutputFormat.LINEEND;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.ucm.UcmCommon.BaselineDesc;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FreezeCodeUcmHistoryAction extends UcmHistoryAction {
    private static final String BASELINE_NAME = "hudson_poll_";
    private static final String BASELINE_COMMENT = "hudson_poll_";

    private final AbstractBuild<?, ?> build;
    private final String viewDrive;
    private final String stream;
    public FreezeCodeUcmHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, String stream, String viewDrive,
            AbstractBuild<?, ?> build, ClearCaseUCMSCMRevisionState oldBaseline, ClearCaseUCMSCMRevisionState newBaseline) {
        super(cleartool, useDynamicView, filter, oldBaseline, newBaseline);
        this.build = build;
        this.stream = stream;
        this.viewDrive = viewDrive;
        
    }
    
    @Override
    public List<? extends Entry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();

        // get latest baselines on the configured stream (set as an action on the build by the checkout operation)
        ClearCaseDataAction latestBaselinesAction = build.getAction(ClearCaseDataAction.class);
        List<UcmCommon.BaselineDesc> latestBlsOnConfiguredStream = latestBaselinesAction.getLatestBlsOnConfiguredStream();

        // find the previous build running on the same stream
        ClearCaseDataAction clearcaseDataAction = null;
        Run<?, ?> previousBuild = build.getPreviousBuild();
        while (previousBuild != null && clearcaseDataAction == null) {
            clearcaseDataAction = previousBuild.getAction(ClearCaseDataAction.class);

            if (clearcaseDataAction != null && !(clearcaseDataAction.getStream().equals(getStream()))) {
                clearcaseDataAction = null;
                previousBuild = previousBuild.getPreviousBuild();
            }
        }

        // get previous build baselines (set as an action on the previous build by the checkout operation)
        List<UcmCommon.BaselineDesc> previousBuildBls = null;
        if (clearcaseDataAction != null) {
            cleartool.getLauncher().getListener().getLogger().println(
                    "Checking changes by comparing this build and the last build (" + previousBuild.getNumber() + ") that ran on stream " + getStream());

            previousBuildBls = clearcaseDataAction.getLatestBlsOnConfiguredStream();
        } else {
            cleartool.getLauncher().getListener().getLogger().println("Found no previous build that ran on stream " + getStream());
        }

        // compare
        if (latestBlsOnConfiguredStream != null && previousBuildBls != null) {
            // calculate changed versions
            List<String> changedVerionsList = getChangedVersions(latestBlsOnConfiguredStream, previousBuildBls);

            // get HistoryEntry list out of changed version
            entries = translateChangedVersionsToEnteries(changedVerionsList);
        }
        List<HistoryEntry> filtered = filterEntries(entries);
        List<? extends Entry> changelog = buildChangelog(viewName, filtered);
        return changelog;
    }

    @Override
    public boolean hasChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        // make baseline on the configured stream.
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy_HH_mm_ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = formatter.format(new Date()).toLowerCase();

        UcmCommon.makeBaseline(cleartool.getLauncher(), true, UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), getStream()), null,
                BASELINE_NAME + dateStr, BASELINE_COMMENT + dateStr, false, false, null);

        // get latest baselines on the configured stream
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = UcmCommon.getLatestBlsWithCompOnStream(cleartool.getLauncher(), getStream(),
                UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), getStream()));

        // find the previous build running on the same stream
        Run<?, ?> previousBuild = build.getPreviousBuild();
        ClearCaseDataAction clearcaseDataAction = previousBuild.getAction(ClearCaseDataAction.class);

        // get previous build baselines (set as an action on the previous build by the checkout operation)
        List<UcmCommon.BaselineDesc> previousBuildBls = null;
        if (clearcaseDataAction != null)
            previousBuildBls = clearcaseDataAction.getLatestBlsOnConfiguredStream();

        // check if any baselines added/removed/changed
        if (latestBlsOnConfgiuredStream != null && previousBuildBls != null) {
            if (latestBlsOnConfgiuredStream.size() != previousBuildBls.size())
                return true;

            for (UcmCommon.BaselineDesc blCurr : latestBlsOnConfgiuredStream) {
                boolean foundBl = false;

                for (UcmCommon.BaselineDesc blPrev : previousBuildBls) {
                    if (blCurr.getBaselineName().equals(blPrev.getBaselineName())) {
                        foundBl = true;
                        break;
                    }
                }

                if (!foundBl)
                    return true;
            }
        }

        return false;
    }

    private List<HistoryEntry> translateChangedVersionsToEnteries(List<String> changedVerionsList) throws IOException, InterruptedException {
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        StringBuilder entriesDesc = new StringBuilder();

        // build output that parseLsHistory can read...
        for (String version : changedVerionsList) {
            String versionDesc = UcmCommon.getVersionDescription(cleartool.getLauncher(), version, getHistoryFormatHandler().getFormat() + COMMENT + LINEEND);

            entriesDesc.append(versionDesc + "\n");
        }

        // call parseLsHistory in order to create the HistoryEntry list
        BufferedReader buffReader = new BufferedReader(new StringReader(entriesDesc.toString()));
        try {
            parseLsHistory(buffReader, entries);
        } catch (ParseException e) {
            throw new IOException(e.getMessage());
        }

        return entries;
    }

    private List<String> getChangedVersions(List<UcmCommon.BaselineDesc> newBls, List<UcmCommon.BaselineDesc> oldBls) throws IOException, InterruptedException {
        List<String> changedVersionList = new ArrayList<String>();

        // compare baselines
        for (UcmCommon.BaselineDesc blDesc : newBls) {
            // ignore read-only components
            if (!blDesc.getComponentDesc().isModifiable())
                continue;

            String previousBl = getBaseLineNameForComponent(oldBls, blDesc.getComponentName());

            // check if baselines changed
            if (previousBl != null && !previousBl.equals(blDesc.getBaselineName())) {
                String viewName = UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), getStream());

                // run diffbl
                List<String> changedVersionListPerBl = UcmCommon.getDiffBlVersions(cleartool.getLauncher(), viewDrive + "/" + viewName, previousBl, blDesc
                        .getBaselineName());

                changedVersionList.addAll(changedVersionListPerBl);
            }
        }

        return changedVersionList;
    }

    private String getBaseLineNameForComponent(List<UcmCommon.BaselineDesc> baselineList, String compName) {
        for (BaselineDesc blDesc : baselineList) {
            if (UcmCommon.getNoVob(blDesc.getComponentName()).equals(UcmCommon.getNoVob(compName)))
                return blDesc.getBaselineName();
        }

        return null;
    }

    public String getStream() {
        return stream;
    }

}
