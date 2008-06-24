package hudson.plugins.clearcase.base;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import hudson.plugins.clearcase.AbstractClearCaseScm;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;
import hudson.plugins.clearcase.util.EventRecordFilter;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class BaseChangeLogActionTest {

    private Mockery context;
    private ClearTool cleartool;
    private EventRecordFilter filter;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
        filter = new EventRecordFilter();
    }

    @Test
    public void assertFormatContainsComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(equal("\\\"%Nd\\\" \\\"%u\\\" \\\"%e\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%o\\\" \\n%c\\n")), 
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));
                will(returnValue(new StringReader("")));
            }
        });
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 0);
        action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        context.assertIsSatisfied();
    }

    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), 
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"destroy sub-branch \"esmalling_branch\" of branch\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n")));
            }
        });
        
        filter.setFilterOutDestroySubBranchEvent(true);
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("The event record should be ignored", 0, changes.size());        
        context.assertIsSatisfied();        
    }

    @Test
    public void assertMergedLogEntries() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), 
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n"
                      + "\"20070906.091705\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n")));
            }
        });
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Two entries should be merged into one", 1, changes.size());        
        context.assertIsSatisfied();        
    }

    @Test(expected=IOException.class)
    public void assertReaderIsClosed() throws Exception {
        final StringReader reader = new StringReader("\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n");                
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(reader));
            }
        });
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        reader.ready();
    }

    @Test
    public void testSorted() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070827.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                      + "\"20070825.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                      + "\"20070830.084801\"   \"inttest1\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n")));
            }
        });
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 3, changes.size());
        assertEquals("First entry is incorrect", "inttest1", changes.get(0).getUser());
        assertEquals("First entry is incorrect", "inttest2", changes.get(1).getUser());
        assertEquals("First entry is incorrect", "inttest3", changes.get(2).getUser());
        context.assertIsSatisfied();
    }

    @Test
    public void testMultiline() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070830.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n"
                        + "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n")));
            }
        });
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, changes.size());
    }

    @Test
    public void testErrorOutput() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                        + "cleartool: Error: Branch type not found: \"sit_r6a\".\n"
                        + "\"20070829.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        assertEquals("First entry is incorrect", "", entries.get(0).getComment());
        assertEquals("Scond entry is incorrect", "", entries.get(1).getComment());
    }

    @Test
    public void testUserOutput() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new InputStreamReader(
                        AbstractClearCaseScm.class.getResourceAsStream( "ct-lshistory-1.log"))));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, entries.size());
    }

    @Test
    public void testOperation() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"  \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        FileElement element = entries.get(0).getElements().get(0);
        assertEquals("Status is incorrect", "mkelem", element.getOperation());
    }

    @Test
    public void testParseNoComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                "\"20070827.084801\" \"inttest14\" \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});

        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Definitions\\Definitions.csproj", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "inttest14", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 48, 1), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\1", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testEmptyComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testCommentWithEmptyLine() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\ntext\n\nend of comment")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});

        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Comment is incorrect", "text\n\nend of comment", entry.getComment());
    }

    @Test
    public void testParseWithComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\nBUG8949")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949", entry.getComment());
    }

    @Test
    public void testParseWithTwoLineComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\nBUG8949\nThis fixed the problem")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949\nThis fixed the problem", entry.getComment());
    }

    @Test
    public void testParseWithLongAction() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create a version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Action is incorrect", "create a version", entry.getElements().get(0).getAction());
    }

    @Test
    public void assertViewPathIsRemovedFromFilePaths() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                        with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070827.085901\" \"user\" \"action\" \"/view/ralef_0.2_nightly/vobs/Tools/framework/util/QT.h\" \"/main/comain\"  \"mkelem\"\n")));
            }
        });

        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 1000);
        action.setExtendedViewPath("/view/ralef_0.2_nightly");
        List<ClearCaseChangeLogEntry> entries = action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File path is incorrect", "/vobs/Tools/framework/util/QT.h", entry.getElements().get(0).getFile());
    }
    
    private Date getDate(int year, int month, int day, int hour, int min, int sec) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        return calendar.getTime();
    }
}
