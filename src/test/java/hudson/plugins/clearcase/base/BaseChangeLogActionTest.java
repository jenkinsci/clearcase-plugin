package hudson.plugins.clearcase.base;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.util.Date;
import java.util.List;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class BaseChangeLogActionTest {

    private Mockery context;
    private ClearTool cleartool;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
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
        action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        context.assertIsSatisfied();
    }

    @Test
    public void assertMergedLogEntries() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(equal("\\\"%Nd\\\" \\\"%u\\\" \\\"%e\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%o\\\" \\n%c\\n")), 
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));
                will(returnValue(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n"
                      + "\"20070906.091705\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"mkelem\"\n")));
            }
        });
        
        BaseChangeLogAction action = new BaseChangeLogAction(cleartool, 10000);
        List<ClearCaseChangeLogEntry> changes = action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Two entries should be merged into one", 1, changes.size());        
        context.assertIsSatisfied();        
    }
}
