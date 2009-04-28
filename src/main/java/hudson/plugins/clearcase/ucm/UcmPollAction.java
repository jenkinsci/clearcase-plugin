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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.clearcase.ucm;

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
public class UcmPollAction extends DefaultPollAction {

    private static final String[] HISTORY_FORMAT = {DATE_NUMERIC,
        NAME_ELEMENTNAME,
        NAME_VERSIONID,
        UCM_VERSION_ACTIVITY,
        EVENT,
        OPERATION
    };

    private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(HISTORY_FORMAT);

    public UcmPollAction(ClearTool cleartool, List<Filter> filters) {
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
        entry.setActivityName(matcher.group(4));
        entry.setEvent(matcher.group(5));
        entry.setOperation(matcher.group(6));
        return entry;
    }
}
