package hudson.plugins.clearcase.ucm;

import static hudson.plugins.clearcase.util.OutputFormat.*;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;

import hudson.scm.ChangeLogSet.Entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
 

/**
 *
 * @author hlyh
 */
public class UcmHistoryAction extends AbstractHistoryAction {

    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        USER_ID,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        EVENT,
        OPERATION,
        UCM_VERSION_ACTIVITY
    };

    private static final String[] ACTIVITY_FORMAT = {UCM_ACTIVITY_HEADLINE,
        UCM_ACTIVITY_STREAM,
        USER_ID,
    };

    private static final String[] INTEGRATION_ACTIVITY_FORMAT = {UCM_ACTIVITY_HEADLINE,
        UCM_ACTIVITY_STREAM,
        USER_ID,
        UCM_ACTIVITY_CONTRIBUTING
    };

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);

    public UcmHistoryAction(ClearTool cleartool, List<Filter> filters) {
        super(cleartool, filters);
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

    @Override
    protected List<? extends Entry> buildChangelog(String viewName,List<HistoryEntry> entries) throws IOException, InterruptedException {
        List<UcmActivity> result = new ArrayList<UcmActivity>();
        Map<String,UcmActivity> activityMap = new HashMap<String, UcmActivity>();

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
            callLsActivity(activityMap,activity, viewName, 3);
        }

        return result;
    }

    private void callLsActivity(Map<String,UcmActivity> activityMap,UcmActivity activity,String viewname, int numberOfContributingActivitiesToFollow) throws IOException, InterruptedException {
        ClearToolFormatHandler handler = null;
        if (activity.isIntegrationActivity()) {
            handler = new ClearToolFormatHandler(INTEGRATION_ACTIVITY_FORMAT);
        } else {
            handler = new ClearToolFormatHandler(ACTIVITY_FORMAT);
        }

        BufferedReader reader = new BufferedReader(cleartool.lsactivity(activity.getName(), handler.getFormat(),viewname));

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

                    if (cachedActivity ==null) {
                        subActivity = new UcmActivity();
                        subActivity.setName(contributing);
                        callLsActivity(activityMap,subActivity,viewname,--numberOfContributingActivitiesToFollow);
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


}
