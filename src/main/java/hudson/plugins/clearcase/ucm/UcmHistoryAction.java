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

import static hudson.plugins.clearcase.util.OutputFormat.DATE_NUMERIC;
import static hudson.plugins.clearcase.util.OutputFormat.EVENT;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_ELEMENTNAME;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_VERSIONID;
import static hudson.plugins.clearcase.util.OutputFormat.OPERATION;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_CONTRIBUTING;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_HEADLINE;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_STREAM;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_VERSION_ACTIVITY;
import static hudson.plugins.clearcase.util.OutputFormat.USER_ID;
import hudson.plugins.clearcase.AbstractClearCaseScm.ChangeSetLevel;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.plugins.clearcase.util.OutputFormat;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author hlyh
 */
public class UcmHistoryAction extends AbstractHistoryAction {

    private static final String[] HISTORY_FORMAT = { DATE_NUMERIC, USER_ID, NAME_ELEMENTNAME, NAME_VERSIONID, EVENT, OPERATION, UCM_VERSION_ACTIVITY };

    private static final String[] ACTIVITY_FORMAT = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, };

    private static final String[] INTEGRATION_ACTIVITY_FORMAT = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, UCM_ACTIVITY_CONTRIBUTING };

    private final ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);

    private final ClearCaseUCMSCMRevisionState oldBaseline;
    private final ClearCaseUCMSCMRevisionState newBaseline;

    public UcmHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, ClearCaseUCMSCMRevisionState oldBaseline,
            ClearCaseUCMSCMRevisionState newBaseline, ChangeSetLevel changeset) {
        super(cleartool, useDynamicView, filter, changeset, false);
        this.oldBaseline = oldBaseline;
        this.newBaseline = newBaseline;
    }

    @Override
    protected List<Entry> buildChangelog(String viewPath, List<HistoryEntry> entries) throws IOException, InterruptedException {
        List<Entry> result = new ArrayList<Entry>();
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

        for (Entry activity : result) {
            callLsActivity(activityMap, (UcmActivity) activity, viewPath, 3);
        }

        return result;
    }

    private void callLsActivity(Map<String, UcmActivity> activityMap, UcmActivity activity, String viewPath, int numberOfContributingActivitiesToFollow)
            throws IOException, InterruptedException {
        ClearToolFormatHandler handler = new ClearToolFormatHandler(activity.isIntegrationActivity() ? INTEGRATION_ACTIVITY_FORMAT : ACTIVITY_FORMAT);
        if (StringUtils.isBlank(activity.getName())) {
            activity.setName("Unable to get activity name");
            return;
        }

        BufferedReader reader = new BufferedReader(cleartool.lsactivity(activity.getName(), handler.getFormat(), viewPath));

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
                        callLsActivity(activityMap, subActivity, viewPath, --numberOfContributingActivitiesToFollow);
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

    @Override
    protected List<HistoryEntry> runLsHistory(Date sinceTime, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
            InterruptedException {
        List<HistoryEntry> history = super.runLsHistory(sinceTime, viewPath, viewTag, branchNames, viewPaths);
        if (needsHistoryOnAllBranches()) {
            if (oldBaseline == null) {
                return history;
            }
            List<Baseline> oldBaselines = oldBaseline.getBaselines();
            List<Baseline> newBaselines = newBaseline.getBaselines();
            if (ObjectUtils.equals(oldBaselines, newBaselines)) {
                return history;
            }
            for (final Baseline oldBl : oldBaselines) {
                String bl1 = oldBl.getBaselineName();
                final String comp1 = oldBl.getComponentName();
                // Lookup the component in the set of new baselines
                Baseline newBl = (Baseline) CollectionUtils.find(newBaselines, new Predicate() {
                    @Override
                    public boolean evaluate(Object bl) {
                        return StringUtils.equals(comp1, ((Baseline) bl).getComponentName());
                    }
                });
                // If we cannot find a new baseline, log and skip
                if (newBl == null) {
                    cleartool.getLauncher().getListener().getLogger().print("Old Baseline " + bl1 + " for component " + comp1 + " couldn't be found in the new set of baselines.");
                    continue;
                }
                String bl2 = newBl.getBaselineName();
                if (!StringUtils.equals(bl1, bl2)) {
                    List<String> versions = UcmCommon.getDiffBlVersions(cleartool, viewPath, "baseline:" + bl1, "baseline:" + bl2);
                    for (String version : versions) {
                        try {
                            parseLsHistory(new BufferedReader(cleartool.describe(getHistoryFormatHandler().getFormat() + OutputFormat.COMMENT + OutputFormat.LINEEND, null, version)), history);
                        } catch (ParseException e) {
                            /* empty by design */
                        }
                    }
                }
            }
        }
        return history;
    }

    private boolean needsHistoryOnAllBranches() {
        return ChangeSetLevel.ALL.equals(getChangeset());
    }

    @Override
    protected ClearToolFormatHandler getHistoryFormatHandler() {
        return historyHandler;
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

}
