package hudson.plugins.clearcase.ucm;

import static hudson.plugins.clearcase.util.OutputFormat.*;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.plugins.clearcase.util.EventRecordFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 *
 * @author Henrik L. Hansen
 */
public class UcmChangeLogAction implements ChangeLogAction {

    // full lshistory output and parsing 
    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        UCM_VERSION_ACTIVITY,
        EVENT, OPERATION
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
    
    private ClearTool cleartool;

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
    private Map<String, UcmActivity> activityNameToEntry = new HashMap<String, UcmActivity>();

    /**
     * Extended view path that should be removed file paths in entries.
     */
    private String extendedViewPath;
    
    public UcmChangeLogAction(ClearTool cleartool) {
        this.cleartool = cleartool;
    }

    public List<UcmActivity> getChanges(EventRecordFilter eventFilter, Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        BufferedReader reader = new BufferedReader(cleartool.lshistory(historyHandler.getFormat() + COMMENT + LINEEND, time, viewName, branchNames[0], viewPaths)); 
        List<UcmActivity> history = parseHistory(reader,eventFilter,viewName);
        reader.close();
        return history;
    }

    private List<UcmActivity> parseHistory(BufferedReader reader, EventRecordFilter eventRecordFilter,String viewname) throws InterruptedException,IOException {
        List<UcmActivity> result = new ArrayList<UcmActivity>();
        try {
            StringBuilder commentBuilder = new StringBuilder();
            String line = reader.readLine();

            UcmActivity.File currentFile = null;
            while (line != null) {

                //TODO: better error handling
                if (line.startsWith("cleartool: Error:")) {
                    line = reader.readLine();
                    continue;
                }
                Matcher matcher = historyHandler.checkLine(line);

                // finder find start of lshistory entry
                if (matcher != null) {

                    if (currentFile != null) {
                        currentFile.setComment(commentBuilder.toString());
                    }
                    commentBuilder = new StringBuilder();
                    currentFile = new UcmActivity.File();

                    // read values;
                    currentFile.setDate(dateFormatter.parse(matcher.group(1)));
                    
                    String fileName = matcher.group(2).trim();
                    if (extendedViewPath != null) {
                        if (fileName.toLowerCase().startsWith(extendedViewPath)) {
                            fileName = fileName.substring(extendedViewPath.length());
                        }
                    }
                    
                    currentFile.setName(fileName);
                    currentFile.setVersion(matcher.group(3));
                    currentFile.setEvent(matcher.group(5));
                    currentFile.setOperation(matcher.group(6));

                    if (! eventRecordFilter.accept(currentFile.getEvent(), currentFile.getVersion())) {
                        line = reader.readLine();
                        continue;
                    }

                    String activityName = matcher.group(4);

                    UcmActivity activity = activityNameToEntry.get(activityName);
                    if (activity == null) {
                        activity = new UcmActivity();
                        activity.setName(activityName);                        
                        if (activityName.length()!=0) {
                            callLsActivity(activity,viewname, 1);
                        } else {
                            activity.setHeadline("Unknown activity");
                            activity.setUser("Unknown");
                            activity.setStream("");
                        }
                        activityNameToEntry.put(activityName, activity);
                        result.add(activity);                            
                    }

                    activity.addFile(currentFile);
                } else {
                    if (commentBuilder.length() > 0) {
                        commentBuilder.append("\n");
                    }
                    commentBuilder.append(line);
                }
                line = reader.readLine();
            }
            if (currentFile != null) {
                currentFile.setComment(commentBuilder.toString());
            }
        } catch (ParseException ex) {
        	IOException ioe = new IOException("Could not parse cleartool output");
        	ioe.setStackTrace(ex.getStackTrace());
            throw ioe;
        }
        return result;
    }

    private void callLsActivity(UcmActivity activity,String viewname, int numberOfContributingActivitiesToFollow) throws IOException, InterruptedException {
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
                    UcmActivity cachedActivity = activityNameToEntry.get(contributing);
                    
                    if (cachedActivity ==null) {
                        subActivity = new UcmActivity();                        
                        subActivity.setName(contributing);
                        callLsActivity(subActivity,viewname,--numberOfContributingActivitiesToFollow);
                        activityNameToEntry.put(contributing, subActivity);
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
    
    /**
     * Sets the extended view path.
     * The extended view path will be removed from file paths in the event.
     * The extended view path is for example the view root + view name; and this
     * path shows up in the history and can be conusing for users.
     * @param path the new extended view path.
     */
    public void setExtendedViewPath(String path) {
        if (path != null) {
            this.extendedViewPath = path.toLowerCase();
        } else {
            this.extendedViewPath = null;
        }
    }

    public String getExtendedViewPath() {
        return extendedViewPath;
    }
}
