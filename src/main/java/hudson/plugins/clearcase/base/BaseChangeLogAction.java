package hudson.plugins.clearcase.base;

import static hudson.plugins.clearcase.util.OutputFormat.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;

/**
 * Change log action for Base ClearCase
 */
public class BaseChangeLogAction implements ChangeLogAction {

    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        USER_ID,
        EVENT,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        OPERATION
    };

    private ClearTool cleartool;    
    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");

    private final int maxTimeDifferenceMillis;
    private List<Filter> filters;
    /**
     * Extended view path that should be removed file paths in entries.
     */
    private String extendedViewPath;

    public BaseChangeLogAction(ClearTool cleartool, int maxTimeDifferenceMillis,List<Filter> filters) {
        this.cleartool = cleartool;
        this.maxTimeDifferenceMillis = maxTimeDifferenceMillis;
        this.filters = filters;
        if (this.filters == null) {
            this.filters = new ArrayList<Filter>();
        }
    }
    
    @Override
    public List<ClearCaseChangeLogEntry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<ClearCaseChangeLogEntry> fullList = new ArrayList<ClearCaseChangeLogEntry>();
        try {
            for (String branchName : branchNames) {
                BufferedReader reader = new BufferedReader(cleartool.lshistory(historyHandler.getFormat() + COMMENT + LINEEND, time, viewName, branchName, viewPaths));
                fullList.addAll(parseEntries(reader));
                reader.close();
            }
        } catch (ParseException ex) {
            ;
        }
        ChangeLogEntryMerger entryMerger = new ChangeLogEntryMerger(maxTimeDifferenceMillis);
        return entryMerger.getMergedList(fullList);
    }

    private List<ClearCaseChangeLogEntry> parseEntries(BufferedReader reader) 
        throws IOException, InterruptedException, ParseException {
        
        List<ClearCaseChangeLogEntry> entries = new ArrayList<ClearCaseChangeLogEntry>();        
        
        StringBuilder commentBuilder = new StringBuilder();
        String line = reader.readLine();

        ClearCaseChangeLogEntry currentEntry = null;
        outer:
        while (line != null) {
            //TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                line = reader.readLine();
                continue;
            }
            Matcher matcher = historyHandler.checkLine(line);

            // finder find start of lshistory entry
            if (matcher != null) {

                if (currentEntry != null) {
                    currentEntry.setComment(commentBuilder.toString());
                }
                commentBuilder = new StringBuilder();
                currentEntry = new ClearCaseChangeLogEntry();
                
                // read values;
                Date date = dateFormatter.parse(matcher.group(1));
                currentEntry.setDate(date);
                currentEntry.setUser(matcher.group(2));
                String fileName = matcher.group(4).trim();
                if (extendedViewPath != null) {
                    if (fileName.toLowerCase().startsWith(extendedViewPath)) {
                        fileName = fileName.substring(extendedViewPath.length());
                    }
                }
                ClearCaseChangeLogEntry.FileElement element = new ClearCaseChangeLogEntry.FileElement(
                        fileName, matcher.group(5).trim(), matcher.group(3).trim(), matcher.group(6).trim());
                currentEntry.addElement(element);
                
                HistoryEntry entry = new HistoryEntry();
                entry.setLine(line);
                entry.setDateText(matcher.group(1).trim());
                entry.setUser(matcher.group(2).trim());
                entry.setEvent(matcher.group(3).trim());
                entry.setElement(matcher.group(4).trim());
                entry.setVersionId(matcher.group(5).trim());
                entry.setOperation(matcher.group(6).trim());

                for (Filter filter : filters) {
                    if (!filter.accept(entry))  {
                        line = reader.readLine();
                        continue outer;
                    }
                }
                
                entries.add(currentEntry);
                
            } else {
                if (commentBuilder.length() > 0) {
                    commentBuilder.append("\n");
                }
                commentBuilder.append(line);
            }
            line = reader.readLine();
        }
        if (currentEntry != null) {
            currentEntry.setComment(commentBuilder.toString());
        }
        
        return entries;
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
