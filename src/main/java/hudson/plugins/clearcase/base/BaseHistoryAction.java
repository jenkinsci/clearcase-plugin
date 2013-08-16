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
package hudson.plugins.clearcase.base;

import static hudson.plugins.clearcase.util.OutputFormat.DATE_NUMERIC;
import static hudson.plugins.clearcase.util.OutputFormat.EVENT;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_ELEMENTNAME;
import static hudson.plugins.clearcase.util.OutputFormat.NAME_VERSIONID;
import static hudson.plugins.clearcase.util.OutputFormat.OPERATION;
import static hudson.plugins.clearcase.util.OutputFormat.USER_ID;
import hudson.FilePath;
import hudson.plugins.clearcase.AbstractClearCaseScm.ChangeSetLevel;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.UpdtEntry;
import hudson.plugins.clearcase.history.AbstractHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.Validate;

/**
 * @author hlyh
 */
public class BaseHistoryAction extends AbstractHistoryAction {

    private static final String[]  HISTORY_FORMAT = { DATE_NUMERIC, USER_ID, NAME_ELEMENTNAME, NAME_VERSIONID, EVENT, OPERATION };

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);
    private int                    maxTimeDifferenceMillis;

    public BaseHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, ChangeSetLevel changeset, boolean useRecurse,
            int maxTimeDifferenceMillis) {
        super(cleartool, useDynamicView, filter, changeset, useRecurse);
        this.maxTimeDifferenceMillis = maxTimeDifferenceMillis;
    }

    public BaseHistoryAction(ClearTool cleartool, boolean useDynamicView, Filter filter, int maxTimeDifferenceMillis) {
        this(cleartool, useDynamicView, filter, null, false, maxTimeDifferenceMillis);
    }

    @Override
    protected List<? extends Entry> buildChangelog(String viewPath, List<HistoryEntry> entries) {
        List<ClearCaseChangeLogEntry> fullList = new ArrayList<ClearCaseChangeLogEntry>();

        for (HistoryEntry entry : entries) {
            ClearCaseChangeLogEntry changelogEntry = new ClearCaseChangeLogEntry();

            changelogEntry.setDate(entry.getDate());
            changelogEntry.setUser(entry.getUser());
            changelogEntry.setComment(entry.getComment());

            ClearCaseChangeLogEntry.FileElement fileElement = new ClearCaseChangeLogEntry.FileElement(entry.getElement(), entry.getVersionId(),
                    entry.getEvent(), entry.getOperation());

            changelogEntry.addElement(fileElement);
            fullList.add(changelogEntry);
        }
        ChangeLogEntryMerger entryMerger = new ChangeLogEntryMerger(maxTimeDifferenceMillis);
        return entryMerger.getMergedList(fullList);
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
        return entry;
    }

    protected List<HistoryEntry> parseUpdt(FilePath updtFile, String viewPath) throws IOException, InterruptedException {
        Validate.notNull(updtFile);
        List<HistoryEntry> history = new ArrayList<HistoryEntry>();
        List<UpdtEntry> updtEntries = new ArrayList<UpdtEntry>();
        InputStream is = updtFile.read();
        try {
            LineIterator it = org.apache.commons.io.IOUtils.lineIterator(is, "UTF-8");
            while (it.hasNext()) {
                String line = it.nextLine();
                UpdtEntry entry = UpdtEntry.getEntryFromLine(line);
                if (entry.getState() == UpdtEntry.State.NEW || entry.getState() == UpdtEntry.State.UPDATED) {
                    updtEntries.add(entry);
                }
            }
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(is);
        }
        for (UpdtEntry entry : updtEntries) {
            try {
                BufferedReader reader = new BufferedReader(cleartool.describe(getLsHistoryFormat(), viewPath, entry.getObjectSelectorNewVersion()));
                try {
                    parseLsHistory(reader, history);
                } catch (ParseException e) {
                    // no op
                }
                reader.close();
            } catch (IOException e) {
                // skip describe errors
            }
        }
        return history;
    }

    @Override
    protected List<HistoryEntry> runLsHistory(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException,
    InterruptedException {
        List<HistoryEntry> entries = null;
        if (ChangeSetLevel.UPDT.equals(getChangeset()) && getUpdtFile() != null) {
            entries = parseUpdt(getUpdtFile(), viewPath);
        } else {
            entries = super.runLsHistory(time, viewPath, viewTag, branchNames, viewPaths);
        }
        return entries;
    }
}
