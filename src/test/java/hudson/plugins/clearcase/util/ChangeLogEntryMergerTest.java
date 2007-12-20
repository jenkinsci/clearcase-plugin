package hudson.plugins.clearcase.util;

import static org.junit.Assert.*;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 02), "user", "action", "comment2", "file2", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment1", "file1", "version"));
        list.add(new ClearCaseChangeLogEntry(createDate(10, 01, 01), "user", "action", "comment1", "file3", "version"));
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

    private Date createDate(int hour, int min, int sec) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        return calendar.getTime();
    }
}
