package hudson.plugins.clearcase.util;

import static org.junit.Assert.*;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ChangeLogEntryMergerTest {

    private List<ClearCaseChangeLogEntry> list;
    private ChangeLogEntryMerger changeLogEntryMerger;

    @Before
    public void setup() {
        list = new ArrayList<ClearCaseChangeLogEntry>();
    }

    @Test
    public void testOneUserOneCommit() {
        changeLogEntryMerger = new ChangeLogEntryMerger();
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment", "file3", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 3, mergedList.get(0).getAffectedPaths().size());
    }

    @Test
    public void testOneUserOneCommitWithDelay() {
        changeLogEntryMerger = new ChangeLogEntryMerger(1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 02), "user", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 2, mergedList.get(0).getAffectedPaths().size());
    }

    @Test
    public void testOneUserTwoCommits() {
        changeLogEntryMerger = new ChangeLogEntryMerger();
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment2", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 02), "user", "action", "comment1", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 02), "user", "action", "comment1", "file3", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 2, mergedList.size());
        assertEquals("The number of files are incorrect", 2, mergedList.get(0).getAffectedPaths().size());
        assertEquals("The number of files are incorrect", 1, mergedList.get(1).getAffectedPaths().size());
    }

    @Test
    public void testTwoUsersOneCommit() {
        changeLogEntryMerger = new ChangeLogEntryMerger();
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user1", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user2", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 2, mergedList.size());
        assertEquals("The number of files are incorrect", 1, mergedList.get(0).getAffectedPaths().size());
        assertEquals("The number of files are incorrect", 1, mergedList.get(1).getAffectedPaths().size());
    }

    @Test
    public void testLongTimeWindow() {
        changeLogEntryMerger = new ChangeLogEntryMerger(60*1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user1", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 52), "user1", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 2, mergedList.get(0).getAffectedPaths().size());
    }

    @Test
    public void testGrowingTimeWindow() {
        changeLogEntryMerger = new ChangeLogEntryMerger(5*1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user1", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 25), "user1", "action", "comment", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 25), "user1", "action", "comment 2", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 25), "user2", "action", "comment", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 29), "user1", "action", "comment", "file3", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 19), "user1", "action", "comment", "file4", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user1", "action", "comment", "file5", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 4, mergedList.size());
        assertEquals("The number of files are incorrect", 1, mergedList.get(3).getAffectedPaths().size());
        assertEquals("The file in the files list is incorrect", "file5", getIndexOf(mergedList.get(3).getAffectedPaths(), 0));
        assertEquals("The number of files are incorrect", 4, mergedList.get(2).getAffectedPaths().size());
        assertEquals("The file in the files list is incorrect", "file1", getIndexOf(mergedList.get(2).getAffectedPaths(), 0));
        assertEquals("The file in the files list is incorrect", "file2", getIndexOf(mergedList.get(2).getAffectedPaths(), 1));
        assertEquals("The file in the files list is incorrect", "file3", getIndexOf(mergedList.get(2).getAffectedPaths(), 2));
        assertEquals("The file in the files list is incorrect", "file4", getIndexOf(mergedList.get(2).getAffectedPaths(), 3));
        assertEquals("The number of files are incorrect", 1, mergedList.get(1).getAffectedPaths().size());
        assertEquals("The file in the files list is incorrect", "file2", getIndexOf(mergedList.get(1).getAffectedPaths(), 0));
        assertEquals("The number of files are incorrect", 1, mergedList.get(0).getAffectedPaths().size());
        assertEquals("The file in the files list is incorrect", "file2", getIndexOf(mergedList.get(0).getAffectedPaths(), 0));
    }

    @Test
    public void testReusedInstance() {
        changeLogEntryMerger = new ChangeLogEntryMerger(60*1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user1", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 52), "user1", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 2, mergedList.get(0).getAffectedPaths().size());
        list.clear();
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user1", "action", "comment", "file1", "version"));
        mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 1, mergedList.get(0).getAffectedPaths().size());
    }

    @Test
    public void testReturnDateForFirstEntry() {
        changeLogEntryMerger = new ChangeLogEntryMerger(60*1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 32), "user1", "action", "comment", "file3", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user1", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 52), "user1", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 1, mergedList.size());
        assertEquals("The number of files are incorrect", 3, mergedList.get(0).getAffectedPaths().size());
        assertEquals("The date is incorrect", createDate(10, 01, 22), mergedList.get(0).getDate());
    }

    @Test
    public void testSortWithOldestEntryFirst() {
        changeLogEntryMerger = new ChangeLogEntryMerger(60*1000);
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 32), "user1", "action", "comment", "file3", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 22), "user2", "action", "comment", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 52), "user3", "action", "comment", "file2", "version"));
        List<ClearCaseChangeLogEntry> mergedList = changeLogEntryMerger.getMergedList(list);
        assertEquals("The entries was not merged", 3, mergedList.size());
        assertEquals("The date is incorrect in entry 1", createDate(10, 01, 52), mergedList.get(0).getDate());
        assertEquals("The date is incorrect in entry 2", createDate(10, 01, 32), mergedList.get(1).getDate());
        assertEquals("The date is incorrect in entry 3", createDate(10, 01, 22), mergedList.get(2).getDate());
    }

    private Date createDate(int hour, int min, int sec) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        return calendar.getTime();
    }
    
    private <T> T getIndexOf(Collection<T> collection, int index) {
        Iterator<T> iterator = collection.iterator();     
        int i = 0;
        while (iterator.hasNext()) {
            T t = iterator.next();
            if (index == i) {
                return t;
            }
            i++;
        }
        // Wont happen
        return null;
    }
}
