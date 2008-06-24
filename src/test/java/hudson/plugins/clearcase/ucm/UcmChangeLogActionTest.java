package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.EventRecordFilter;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class UcmChangeLogActionTest {

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
                one(cleartool).lshistory(with(equal("\\\"%Nd\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%[activity]p\\\" \\\"%e\\\" \\\"%o\\\" \\n%c\\n")), 
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));                
                will(returnValue(new StringReader("")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        action.getChanges(filter, new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
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
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"\" " +
                        "\"destroy sub-branch \"esmalling_branch\" of branch\" " +
                        "\"checkin\" ")));
            }
        });
        
        filter.setFilterOutDestroySubBranchEvent(true);
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);        
        List<UcmActivity> activities = action.getChanges(filter, null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
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
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" ")));
                one(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                                "\"Release_3_3_jdk5\" " +
                                "\"bob\" ")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        List<UcmActivity> activities = action.getChanges(filter, null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
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
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"rebase.Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" ")));
                one(cleartool).lsactivity(
                        with(equal("rebase.Release_3_3_jdk5.20080509.155359")), 
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
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        List<UcmActivity> activities = action.getChanges(filter, null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = activities.get(0);
        assertEquals("Activity name is incorrect", "rebase.Release_3_3_jdk5.20080509.155359", activity.getName());
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
                "\"vobs/projects/Server//config-admin-client\" " +
                "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                "\"Release_3_3_jdk5.20080509.155359\" " +
                "\"create directory version\" " +
                "\"checkin\" ");
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
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        action.getChanges(filter, null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
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
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" ")));
                ignoring(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(lsactivityReader));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool);
        action.getChanges(filter, null, "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        lsactivityReader.ready();
    }
}
