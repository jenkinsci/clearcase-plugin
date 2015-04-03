/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.ucm;

import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.ucm.model.ActivitiesDelta;
import hudson.plugins.clearcase.ucm.model.Activity;

import java.util.ArrayList;
import java.util.List;

public class EntryListAdapter {

    public List<HistoryEntry> adapt(ActivitiesDelta activities) {
        List<HistoryEntry> historyEntries = new ArrayList<HistoryEntry>();
        for (Activity activity : activities.getRight()) {
            HistoryEntry historyEntry = new HistoryEntry();
            historyEntry.setActivityHeadline(activity.getHeadline());
            historyEntry.setActivityName(activity.getSelector());
            historyEntry.setEvent("ucm activity");
            historyEntries.add(historyEntry);
        }
        return historyEntries;
    }

}
