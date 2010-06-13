/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import static hudson.plugins.clearcase.util.OutputFormat.COMMENT;
import static hudson.plugins.clearcase.util.OutputFormat.LINEEND;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;
import hudson.scm.ChangeLogSet.Entry;

import java.io.BufferedReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * @author hlyh
 */
public abstract class AbstractHistoryAction implements HistoryAction {

    protected ClearTool cleartool;
    private Filter filter;
    protected String extendedViewPath;
    protected boolean isDynamicView;

    public AbstractHistoryAction(ClearTool cleartool, boolean isDynamicView, Filter filter) {
        this.cleartool = cleartool;
        this.filter = filter;
        this.isDynamicView = isDynamicView;
    }

    protected abstract List<? extends Entry> buildChangelog(String viewPath, List<HistoryEntry> entries) throws IOException, InterruptedException;

    protected List<HistoryEntry> filterEntries(List<HistoryEntry> entries) throws IOException, InterruptedException {
        if (filter == null) {
            return entries;
        }
        List<HistoryEntry> filtered = new ArrayList<HistoryEntry>();
        for (HistoryEntry entry : entries) {
            if (filter.accept(entry)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    @Override
    public List<? extends Entry> getChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<HistoryEntry> entries = runLsHistory(time, viewPath, viewTag, branchNames, viewPaths);
        List<HistoryEntry> filtered = filterEntries(entries);

        List<? extends Entry> changelog = buildChangelog(viewPath, filtered);
        return changelog;
    }

    public String getExtendedViewPath() {
        return extendedViewPath;
    }

    protected abstract ClearToolFormatHandler getHistoryFormatHandler();

    @Override
    public boolean hasChanges(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        List<HistoryEntry> entries = runLsHistory(time, viewPath, viewTag, branchNames, viewPaths);
        List<HistoryEntry> filtered = filterEntries(entries);
        return filtered.size() > 0;
    }

    protected abstract HistoryEntry parseEventLine(Matcher matcher, String line) throws IOException, InterruptedException, ParseException;

    protected void parseLsHistory(BufferedReader reader, Collection<HistoryEntry> history) throws IOException, InterruptedException, ParseException {
        HistoryEntry currentEntry = null;

        for(String line = reader.readLine(); line != null; line = reader.readLine()) {
            // TODO: better error handling
            if (line.startsWith("cleartool: Error:")) {
                continue;
            }
            Matcher matcher = getHistoryFormatHandler().checkLine(line);
            
            // finder find start of lshistory entry
            if (matcher != null) {
                currentEntry = parseEventLine(matcher, line);
                // Trim the extended view path
                currentEntry.setElement(StringUtils.removeStart(currentEntry.getElement(), extendedViewPath));
                history.add(currentEntry);
            } else {
                if (currentEntry != null) {
                    currentEntry.appendComment(line).appendComment("\n");
                } else {
                    Logger.getLogger(AbstractHistoryAction.class.getName()).warning("Got the comment : \"" + line + "\" but couldn't attach it to any entry");
                }
            }
        }
    }

    protected List<HistoryEntry> runLsHistory(Date time, String viewPath, String viewTag, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        Validate.notNull(viewPath);
        List<HistoryEntry> history = new ArrayList<HistoryEntry>();
        if (!cleartool.doesViewExist(viewTag)) {
            return history;
        }
        if (isDynamicView) {
                cleartool.startView(viewTag);
        }

        if (ArrayUtils.isEmpty(viewPaths)) {
            return history;
        }
        try {
            for (String branchName : branchNames) {
                BufferedReader reader = new BufferedReader(cleartool.lshistory(getHistoryFormatHandler().getFormat() + COMMENT + LINEEND, time, viewPath, branchName, viewPaths));
                parseLsHistory(reader, history);
                reader.close();
            }
        } catch (ParseException ex) {
            /* empty by design */
        }
        return history;
    }

    /**
     * Sets the extended view path. The extended view path will be removed from file paths in the event. The extended
     * view path is for example the view root + view name; and this path shows up in the history and can be confusing for
     * users.
     * 
     * @param path the new extended view path.
     */
    public void setExtendedViewPath(String path) {
        this.extendedViewPath = path;
    }
}
