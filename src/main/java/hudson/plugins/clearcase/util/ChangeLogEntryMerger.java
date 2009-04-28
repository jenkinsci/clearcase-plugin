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
package hudson.plugins.clearcase.util;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that merges log entries into
 */
public class ChangeLogEntryMerger {

    private Map<String, List<MergedLogEntry>> userEntries = new HashMap<String, List<MergedLogEntry>>();

    private transient int maxTimeDifference;

    public ChangeLogEntryMerger() {
        this(0);
    }

    public ChangeLogEntryMerger(int maxTimeDifferenceMillis) {
        this.maxTimeDifference = maxTimeDifferenceMillis + 1000;
    }

    public List<ClearCaseChangeLogEntry> getMergedList(List<ClearCaseChangeLogEntry> orgList) {
        userEntries.clear();
        for (ClearCaseChangeLogEntry entry : orgList) {
            boolean wasMerged = false;
            List<MergedLogEntry> entries = getUserEntries(entry.getUser());
            for (MergedLogEntry storedEntry : entries) {
                if (canBeMerged(storedEntry, entry)) {
                    storedEntry.merge(entry);
                    wasMerged = true;
                    break;
                }
            }
            if (!wasMerged) {
                entries.add(new MergedLogEntry(entry));
            }
        }
        List<ClearCaseChangeLogEntry> list = getList();
        Collections.sort(list, new Comparator<ClearCaseChangeLogEntry>() {
            public int compare(ClearCaseChangeLogEntry o1, ClearCaseChangeLogEntry o2) {
                return o2.getDate().compareTo(o1.getDate());
            }
        });
        return list;
    }

    private List<ClearCaseChangeLogEntry> getList() {
        List<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        Set<String> users = userEntries.keySet();
        for (String user : users) {
            List<MergedLogEntry> userList = userEntries.get(user);
            for (MergedLogEntry entry : userList) {
                entry.entry.setDate(entry.oldest);
                list.add(entry.entry);
            }
        }
        return list;
    }

    private List<MergedLogEntry> getUserEntries(String user) {
        if (!userEntries.containsKey(user)) {
            userEntries.put(user, new ArrayList<MergedLogEntry>());
        }
        return userEntries.get(user);
    }

    private boolean canBeMerged(MergedLogEntry entryOne, ClearCaseChangeLogEntry entryTwo) {
        if (entryOne.entry.getComment().equals(entryTwo.getComment())) {

            long oldestDiff = Math.abs(entryOne.oldest.getTime() - entryTwo.getDate().getTime());
            long newestDiff = Math.abs(entryOne.newest.getTime() - entryTwo.getDate().getTime());
            return (oldestDiff < maxTimeDifference) || (newestDiff < maxTimeDifference);
        }
        return false;
    }
    
    private class MergedLogEntry {
        private ClearCaseChangeLogEntry entry;
        private Date oldest;
        private Date newest;
        
        public MergedLogEntry(ClearCaseChangeLogEntry entry) {
            this.entry = entry;
            oldest = entry.getDate();
            newest = entry.getDate();
        }
        
        public void merge(ClearCaseChangeLogEntry newEntry) {
            Date date = newEntry.getDate();
            if (date.after(newest)) {
                newest = date;
            } else {
                if (date.before(oldest)) {
                    oldest = date;
                }
            }
            entry.addElements(newEntry.getElements());
        }
    }
}
