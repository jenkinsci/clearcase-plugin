package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.EventRecordFilter;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class DefaultPollActionTest {

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
    public void assertSeparateBranchCommands() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("")));
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchtwo")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test
    public void assertFirstFoundChangeStopsPolling() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader(
                        "\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" "
                      + "\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringErrors() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("cleartool: Error: Not an object in a vob: \"view.dat\".\n")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" ")));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader(
                        "\"20080326.110739\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\"")));
            }
        });

        filter.setFilterOutDestroySubBranchEvent(true);
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);        
        context.assertIsSatisfied();
    }
    
    @Test
    public void assertNotIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));                
                will(returnValue(new StringReader(
                        "\"20080326.110739\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\"")));
            }
        });

        filter.setFilterOutDestroySubBranchEvent(false);
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        boolean hasChange = action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method reported a change", hasChange);        
        context.assertIsSatisfied();
    }

    @Test(expected=IOException.class)
    public void assertReaderIsClosed() throws Exception {                
        final StringReader reader = new StringReader("\"20071015.151822\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" ");
        context.checking(new Expectations() {
            {
                ignoring(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(reader));
            }
        });
        
        DefaultPollAction action = new DefaultPollAction(cleartool);
        action.getChanges(filter, null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        reader.ready();
    }
}
