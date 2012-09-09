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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.Filter;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.mockito.Mock;

public class UcmChangeLogActionTest extends AbstractWorkspaceTest {

    @Mock
    private ClearTool cleartool;

    @Test
    public void assertFormatContainsComment() throws Exception {
        when(
                cleartool.lshistory(eq("\\\"%Nd\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%[activity]Xp\\\" \\\"%e\\\" \\\"%o\\\" \\\"%u\\\" \\n%c\\n"), any(Date.class),
                        anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenReturn(new StringReader(""));

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, null);
        action.getChanges(new Date(), "IGNORED", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        verify(cleartool).lshistory(eq("\\\"%Nd\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%[activity]Xp\\\" \\\"%e\\\" \\\"%o\\\" \\\"%u\\\" \\n%c\\n"), any(Date.class),
                anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean());
    }

    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"), eq(new String[] { "VIEW_NAME" + File.separator
                        + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                new StringReader("\"20080509.140451\" " + "\"vobs/projects/Server//config-admin-client\" "
                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"\" " + "\"destroy sub-branch \"esmalling_branch\" of branch\" "
                        + "\"checkin\" \"username\" "));

        List<Filter> filters = new ArrayList<Filter>();

        filters.add(new DestroySubBranchFilter());

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, filters);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        assertEquals("There should be 0 activity", 0, activities.size());
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"),
                eq(new String[] { "VIEW_NAME" + File.separator + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE));
    }

    @Test
    public void assertParsingOfNonIntegrationActivity() throws Exception {
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"), eq(new String[] { "VIEW_NAME" + File.separator
                        + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                new StringReader("\"20080509.140451\" " + "\"vobs/projects/Server//config-admin-client\" "
                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"Release_3_3_jdk5.20080509.155359\" " + "\"create directory version\" "
                        + "\"checkin\" \"username\" "));
        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "));

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, null);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = activities.get(0);
        assertEquals("Activity name is incorrect", "Release_3_3_jdk5.20080509.155359", activity.getName());
        assertEquals("Activity headline is incorrect", "Convert to Java 6", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "Release_3_3_jdk5", activity.getStream());
        assertEquals("Activity user is incorrect", "bob", activity.getUser());

        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"),
                eq(new String[] { "VIEW_NAME" + File.separator + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE));
        verify(cleartool).lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull());
    }

    @Test
    public void assertParsingOfIntegrationActivity() throws Exception {
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"), eq(new String[] { "VIEW_NAME" + File.separator
                        + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                new StringReader("\"20080509.140451\" " + "\"vobs/projects/Server//config-admin-client\" "
                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"deliver.Release_3_3_jdk5.20080509.155359\" "
                        + "\"create directory version\" " + "\"checkin\" \"username\" "));
        when(cleartool.lsactivity(eq("deliver.Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "
                        + "\"maven2_Release_3_3.20080421.154619 maven2_Release_3_3.20080421.163355\" "));
        when(cleartool.lsactivity(eq("maven2_Release_3_3.20080421.154619"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Deliver maven2\" " + "\"Release_3_3\" " + "\"doe\" " + "\"John Doe\" "));
        when(cleartool.lsactivity(eq("maven2_Release_3_3.20080421.163355"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Deliver maven3\" " + "\"Release_3_3\" " + "\"doe\" " + "\"John Doe\" "));

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, null);
        List<UcmActivity> activities = action.getChanges(null, "VIEW_NAME", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
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

        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"),
                eq(new String[] { "VIEW_NAME" + File.separator + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE));
        verify(cleartool).lsactivity(eq("deliver.Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull());
        verify(cleartool).lsactivity(eq("maven2_Release_3_3.20080421.154619"), (String) notNull(), (String) notNull());
        verify(cleartool).lsactivity(eq("maven2_Release_3_3.20080421.163355"), (String) notNull(), (String) notNull());
    }

    @Test(expected = IOException.class)
    public void assertLshistoryReaderIsClosed() throws Exception {
        final StringReader lshistoryReader = new StringReader("\"20080509.140451\" " + "\"vobs/projects/Server//config-admin-client\" "
                + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"Release_3_3_jdk5.20080509.155359\" " + "\"create directory version\" "
                + "\"checkin\" \"username\" ");
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"), eq(new String[] { "VIEW_NAME" + File.separator
                        + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(lshistoryReader);
        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "));

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, null);
        action.getChanges(null, "VIEW_NAME", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"),
                eq(new String[] { "VIEW_NAME" + File.separator + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE));
        lshistoryReader.ready();

    }

    @Test(expected = IOException.class)
    public void assertLsactivityReaderIsClosed() throws Exception {
        final StringReader lsactivityReader = new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" ");
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"), eq(new String[] { "VIEW_NAME" + File.separator
                        + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                new StringReader("\"20080509.140451\" " + "\"vobs/projects/Server//config-admin-client\" "
                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"Release_3_3_jdk5.20080509.155359\" " + "\"create directory version\" "
                        + "\"checkin\" \"username\" "));
        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(lsactivityReader);

        UcmChangeLogAction action = new UcmChangeLogAction(cleartool, null);
        action.getChanges(null, "VIEW_NAME", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });

        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("VIEW_NAME"), eq("Release_2_1_int"),
                eq(new String[] { "VIEW_NAME" + File.separator + "vobs/projects/Server" }), eq(Boolean.FALSE), eq(Boolean.FALSE));

        lsactivityReader.ready();
    }
}
