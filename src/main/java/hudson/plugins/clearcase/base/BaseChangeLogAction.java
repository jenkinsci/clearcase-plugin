package hudson.plugins.clearcase.base;

import static hudson.plugins.clearcase.util.OutputFormat.*;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolHistoryParser;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.plugins.clearcase.util.EventRecordFilter;

/**
 * Change log action for Base ClearCase
 * @todo move code from ClearToolHistoryParser into this
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

    private final int maxTimeDifferenceMillis;

    public BaseChangeLogAction(ClearTool cleartool, int maxTimeDifferenceMillis) {
        this.cleartool = cleartool;
        this.maxTimeDifferenceMillis = maxTimeDifferenceMillis;
    }
    
    public List<ClearCaseChangeLogEntry> getChanges(EventRecordFilter eventFilter, Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<ClearCaseChangeLogEntry> fullList = new ArrayList<ClearCaseChangeLogEntry>();
        try {
            for (String branchName : branchNames) {

                Reader reader = cleartool.lshistory(historyHandler.getFormat() + COMMENT + LINEEND, time, viewName, branchName, viewPaths);
                ClearToolHistoryParser parser = new ClearToolHistoryParser(eventFilter);
                List<ClearCaseChangeLogEntry> data = parser.parse(reader);
                fullList.addAll(data);
                reader.close();
            }
        } catch (ParseException ex) {
            ;
        }
        ChangeLogEntryMerger entryMerger = new ChangeLogEntryMerger(maxTimeDifferenceMillis);
        return entryMerger.getMergedList(fullList);
    }
}
