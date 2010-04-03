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
package hudson.plugins.clearcase.ucm;

import static hudson.plugins.clearcase.util.OutputFormat.COMMENT;
import static hudson.plugins.clearcase.util.OutputFormat.DATE_NUMERIC;
import static hudson.plugins.clearcase.util.OutputFormat.EVENT;
import static hudson.plugins.clearcase.util.OutputFormat.LINEEND;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_ELEMENTNAME;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_VERSIONID;
import static hudson.plugins.clearcase.util.OutputFormat.OPERATION;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_CONTRIBUTING;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_HEADLINE;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_STREAM;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_VERSION_ACTIVITY;
import static hudson.plugins.clearcase.util.OutputFormat.USER_ID;
import hudson.model.AbstractBuild;
import hudson.model.Run;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.UcmDynamicCheckoutAction;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.ucm.UcmCommon.BaselineDesc;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;

/**
 * @author hlyh
 */
public class UcmHistoryAction extends AbstractHistoryAction {

    private static final String[] HISTORY_FORMAT = { DATE_NUMERIC, USER_ID, NAME_ELEMENTNAME, NAME_VERSIONID, EVENT, OPERATION, UCM_VERSION_ACTIVITY };

    private static final String[] ACTIVITY_FORMAT = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, };

    private static final String[] INTEGRATION_ACTIVITY_FORMAT = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, UCM_ACTIVITY_CONTRIBUTING };

    private static final String BASELINE_NAME = "hudson_poll_";
    private static final String BASELINE_COMMENT = "hudson_poll_";

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);
    private String stream;
    private String viewDrive;
    private AbstractBuild<?, ?> build;
    private boolean freezeCode;

    public UcmHistoryAction(ClearTool cleartool, boolean useDynamicView, List<Filter> filters, String stream, String viewDrive, AbstractBuild<?, ?> build,
            boolean freezeCode) {
        super(cleartool, useDynamicView, filters);
        this.stream = stream;
        this.viewDrive = viewDrive;
        this.build = build;
        this.freezeCode = freezeCode;
    }

    @Override
    protected ClearToolFormatHandler getHistoryFormatHandler() {
        return historyHandler;
    }

    @Override
    public boolean hasChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        if (freezeCode) {
            return hasChangesCodeFreeze();
        } else {
            return super.hasChanges(time, viewName, branchNames, viewPaths);
        }
    }

    @Override
    public List<? extends Entry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        if (freezeCode) {
            List<HistoryEntry> entries = getChangesCodeFreeze();
            List<HistoryEntry> filtered = filterEntries(entries);
            List<? extends Entry> changelog = buildChangelog(viewName, filtered);
            return changelog;
        } else {
            return super.getChanges(time, viewName, branchNames, viewPaths);
        }
    }

    @Override
    protected HistoryEntry parseEventLine(Matcher matcher, String line) throws ParseException {
        // read values;
        HistoryEntry entry = new HistoryEntry();
        entry.setLine(line);

        entry.setDateText(matcher.group(1));
        entry.setUser(matcher.group(2).trim());
        entry.setElement(matcher.group(3).trim());
        entry.setVersionId(matcher.group(4).trim());
        entry.setEvent(matcher.group(5).trim());
        entry.setOperation(matcher.group(6).trim());
        entry.setActivityName(matcher.group(7).trim());
        return entry;
    }

    @Override
    protected List<? extends Entry> buildChangelog(String viewName, List<HistoryEntry> entries) throws IOException, InterruptedException {
        List<UcmActivity> result = new ArrayList<UcmActivity>();
        Map<String, UcmActivity> activityMap = new HashMap<String, UcmActivity>();

        for (HistoryEntry entry : entries) {

            UcmActivity activity = activityMap.get(entry.getActivityName());
            if (activity == null) {
                activity = new UcmActivity();
                activity.setName(entry.getActivityName());
                activity.setUser(entry.getUser());
                activityMap.put(entry.getActivityName(), activity);
                result.add(activity);
            }

            UcmActivity.File currentFile = new UcmActivity.File();
            currentFile.setComment(entry.getComment());
            currentFile.setDate(entry.getDate());
            currentFile.setDateStr(entry.getDateText());
            currentFile.setEvent(entry.getEvent());
            currentFile.setName(entry.getElement());
            currentFile.setOperation(entry.getOperation());
            currentFile.setVersion(entry.getVersionId());
            activity.addFile(currentFile);
        }

        for (UcmActivity activity : result) {
            callLsActivity(activityMap, activity, viewName, 3);
        }

        return result;
    }

    private void callLsActivity(Map<String, UcmActivity> activityMap, UcmActivity activity, String viewname, int numberOfContributingActivitiesToFollow)
            throws IOException, InterruptedException {
        ClearToolFormatHandler handler = null;
        if (activity.isIntegrationActivity()) {
            handler = new ClearToolFormatHandler(INTEGRATION_ACTIVITY_FORMAT);
        } else {
            handler = new ClearToolFormatHandler(ACTIVITY_FORMAT);
        }

        if (activity.getName() == null || activity.getName().trim().length() == 0) {
            activity.setName("Unable to get activity name");
            return;
        }

        BufferedReader reader = new BufferedReader(cleartool.lsactivity(activity.getName(), handler.getFormat(), viewname));

        String line = reader.readLine();
        Matcher matcher = handler.checkLine(line);
        if (matcher != null) {
            activity.setHeadline(matcher.group(1));
            activity.setStream(matcher.group(2));
            activity.setUser(matcher.group(3));

            if (activity.isIntegrationActivity() && numberOfContributingActivitiesToFollow > 0) {

                String contributingActivities = matcher.group(4);

                for (String contributing : contributingActivities.split(" ")) {

                    UcmActivity subActivity = null;
                    UcmActivity cachedActivity = activityMap.get(contributing);

                    if (cachedActivity == null) {
                        subActivity = new UcmActivity();
                        subActivity.setName(contributing);
                        callLsActivity(activityMap, subActivity, viewname, --numberOfContributingActivitiesToFollow);
                        activityMap.put(contributing, subActivity);
                    } else {
                        /* do deep copy */
                        subActivity = new UcmActivity(cachedActivity);
                    }
                    activity.addSubActivity(subActivity);
                }
            }
        }

        reader.close();
    }

    public boolean hasChangesCodeFreeze() throws IOException, InterruptedException {
        // make baseline on the configured stream.
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy_HH_mm_ss", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        String dateStr = formatter.format(new Date()).toLowerCase();

        UcmCommon.makeBaseline(cleartool.getLauncher(), true, UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), stream), null,
                BASELINE_NAME + dateStr, BASELINE_COMMENT + dateStr, false, false, null);

        // get latest baselines on the configured stream
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = UcmCommon.getLatestBlsWithCompOnStream(cleartool.getLauncher(), stream,
                UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), stream));

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

    public List<HistoryEntry> getChangesCodeFreeze() throws IOException, InterruptedException {
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();

        // get latest baselines on the configured stream (set as an action on the build by the checkout operation)
        ClearCaseDataAction latestBaselinesAction = build.getAction(ClearCaseDataAction.class);
        List<UcmCommon.BaselineDesc> latestBlsOnConfgiuredStream = latestBaselinesAction.getLatestBlsOnConfiguredStream();

        // find the previous build running on the same stream
        ClearCaseDataAction clearcaseDataAction = null;
        Run<?, ?> previousBuild = build.getPreviousBuild();
        while (previousBuild != null && clearcaseDataAction == null) {
            clearcaseDataAction = previousBuild.getAction(ClearCaseDataAction.class);

            if (clearcaseDataAction != null && !(clearcaseDataAction.getStream().equals(stream))) {
                clearcaseDataAction = null;
                previousBuild = previousBuild.getPreviousBuild();
            }
        }

        // get previous build baselines (set as an action on the previous build by the checkout operation)
        List<UcmCommon.BaselineDesc> previousBuildBls = null;
        if (clearcaseDataAction != null) {
            cleartool.getLauncher().getListener().getLogger().println(
                    "Checking changes by comparing this build and the last build (" + previousBuild.getNumber() + ") that ran on stream " + stream);

            previousBuildBls = clearcaseDataAction.getLatestBlsOnConfiguredStream();
        } else {
            cleartool.getLauncher().getListener().getLogger().println("Found no previous build that ran on stream " + stream);
        }

        // compare
        if (latestBlsOnConfgiuredStream != null && previousBuildBls != null) {
            // calculate changed versions
            List<String> changedVerionsList = getChangedVersions(latestBlsOnConfgiuredStream, previousBuildBls);

            // get HistoryEntry list out of changed version
            entries = translateChangedVersionsToEnteries(changedVerionsList);
        }

        return entries;
    }

    private List<HistoryEntry> translateChangedVersionsToEnteries(List<String> changedVerionsList) throws IOException, InterruptedException {
        List<HistoryEntry> entries = null;
        StringBuilder entriesDesc = new StringBuilder();

        // build output that parseLsHistory can read...
        for (String version : changedVerionsList) {
            String versionDesc = UcmCommon.getVersionDescription(cleartool.getLauncher(), version, historyHandler.getFormat() + COMMENT + LINEEND);

            entriesDesc.append(versionDesc + "\n");
        }

        // call parseLsHistory in order to create the HistoryEntry list
        BufferedReader buffReader = new BufferedReader(new StringReader(entriesDesc.toString()));
        try {
            entries = parseLsHistory(buffReader);
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
                String viewName = UcmDynamicCheckoutAction.getConfiguredStreamViewName(build.getProject().getName(), stream);

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

}
