package hudson.plugins.clearcase.util;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class that merges log entries into
 */
public class ChangeLogEntryMerger {

    private Map<String, List<ClearCaseChangeLogEntry>> userEntries = new HashMap<String, List<ClearCaseChangeLogEntry>>();

    private int maxTimeDifference;

    public ChangeLogEntryMerger() {
        this(0);
    }

    public ChangeLogEntryMerger(int maxTimeDifferenceMillis) {
        this.maxTimeDifference = maxTimeDifferenceMillis + 1000;
    }

    public List<ClearCaseChangeLogEntry> getMergedList(List<ClearCaseChangeLogEntry> orgList) {
        for (ClearCaseChangeLogEntry entry : orgList) {
            boolean wasMerged = false;
            List<ClearCaseChangeLogEntry> entries = getUserEntries(entry.getUser());
            for (ClearCaseChangeLogEntry storedEntry : entries) {
                if (canBeMerged(entry, storedEntry)) {
                    storedEntry.addFiles(entry.getAffectedPaths());
                    wasMerged = true;
                    break;
                }
            }
            if (!wasMerged) {
                entries.add(entry);
            }
        }
        List<ClearCaseChangeLogEntry> list = getList();
        Collections.sort(list, new Comparator<ClearCaseChangeLogEntry>() {
            public int compare(ClearCaseChangeLogEntry o1, ClearCaseChangeLogEntry o2) {
                return o1.getDate().compareTo(o2.getDate());
            }
        });
        return list;
    }

    private List<ClearCaseChangeLogEntry> getList() {
        List<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        Set<String> users = userEntries.keySet();
        for (String user : users) {
            List<ClearCaseChangeLogEntry> userList = userEntries.get(user);
            for (ClearCaseChangeLogEntry entry : userList) {
                list.add(entry);
            }
        }
        return list;
    }

    private List<ClearCaseChangeLogEntry> getUserEntries(String user) {
        if (!userEntries.containsKey(user)) {
            userEntries.put(user, new ArrayList<ClearCaseChangeLogEntry>());
        }
        return userEntries.get(user);
    }

    private boolean canBeMerged(ClearCaseChangeLogEntry entryOne, ClearCaseChangeLogEntry entryTwo) {
        if (entryOne.getComment().equals(entryTwo.getComment())) {
            long difference = Math.abs(entryOne.getDate().getTime() - entryTwo.getDate().getTime());
            return (difference < maxTimeDifference);
        }
        return false;
    }
}
