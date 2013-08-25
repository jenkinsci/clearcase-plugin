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
import hudson.model.TaskListener;
import hudson.plugins.clearcase.AbstractClearCaseScm.ChangeSetLevel;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.ucm.model.ActivitiesDelta;
import hudson.plugins.clearcase.ucm.model.Activity;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.ucm.service.BaselineService;
import hudson.plugins.clearcase.ucm.service.FacadeService;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;

/**
 * @author hlyh
 */
public class UcmHistoryAction extends AbstractHistoryAction {

    static final Logger                  LOG                               = Logger.getLogger(UcmHistoryAction.class.getName());

    private static final String[]        ACTIVITY_FORMAT                   = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, };

    private static final String[]        HISTORY_FORMAT                    = { DATE_NUMERIC, USER_ID, NAME_ELEMENTNAME, NAME_VERSIONID, EVENT, OPERATION,
        UCM_VERSION_ACTIVITY                                          };

    private static final String[]        INTEGRATION_ACTIVITY_FORMAT       = { UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID, UCM_ACTIVITY_CONTRIBUTING };

    private static final int             MAX_DEPTH_CONTRIBUTING_ACTIVITIES = 3;

    private EntryListAdapter             entryListAdapter                  = new EntryListAdapter();

    private FacadeService                facadeService;

    private final ClearToolFormatHandler historyHandler                    = new ClearToolFormatHandler(HISTORY_FORMAT);

    private final UcmRevisionState       newBaseline;

    private final UcmRevisionState       oldBaseline;

    public UcmHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, UcmRevisionState oldBaseline, UcmRevisionState newBaseline,
            ChangeSetLevel changeset, FacadeService facadeService) {
        super(cleartool, useDynamicView, filter, changeset, false);
        this.oldBaseline = oldBaseline;
        this.newBaseline = newBaseline;
        this.facadeService = facadeService;
    }

    @Override
    public List<Entry> getChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
    InterruptedException {
        List<Entry> result = new ArrayList<Entry>();
        result.addAll(getChangesOnBaseline(time, viewPath, viewTag, branchNames, viewPaths));
        result.addAll(super.getChanges(time, viewPath, viewTag, branchNames, viewPaths));
        return result;
    }

    public FacadeService getFacadeService() {
        return facadeService;
    }

    @Override
    public boolean hasChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        return hasChangesOnBaseline(time, viewPath, viewTag, branchNames, viewPaths) || super.hasChanges(time, viewPath, viewTag, branchNames, viewPaths);
    }

    public void setFacadeService(FacadeService facadeService) {
        this.facadeService = facadeService;
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
            if (entry.getElement() != null) {
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
        }

        for (Entry activity : result) {
            callLsActivity(activityMap, (UcmActivity) activity, viewPath, MAX_DEPTH_CONTRIBUTING_ACTIVITIES);
        }

        return result;
    }

    protected List<HistoryEntry> compareBaselines(String viewPath) throws IOException, InterruptedException {
        Map<Component, Baseline> from = toMap(getOldBaseline().getBaselines());
        Map<Component, Baseline> to = toMap(getNewBaseline().getBaselines());
        List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
        for (java.util.Map.Entry<Component, Baseline> entry : from.entrySet()) {
            Baseline oldBl = entry.getValue();
            Baseline newBl;
            Component component = entry.getKey();
            if ((newBl = to.get(component)) == null) {
                LOG.warning(MessageFormat.format("Skipping {0} since there is no new baseline for component {1}.", oldBl, component));
                continue;
            }
            historyEntries.addAll(entryListAdapter.adapt(facadeService.getBaselineService().compare(oldBl, newBl)));
        }
        return historyEntries;
    }

    protected List<Entry> getChangesOnBaseline(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
    InterruptedException {
        if (getOldBaseline() == null)
            return Collections.emptyList();
        if (getNewBaseline() == null)
            return Collections.emptyList();
        Map<Component, Baseline> from = toMap(getOldBaseline().getBaselines());
        Map<Component, Baseline> to = toMap(getNewBaseline().getBaselines());
        List<Entry> entries = new ArrayList<Entry>();
        for (java.util.Map.Entry<Component, Baseline> entry : from.entrySet()) {
            Baseline oldBl = entry.getValue();
            Baseline newBl;
            Component component = entry.getKey();
            if ((newBl = to.get(component)) == null) {
                LOG.warning(MessageFormat.format("Skipping {0} since there is no new baseline for component {1}.", oldBl, component));
                continue;
            }
            if (oldBl.equals(newBl)) {
                continue;
            }
            UcmActivity rebaseActivity = new UcmActivity();
            String componentName = component == null ? "??" : component.getName();
            rebaseActivity.setName("rebase_" + componentName);
            rebaseActivity.setHeadline(MessageFormat.format("Rebase on component {0} : {1} → {2}", componentName, oldBl.getName(), newBl.getName()));
            ActivitiesDelta blComparison = getFacadeService().getBaselineService().compare(oldBl, newBl);
            fillSubActivities(rebaseActivity, UcmActivity.MODIFIER_ADD, blComparison.getRight());
            fillSubActivities(rebaseActivity, UcmActivity.MODIFIER_DELETE, blComparison.getLeft());
            entries.add(rebaseActivity);
        }
        return entries;
    }

    @Override
    protected ClearToolFormatHandler getHistoryFormatHandler() {
        return historyHandler;
    }

    protected UcmRevisionState getNewBaseline() {
        return newBaseline;
    }

    protected UcmRevisionState getOldBaseline() {
        return oldBaseline;
    }

    protected boolean hasChangesOnBaseline(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) {
        LOG.finest("Called hasChangesOnBaseline");
        if (getOldBaseline() == null) {
            LOG.finest("oldBaseline == null");
            return false;
        }
        if (getNewBaseline() == null) {
            LOG.finest("newBaseline == null");
            return false;
        }
        Baseline[] oldBaselines = getOldBaseline().getBaselines();
        Baseline[] newBaselines = getNewBaseline().getBaselines();
        TaskListener listener = cleartool.getLauncher().getListener();
        if (Arrays.equals(oldBaselines, newBaselines)) {
            String message = "Baselines are identical : " + StringUtils.join(oldBaselines, ", ");
            LOG.fine(message);
            listener.getLogger().println(message);
            return false;
        }
        String oldBaselinesMessage = "Old baselines : " + StringUtils.join(oldBaselines, ", ");
        LOG.fine(oldBaselinesMessage);
        listener.getLogger().println(oldBaselinesMessage);
        String newBaselinesMessage = "New baselines : " + StringUtils.join(newBaselines, ", ");
        LOG.fine(newBaselinesMessage);
        listener.getLogger().println(newBaselinesMessage);
        return true;
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
    protected List<HistoryEntry> runLsHistory(Date sinceTime, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
    InterruptedException {
        List<HistoryEntry> history = super.runLsHistory(sinceTime, viewPath, viewTag, branchNames, viewPaths);
        if (needsHistoryOnAllBranches()) {
            if (oldBaseline == null) {
                return history;
            }
            history.addAll(compareBaselines(viewPath));
        }
        return history;
    }

    protected Map<Component, Baseline> toMap(Baseline[] baselines) throws IOException, InterruptedException {
        Map<Component, Baseline> result = new HashMap<Component, Baseline>();
        for (Baseline baseline : baselines) {
            result = addBaselineToResult(baseline, result);
        }
        return result;
    }

    private Map<Component, Baseline> addBaselineToResult(Baseline baseline, Map<Component, Baseline> result) throws IOException, InterruptedException {
        Baseline oldValue;
        BaselineService baselineService = getFacadeService().getBaselineService();
        Component component = baselineService.getComponent(baseline);
        if ((oldValue = result.put(component, baseline)) != null) {
            LOG.warning(MessageFormat.format("Skipping {0} for {1}. Replaced by {2}.", oldValue, component, baseline));
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

    private void fillSubActivities(UcmActivity rootActivity, String modifier, Collection<Activity> subActivities) {
        for (Activity activity : subActivities) {
            UcmActivity contributingActivity = new UcmActivity();
            contributingActivity.setName(activity.getSelector());
            contributingActivity.setHeadline(activity.getHeadline());
            contributingActivity.setModifier(modifier);
            rootActivity.addSubActivity(contributingActivity);
        }
    }

    private boolean needsHistoryOnAllBranches() {
        return ChangeSetLevel.ALL.equals(getChangeset());
    }

}
