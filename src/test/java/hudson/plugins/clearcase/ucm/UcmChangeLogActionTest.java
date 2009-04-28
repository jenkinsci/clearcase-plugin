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
package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.io.File;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.util.EventRecordFilter;

import java.util.ArrayList;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class UcmChangeLogActionTest {

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
                one(cleartool).lshistory(with(equal("\\\"%Nd\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%[activity]p\\\" \\\"%e\\\" \\\"%o\\\" \\\"%u\\\" \\n%c\\n")),
                        with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                        with(any(String[].class)));                
                will(returnValue(new StringReader("")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,null);
        action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        context.assertIsSatisfied();
    }
    
    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("VIEW_NAME")), with(equal("Release_2_1_int")), with(equal(new String[]{"VIEW_NAME" + File.separator + "vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"\" " +
                        "\"destroy sub-branch \"esmalling_branch\" of branch\" " +
                        "\"checkin\" \"username\" ")));
            }
        });
        
        List<Filter> filters = new ArrayList<Filter>();

        filters.add(new DestroySubBranchFilter());
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,filters);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("There should be 0 activity", 0, activities.size());
    }

    
    @Test
    public void assertParsingOfNonIntegrationActivity() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("VIEW_NAME")), with(equal("Release_2_1_int")), with(equal(new String[]{"VIEW_NAME" + File.separator + "vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" \"username\" ")));
                one(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                                "\"Release_3_3_jdk5\" " +
                                "\"bob\" ")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,null);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
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
                        with(equal("VIEW_NAME")), with(equal("Release_2_1_int")), with(equal(new String[]{"VIEW_NAME" + File.separator + "vobs/projects/Server"})));                
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"deliver.Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" \"username\" ")));
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
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,null);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
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
                "\"vobs/projects/Server//config-admin-client\" " +
                "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                "\"Release_3_3_jdk5.20080509.155359\" " +
                "\"create directory version\" " +
                "\"checkin\" \"username\" ");
        context.checking(new Expectations() {
            {
                one(cleartool).lshistory(with(any(String.class)), with(aNull(Date.class)), 
                        with(equal("VIEW_NAME")), with(equal("Release_2_1_int")), with(equal(new String[]{"VIEW_NAME" + File.separator + "vobs/projects/Server"})));
                will(returnValue(lshistoryReader));
                ignoring(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(new StringReader("\"Convert to Java 6\" " +
                        "\"Release_3_3_jdk5\" " +
                        "\"bob\" ")));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,null);
        action.getChanges( null, "VIEW_NAME", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
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
                        with(equal("VIEW_NAME")), with(equal("Release_2_1_int")), with(equal(new String[]{"VIEW_NAME" + File.separator + "vobs/projects/Server"})));
                will(returnValue(new StringReader(
                        "\"20080509.140451\" " +
                        "\"vobs/projects/Server//config-admin-client\" " +
                        "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " +
                        "\"Release_3_3_jdk5.20080509.155359\" " +
                        "\"create directory version\" " +
                        "\"checkin\" \"username\" ")));
                ignoring(cleartool).lsactivity(
                        with(equal("Release_3_3_jdk5.20080509.155359")), 
                        with(aNonNull(String.class)),with(aNonNull(String.class)));
                will(returnValue(lsactivityReader));
            }
        });
        
        UcmChangeLogAction action = new UcmChangeLogAction(cleartool,null);
        action.getChanges(null, "VIEW_NAME", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        lsactivityReader.ready();
    }
}
