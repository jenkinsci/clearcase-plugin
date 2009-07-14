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
package hudson.plugins.clearcase.history;

import static hudson.plugins.clearcase.util.OutputFormat.*;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.scm.ChangeLogSet.Entry;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;


/**
 *
 * @author hlyh
 */
public abstract class AbstractHistoryAction implements HistoryAction {

    protected ClearTool cleartool;
    protected List<Filter> filters;
    protected String extendedViewPath;

    public AbstractHistoryAction(ClearTool cleartool, List<Filter> filters) {
        this.cleartool = cleartool;
        this.filters = filters!=null ? filters : new ArrayList<Filter>();

    }
    
    @Override
    public boolean hasChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<HistoryEntry> entries = runLsHistory(time,viewName,branchNames,viewPaths); 
        List<HistoryEntry> filtered = filterEntries(entries);

        return filtered.size() > 0;

    }

    @Override
    public List<? extends Entry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<HistoryEntry> entries = runLsHistory(time,viewName,branchNames,viewPaths);
        List<HistoryEntry> filtered = filterEntries(entries);

        List<? extends Entry> changelog = buildChangelog(viewName,filtered);
        return changelog;
    }


    protected List<HistoryEntry> runLsHistory(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        ClearToolFormatHandler historyHandler = getHistoryFormatHandler();
        List<HistoryEntry> fullList = new ArrayList<HistoryEntry>();
        System.err.println("format: |" + historyHandler.getFormat() + COMMENT + LINEEND + "|");
        try {
            for (String branchName : branchNames) {
                BufferedReader reader = new BufferedReader(cleartool.lshistory(historyHandler.getFormat() + COMMENT + LINEEND, time, viewName, branchName, viewPaths));
                fullList.addAll(parseLsHistory(reader));
                reader.close();
            }
        } catch (ParseException ex) {
            /* empty by design */
        }
        return fullList;
        //ChangeLogEntryMerger entryMerger = new ChangeLogEntryMerger(maxTimeDifferenceMillis);
        //return entryMerger.getMergedList(fullList);
    }

    protected List<HistoryEntry> parseLsHistory(BufferedReader reader) throws IOException, InterruptedException, ParseException{
        List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
        HistoryEntry currentEntry =null;

        StringBuilder commentBuilder = new StringBuilder();
        String line = reader.readLine();

        while (line != null) {
            //TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                line = reader.readLine();
                continue;
            }
            Matcher matcher = getHistoryFormatHandler().checkLine(line);

            // finder find start of lshistory entry
            if (matcher != null) {

                if (currentEntry != null) {
                    currentEntry.setComment(commentBuilder.toString());
                }

                commentBuilder = new StringBuilder();
                currentEntry = parseEventLine(matcher,line);

                String fileName = currentEntry.getElement();
                if (extendedViewPath != null) {
                    if (fileName.toLowerCase().startsWith(extendedViewPath)) {
                        fileName = fileName.substring(extendedViewPath.length());
                        currentEntry.setElement(fileName);
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

    protected List<HistoryEntry> filterEntries(List<HistoryEntry> unfiltered) throws IOException, InterruptedException{
        List<HistoryEntry> filtered = new ArrayList<HistoryEntry>();

        for (HistoryEntry entry : unfiltered) {
            boolean accepted = true;
            for (Filter filter : filters) {
                accepted = accepted & filter.accept(entry);
            }
            if (accepted) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    protected abstract List<? extends Entry> buildChangelog(String viewName,List<HistoryEntry> entries) throws IOException, InterruptedException;

    protected abstract ClearToolFormatHandler getHistoryFormatHandler();

    protected abstract HistoryEntry parseEventLine(Matcher matcher,String line) throws IOException, InterruptedException, ParseException;

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
