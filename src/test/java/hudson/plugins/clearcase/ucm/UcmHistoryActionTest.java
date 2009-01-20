package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.Filter;

import java.util.ArrayList;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class UcmHistoryActionTest {

    private Mockery context;
    private ClearTool cleartool;

    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
        
    }

    /*
     * Below are taken from DefaultPollActionTest
     */

    @Test
    public void assertSeparateBranchCommands() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader("")));
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchtwo")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" \"activity\" ")));
            }
        });

        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);
        context.assertIsSatisfied();
    }

//    @Test
//    public void assertFirstFoundChangeStopsPolling() throws Exception {
//        context.checking(new Expectations() {
//            {
//                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));
//                will(returnValue(new StringReader("\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" \"activity\" ")));
//            }
//        });
//
//        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
//        boolean hasChange = action.hasChanges(null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
//        assertTrue("The getChanges() method did not report a change", hasChange);
//        context.assertIsSatisfied();
//    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader(
                        "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" \"activity\" "
                      + "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" \"activity\" ")));
            }
        });

        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
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
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());
        UcmHistoryAction action = new UcmHistoryAction(cleartool,filters);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader("\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" \"activity\" ")));
            }
        });
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());
        UcmHistoryAction action = new UcmHistoryAction(cleartool,filters);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader(
                        "\"20080326.110739\" \"username\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\" \"activity\" ")));
            }
        });

        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DestroySubBranchFilter());

        UcmHistoryAction action = new UcmHistoryAction(cleartool,filters);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertNotIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(new StringReader(
                        "\"20080326.110739\" \"username\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\" \"activity\" ")));
            }
        });


        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test(expected=IOException.class)
    public void assertReaderIsClosed() throws Exception {
        final StringReader reader = new StringReader("\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" \"activity\" ");
        context.checking(new Expectations() {
            {
                ignoring(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                will(returnValue(reader));
            }
        });

        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        reader.ready();
    }



    /*
     * Below are taken from UcmBaseChangelogActionTest
     */
    @Test
    public void assertFormatContainsComment() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(equal("\\\"%Nd\\\" \\\"%u\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%e\\\" \\\"%o\\\" \\\"%[activity]p\\\" \\n%c\\n")),
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));                
                will(returnValue(new StringReader("")));
            }
        });
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        context.assertIsSatisfied();
    }
    
    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("IGNORED")), with(equal("Release_2_1_int")), with(equal(new String[]{"vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"user\"" +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"destroy sub-branch \"esmalling_branch\" of branch\" " +
                        "\"checkin\" \"activity\" ")));
            }
        });
        
        List<Filter> filters = new ArrayList<Filter>();

        filters.add(new DestroySubBranchFilter());
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,filters);
        @SuppressWarnings("unchecked")
        List<UcmActivity> activities = (List<UcmActivity>) action.getChanges(null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("There should be 0 activity", 0, activities.size());
    }

    
    @Test
    public void assertParsingOfNonIntegrationActivity() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("IGNORED")), with(equal("Release_2_1_int")), with(equal(new String[]{"vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"username\" "+
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +                        
                        "\"create directory version\" " +
                        "\"checkin\"  " +
                        "\"Release_3_3_jdk5.20080509.155359\" ")));
                one(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                                "\"Release_3_3_jdk5\" " +
                                "\"bob\" ")));
            }
        });
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        List<UcmActivity> activities = (List<UcmActivity>) action.getChanges(null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = activities.get(0);
        assertEquals("Activity name is incorrect", "Release_3_3_jdk5.20080509.155359", activity.getName());
        assertEquals("Activity headline is incorrect", "Convert to Java 6", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "Release_3_3_jdk5", activity.getStream());
        assertEquals("Activity user is incorrect", "bob", activity.getUser());
    }
    
    @Test
    public void assertParsingOfIntegrationActivity() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("IGNORED")), with(equal("Release_2_1_int")), with(equal(new String[]{"vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"username\"  " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +                        
                        "\"create directory version\" " +
                        "\"checkin\" " +
                        "\"deliver.Release_3_3_jdk5.20080509.155359\" ")));
                one(cleartool).lsactivity(
                        with(equal("deliver.Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                                "\"Release_3_3_jdk5\" " +
                                "\"bob\" " +
                                "\"maven2_Release_3_3.20080421.154619 maven2_Release_3_3.20080421.163355\" ")));
                one(cleartool).lsactivity(
                        with(equal("maven2_Release_3_3.20080421.154619")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Deliver maven2\" " +
                                "\"Release_3_3\" " +
                                "\"doe\" " +
                                "\"John Doe\" ")));
                one(cleartool).lsactivity(
                        with(equal("maven2_Release_3_3.20080421.163355")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Deliver maven3\" " +
                                "\"Release_3_3\" " +
                                "\"doe\" " +
                                "\"John Doe\" ")));
            }
        });
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        List<UcmActivity> activities = (List<UcmActivity>) action.getChanges(null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = activities.get(0);
        assertEquals("Activity name is incorrect", "deliver.Release_3_3_jdk5.20080509.155359", activity.getName());
        assertEquals("Activity headline is incorrect", "Convert to Java 6", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "Release_3_3_jdk5", activity.getStream());
        assertEquals("Activity user is incorrect", "bob", activity.getUser());
        
        List<UcmActivity> subActivities = activity.getSubActivities();
        assertEquals("There should be 2 sub activities", 2, subActivities.size());
        assertEquals("Name of first sub activity is incorrect", "maven2_Release_3_3.20080421.154619", subActivities.get(0).getName());
        assertEquals("Name of second sub activity is incorrect", "maven2_Release_3_3.20080421.163355", subActivities.get(1).getName());
    }

    @Test(expected=IOException.class)
    public void assertLshistoryReaderIsClosed() throws Exception {
        final StringReader lshistoryReader = new StringReader(
                "\"20080509.140451\" " +
                "\"username\" " +
                "\"vobs/projects/Server//config-admin-client\" " +
                "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +                
                "\"create directory version\" " +
                "\"checkin\" "+
                "\"Release_3_3_jdk5.20080509.155359\" ");
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("IGNORED")), with(equal("Release_2_1_int")), with(equal(new String[]{"vobs/projects/Server"})));
                will(returnValue(lshistoryReader));
                ignoring(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                        "\"Release_3_3_jdk5\" " +
                        "\"bob\" ")));
            }
        });
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        action.getChanges( null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        lshistoryReader.ready();
    }

    @Test(expected=IOException.class)
    public void assertLsactivityReaderIsClosed() throws Exception {
        final StringReader lsactivityReader = new StringReader("\"Convert to Java 6\" " +
                "\"Release_3_3_jdk5\" " +
                "\"bob\" ");
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("IGNORED")), with(equal("Release_2_1_int")), with(equal(new String[]{"vobs/projects/Server"})));
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"username\" " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +                        
                        "\"create directory version\" " +
                        "\"checkin\"  "+ 
                        "\"Release_3_3_jdk5.20080509.155359\" " )));
                ignoring(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(lsactivityReader));
            }
        });
        
        UcmHistoryAction action = new UcmHistoryAction(cleartool,null);
        action.getChanges(null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        lsactivityReader.ready();
    }
}
