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
package hudson.plugins.clearcase.action;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Default action for polling for changes in a repository.
 */
public abstract class DefaultPollAction implements PollAction {
    
    private ClearTool cleartool;
    protected List<Filter> filters;
    
    public DefaultPollAction(ClearTool cleartool,List<Filter> filters) {
        this.cleartool = cleartool;
        this.filters = filters;
    }

    @Override
    public boolean getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException {
        boolean hasChanges = false;
        ClearToolFormatHandler historyHandler = getHistoryFormatHandler();

        for (int i = 0; (i < branchNames.length) && (!hasChanges); i++) {
            String branchName = branchNames[i];

            Reader lshistoryOutput = cleartool.lshistory(historyHandler.getFormat(), time, viewName, branchName, viewPaths);

            if (parseHistoryOutputForChanges(new BufferedReader(lshistoryOutput))) {
                hasChanges = true;
            }
            lshistoryOutput.close();
        } 
        return hasChanges;
    }

    protected abstract ClearToolFormatHandler getHistoryFormatHandler();
    
    protected abstract HistoryEntry parseLine(String line) throws ParseException;

    protected boolean parseHistoryOutputForChanges(BufferedReader reader) throws IOException, InterruptedException {
        String line = reader.readLine();
        while (line != null) {
            try {
                HistoryEntry entry = parseLine(line);

                if (entry != null) {
                    boolean accepted = filterEntry(entry);

                    if (accepted) {
                        return true;
                    }
                }
            } catch (ParseException e) {
                cleartool.getLauncher().getListener().getLogger().append("ClearCase Plugin: This line could not be parsed: "+ line);
            }
            line = reader.readLine();
        }
        return false;
    }

    protected boolean filterEntry(HistoryEntry entry) {
        boolean included = true;
        if (filters==null) {
            return true;
        }

        for (Filter filter : filters) {
            included = included & filter.accept(entry);
        }

        return included;
    }

}
