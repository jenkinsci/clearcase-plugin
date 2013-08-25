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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.AbstractClearCaseScm;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.ClearCaseUcmSCMDummy;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.scm.ChangeLogSet;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mock;

public class UcmHistoryActionTest extends AbstractWorkspaceTest {

    @Mock
    private AbstractBuild                             build;
    @Mock
    private ClearCaseUcmSCM.ClearCaseUcmScmDescriptor clearCaseUcmScmDescriptor;
    @Mock
    private ClearTool                                 cleartool;
    @Mock
    private ClearToolLauncher                         clearToolLauncher;
    @Mock
    private Launcher                                  launcher;

    /*
     * Below are taken from DefaultPollActionTest
     */

    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20080509.140451\" " + "\"user\"" + "\"vobs/projects/Server//config-admin-client\" "
                                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"destroy sub-branch \"esmalling_branch\" of branch\" "
                                        + "\"checkin\" \"activity\" "));

        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, new DestroySubBranchFilter(), null, null, null, null);
        @SuppressWarnings("unchecked")
        List<ChangeLogSet.Entry> activities = action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("There should be 0 activity", 0, activities.size());
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
    }

    @Test
    public void assertExcludedRegionsAreIgnored() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20080509.140451\" " + "\"user\"" + "\"vobs/projects/Server//config-admin-client\" "
                                        + "\"/main/Product/Release_3_3_int/activityA/2\" " + "\"create version\" " + "\"checkin\" \"activityA\" " + "\"20080509.140451\" "
                                        + "\"user\"" + "\"vobs/projects/Client//config-admin-client\" " + "\"/main/Product/Release_3_3_int/activityB/2\" "
                                        + "\"create version\" " + "\"checkin\" \"activityB\" "));
        when(cleartool.lsactivity(eq("activityB"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Activity B info \" " + "\"activityB\" " + "\"bob\" " + "\"maven2_Release_3_3.20080421.154619\" "));

        List<Filter> filters = new ArrayList<Filter>();

        filters.add(new DefaultFilter());
        filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, "Server"));
        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, new FilterChain(filters), null, null, null, null);
        List<ChangeLogSet.Entry> activities = action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("There should be 1 activity", 1, activities.size());
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
        verify(cleartool, times(0)).lsactivity(eq("activityA"), (String) notNull(), (String) notNull());
        verify(cleartool).lsactivity(eq("activityB"), (String) notNull(), (String) notNull());
    }

    /*
     * Below are taken from UcmBaseChangelogActionTest
     */
    @Test
    public void assertFormatContainsComment() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(eq("\\\"%Nd\\\" \\\"%u\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%e\\\" \\\"%o\\\" \\\"%[activity]p\\\" \\n%c\\n"), any(Date.class),
                        anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean())).thenReturn(new StringReader(""));

        UcmHistoryAction action = createUcmHistoryAction();
        action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });

        verify(cleartool).lshistory(eq("\\\"%Nd\\\" \\\"%u\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%e\\\" \\\"%o\\\" \\\"%[activity]p\\\" \\n%c\\n"), any(Date.class),
                anyString(), anyString(), any(String[].class), anyBoolean(), anyBoolean());
    }

    @Test
    public void assertIgnoringDestroySubBranchEvent() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE)))
                        .thenReturn(
                                new StringReader(
                                        "\"20080326.110739\" \"username\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\" \"activity\" "));
        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, new DestroySubBranchFilter(), null, null, null, null);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertFalse("The getChanges() method reported a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test
    public void assertIgnoringErrors() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE))).thenReturn(new StringReader("cleartool: Error: Not an object in a vob: \"view.dat\".\n"));

        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, new DefaultFilter(), null, null, null, null);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });

        assertFalse("The getChanges() method reported a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE))).thenReturn(
                                new StringReader(
                                        "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" \"activity\" "));
        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, new DefaultFilter(), null, null, null, null);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertFalse("The getChanges() method reported a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test(expected = IOException.class)
    public void assertLsactivityReaderIsClosed() throws Exception {
        final StringReader lsactivityReader = new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" ");
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20080509.140451\" " + "\"username\" " + "\"vobs/projects/Server//config-admin-client\" "
                                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"create directory version\" " + "\"checkin\"  "
                                        + "\"Release_3_3_jdk5.20080509.155359\" "));
        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(lsactivityReader);

        UcmHistoryAction action = createUcmHistoryAction();
        action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
        lsactivityReader.ready();
    }

    @Test(expected = IOException.class)
    public void assertLshistoryReaderIsClosed() throws Exception {
        final StringReader lshistoryReader = new StringReader("\"20080509.140451\" " + "\"username\" " + "\"vobs/projects/Server//config-admin-client\" "
                + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"create directory version\" " + "\"checkin\" "
                + "\"Release_3_3_jdk5.20080509.155359\" ");
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(lshistoryReader);

        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "));

        UcmHistoryAction action = createUcmHistoryAction();
        action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
        lshistoryReader.ready();
    }

    @Test
    public void assertNotIgnoringDestroySubBranchEvent() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE)))
                        .thenReturn(
                                new StringReader(
                                        "\"20080326.110739\" \"username\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\" \"activity\" "));
        UcmHistoryAction action = createUcmHistoryAction();
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertTrue("The getChanges() method reported a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test
    public void assertParsingOfIntegrationActivity() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20080509.140451\" " + "\"username\"  " + "\"vobs/projects/Server//config-admin-client\" "
                                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"create directory version\" " + "\"checkin\" "
                                        + "\"deliver.Release_3_3_jdk5.20080509.155359\" "));
        when(cleartool.lsactivity(eq("deliver.Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "
                        + "\"maven2_Release_3_3.20080421.154619 maven2_Release_3_3.20080421.163355\" "));
        when(cleartool.lsactivity(eq("maven2_Release_3_3.20080421.154619"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Deliver maven2\" " + "\"Release_3_3\" " + "\"doe\" " + "\"John Doe\" "));
        when(cleartool.lsactivity(eq("maven2_Release_3_3.20080421.163355"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Deliver maven3\" " + "\"Release_3_3\" " + "\"doe\" " + "\"John Doe\" "));

        UcmHistoryAction action = createUcmHistoryAction();
        List<ChangeLogSet.Entry> activities = action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = (UcmActivity) activities.get(0);
        assertEquals("Activity name is incorrect", "deliver.Release_3_3_jdk5.20080509.155359", activity.getName());
        assertEquals("Activity headline is incorrect", "Convert to Java 6", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "Release_3_3_jdk5", activity.getStream());
        assertEquals("Activity user is incorrect", "bob", activity.getUser());

        List<UcmActivity> subActivities = activity.getSubActivities();
        assertEquals("There should be 2 sub activities", 2, subActivities.size());
        assertEquals("Name of first sub activity is incorrect", "maven2_Release_3_3.20080421.154619", subActivities.get(0).getName());
        assertEquals("Name of second sub activity is incorrect", "maven2_Release_3_3.20080421.163355", subActivities.get(1).getName());

        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
        verify(cleartool).lsactivity(eq("deliver.Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull());
        verify(cleartool).lsactivity(eq("maven2_Release_3_3.20080421.154619"), (String) notNull(), (String) notNull());
        verify(cleartool).lsactivity(eq("maven2_Release_3_3.20080421.154619"), (String) notNull(), (String) notNull());
    }

    @Test
    public void assertParsingOfNonIntegrationActivity() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                        eq(Boolean.FALSE), eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20080509.140451\" " + "\"username\" " + "\"vobs/projects/Server//config-admin-client\" "
                                        + "\"/main/Product/Release_3_3_int/Release_3_3_jdk5/2\" " + "\"create directory version\" " + "\"checkin\"  "
                                        + "\"Release_3_3_jdk5.20080509.155359\" "));
        when(cleartool.lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull())).thenReturn(
                new StringReader("\"Convert to Java 6\" " + "\"Release_3_3_jdk5\" " + "\"bob\" "));

        UcmHistoryAction action = createUcmHistoryAction();
        List<ChangeLogSet.Entry> activities = action.getChanges(null, "IGNORED", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("There should be 1 activity", 1, activities.size());
        UcmActivity activity = (UcmActivity) activities.get(0);
        assertEquals("Activity name is incorrect", "Release_3_3_jdk5.20080509.155359", activity.getName());
        assertEquals("Activity headline is incorrect", "Convert to Java 6", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "Release_3_3_jdk5", activity.getStream());
        assertEquals("Activity user is incorrect", "bob", activity.getUser());
        verify(cleartool).lshistory(anyString(), (Date) isNull(), eq("IGNORED"), eq("Release_2_1_int"), eq(new String[] { "vobs/projects/Server" }),
                eq(Boolean.FALSE), eq(Boolean.FALSE));
        verify(cleartool).lsactivity(eq("Release_3_3_jdk5.20080509.155359"), (String) notNull(), (String) notNull());
    }

    @Test(expected = IOException.class)
    public void assertReaderIsClosed() throws Exception {
        final StringReader reader = new StringReader(
                "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" \"activity\" ");
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE))).thenReturn(reader).thenReturn(reader);

        UcmHistoryAction action = createUcmHistoryAction();
        action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        reader.ready();
    }

    @Test
    public void assertSeparateBranchCommands() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchone"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE))).thenReturn(new StringReader(""));
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchtwo"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE))).thenReturn(
                                new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" \"activity\" "));

        UcmHistoryAction action = createUcmHistoryAction();
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branchone", "branchtwo" }, new String[] { "vobpath" });

        assertTrue("The getChanges() method did not report a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchone"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchtwo"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                        eq(Boolean.FALSE)))
                        .thenReturn(
                                new StringReader(
                                        "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" \"activity\" "
                                                + "\"20071015.151822\" \"username\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" \"activity\" "));

        UcmHistoryAction action = createUcmHistoryAction();
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });

        assertTrue("The getChanges() method did not report a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE),
                eq(Boolean.FALSE));
    }

    @Bug(5342)
    @Test
    public void testUCMTrailingSlashesInLoadRules() throws Exception {
        AbstractClearCaseScm scm = new ClearCaseUcmSCMDummy("jcp_v13.1_be_int@\\june2008_recover", "\\be_rec\\config\\\r\n\\be_rec\\access\\\r\n"
                + "\\be_rec\\admins\\\r\n\\be_rec\\be\\\r\n\\be_rec\\buildservices\\\r\n" + "\\be_rec\\uf\\\r\n\\be_rec\\sef\\\r\n\\be_rec\\jwash\\",
                "stromp_be_builc", false, "M:\\", null, true, true, false, null, null, null, false, cleartool, clearCaseUcmScmDescriptor);
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(
                cleartool.lshistory((String) notNull(), (Date) isNull(), eq("stromp_be_builc"), eq("jcp_v13.1_be_int"), any(String[].class), anyBoolean(),
                        anyBoolean()))
                        .thenReturn(
                                new StringReader(
                                        "\"20100120.114845\" \"lmiguet\" "
                                                + "\"D:\\java\\hudson\\jobs\\stromp_be_test\\workspace\\stromp_be_builc\\be_rec\\be\\airshopper\\legacy\\src\\main\\java\\com\\amadeus\\ocg\\standard\\business\\farecommon\\entity\\PricingCommandOutput.java\" "
                                                + "\"\\main\\jcp_v13.1_be_int\\4\" \"create version\" \"checkin\" \"PTR3693254_WWW_AeRE_V131_INTCR_3313592-_Code_Review\" "));

        UcmHistoryAction action = new UcmHistoryAction(cleartool, false, scm.configureFilters(
                new VariableResolver.ByMap<String>(new HashMap<String, String>()), build, launcher), null, null, null, null);
        action.setExtendedViewPath("D:\\java\\hudson\\jobs\\stromp_be_test\\workspace\\stromp_be_builc\\");
        boolean hasChange = action.hasChanges(null, "stromp_be_builc", "viewTag", new String[] { "jcp_v13.1_be_int" }, scm.getViewPaths(null, null, launcher));
        assertTrue("The hasChanges() method did not report a change", hasChange);
    }

    private UcmHistoryAction createUcmHistoryAction() {
        return new UcmHistoryAction(cleartool, false, null, null, null, null, null);
    }

}
