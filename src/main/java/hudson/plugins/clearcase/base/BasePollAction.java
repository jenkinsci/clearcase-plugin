/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.clearcase.base;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.DefaultPollAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.plugins.clearcase.util.EventRecordFilter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import static hudson.plugins.clearcase.util.OutputFormat.*;

/**
 *
 * @author hlyh
 */
public class BasePollAction extends DefaultPollAction {

    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        EVENT,
        OPERATION
    };

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);

    public BasePollAction(ClearTool cleartool, List<Filter> filters) {
        super(cleartool, filters);
    }


    @Override
    protected ClearToolFormatHandler getHistoryFormatHandler() {
        return historyHandler;

    }
    @Override
    protected HistoryEntry parseLine(String line) throws ParseException {
        if (line.startsWith("cleartool: Error:")) {
            return null;
        }

        Matcher matcher = historyHandler.checkLine(line);
        if (matcher == null) {
            return null;
        }
        // read values;
        HistoryEntry entry = new HistoryEntry();
        entry.setLine(line);
        entry.setDateText(matcher.group(1));
        entry.setElement(matcher.group(2));
        entry.setVersionId(matcher.group(3));
        entry.setEvent(matcher.group(4));
        entry.setOperation(matcher.group(5));
        return entry;
    }

}
