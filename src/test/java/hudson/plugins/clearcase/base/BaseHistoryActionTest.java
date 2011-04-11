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
package hudson.plugins.clearcase.base;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.clearcase.AbstractClearCaseScm;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;
import hudson.plugins.clearcase.ClearCaseSCM;
import hudson.plugins.clearcase.ClearCaseSCMDummy;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.history.LabelFilter;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Node.class)
public class BaseHistoryActionTest extends AbstractWorkspaceTest {

    private static final String                 VALID_HISTORY_FORMAT = "\\\"%Nd\\\" \\\"%u\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%e\\\" \\\"%o\\\" \\n%c\\n";
    @Mock
    private AbstractProject                     project;
    @Mock
    private Build                               build;
    @Mock
    private Launcher                            launcher;
    @Mock
    private ClearToolLauncher                   clearToolLauncher;
    @Mock
    private ClearCaseSCM.ClearCaseScmDescriptor clearCaseScmDescriptor;
    private Node                                node;
    @Mock
    private Computer                            computer;

    @Mock
    private ClearTool                           cleartool;

    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070906.091701\"   \"egsperi\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"destroy sub-branch \"esmalling_branch\" of branch\"   \"mkelem\"\n"));
        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, new DestroySubBranchFilter(), 10000);
        List<ChangeLogSet.Entry> changes = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("The event record should be ignored", 0, changes.size());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    /*
     * Below is taken from DefaultPollAction
     */

    @Test
    public void assertExcludedRegionsAreIgnored() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat()).thenReturn(
                new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" "));

        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());
        filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, "Customer"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, new FilterChain(filters), 10000);
        List<ChangeLogSet.Entry> changes = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("The event record should be ignored", 0, changes.size());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    @Test
    public void assertFormatContainsComment() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat()).thenReturn(new StringReader(""));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 0);
        action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    @Test
    public void assertIgnoringDestroySubBranchEvent() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput())
                .thenReturn(
                        new StringReader(
                                "\"20080326.110739\" \"user\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\""));
        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, new DestroySubBranchFilter(), 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertFalse("The getChanges() method reported a change", hasChange);
        verifyCleartoolLsHistoryWithStandardInput();
    }

    @Test
    public void assertIgnoringErrors() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput()).thenReturn(new StringReader("cleartool: Error: Not an object in a vob: \"view.dat\".\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, new DefaultFilter(), 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });

        assertFalse("The getChanges() method reported a change", hasChange);
        verifyCleartoolLsHistoryWithStandardInput();
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput()).thenReturn(
                new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" "));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, new DefaultFilter(), 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });

        assertFalse("The getChanges() method reported a change", hasChange);
        verifyCleartoolLsHistoryWithStandardInput();
    }

    @Test
    public void assertMergedLogEntries() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat()).thenReturn(
                new StringReader("\"20070906.091701\"   \"egsperi\"  \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"create version\"   \"mkelem\"\n"
                        + "\"20070906.091705\"   \"egsperi\"  \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"   \"create version\"  \"mkelem\"\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        List<ChangeLogSet.Entry> changes = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Two entries should be merged into one", 1, changes.size());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    @Test
    public void assertNotIgnoringDestroySubBranchEvent() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput())
                .thenReturn(
                        new StringReader(
                                "\"20080326.110739\" \"user\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\""));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertTrue("The getChanges() method reported a change", hasChange);
        verifyCleartoolLsHistoryWithStandardInput();
    }

    @Test(expected = IOException.class)
    public void assertReaderIsClosed() throws Exception {
        final StringReader reader = new StringReader(
                "\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" ");
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput()).thenReturn(reader);

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 0);
        action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        reader.ready();
    }

    @Test(expected = IOException.class)
    public void assertReaderIsClosed2() throws Exception {
        final StringReader reader = new StringReader(
                "\"20070906.091701\"   \"egsperi\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"create version\"  \"mkelem\"\n");

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat()).thenReturn(reader);

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" }, new String[] { "vobs/projects/Server" });
        reader.ready();
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    /*
     * Below is taken from BaseChangelogAction
     */

    @Test
    public void assertSeparateBranchCommands() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchone"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE)))
                .thenReturn(new StringReader(""));
        when(cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchtwo"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE)))
                .thenReturn(new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" "));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branchone", "branchtwo" }, new String[] { "vobpath" });

        assertTrue("The getChanges() method did not report a change", hasChange);
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchone"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE));
        verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branchtwo"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE));
    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithStandardInput()).thenReturn(
                new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" "
                        + "\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" "));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 0);
        boolean hasChange = action.hasChanges(null, "view", "viewTag", new String[] { "branch" }, new String[] { "vobpath" });
        assertTrue("The getChanges() method did not report a change", hasChange);
        verifyCleartoolLsHistoryWithStandardInput();
    }

    @Test
    public void assertViewPathIsRemovedFromFilePaths() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat()).thenReturn(
                new StringReader(
                        "\"20070827.085901\" \"user\" \"/view/ralef_0.2_nightly/vobs/Tools/framework/util/QT.h\" \"/main/comain\" \"action\"   \"mkelem\"\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        action.setExtendedViewPath("/view/ralef_0.2_nightly");
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File path is incorrect", "/vobs/Tools/framework/util/QT.h", entry.getElements().get(0).getFile());

        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    private Reader cleartoolLsHistoryWithStandardInput() throws IOException, InterruptedException {
        return cleartool.lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE));
    }

    private Reader cleartoolLsHistoryWithValidHistoryFormat() throws IOException, InterruptedException {
        return cleartool.lshistory(eq(VALID_HISTORY_FORMAT), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.FALSE));
    }

    private Reader cleartoolLsHistoryWithAnyHistoryFormat() throws IOException, InterruptedException {
        return cleartool.lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.FALSE));
    }

    private Reader cleartoolLsHistoryWithAnyHistoryFormatAndMinorEvents() throws IOException, InterruptedException {
        return cleartool.lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.TRUE));
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

    @Before
    public void setUp() throws Exception {
        node = PowerMockito.mock(Node.class);
    }

    /**
     * Very similar to above - but here, the upper-case is in the path to the workspace. Also, we're verifying that an lshistory item for a path *not* specified
     * in the load rules doesn't get included in the changelog.
     */
    @Bug(4430)
    @Test
    public void testCaseSensitivityInExtendedViewPath() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(project.getName()).thenReturn("Issue4430");
        when(clearCaseScmDescriptor.getLogMergeTimeWindow()).thenReturn(Integer.valueOf(5));
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(cleartool.doesViewExist("sa-seso-tempusr4__refact_structure__sot")).thenReturn(Boolean.TRUE);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(cleartool.pwv(anyString())).thenReturn("D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot");
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20090909.124752\" \"erustt\" "
                                        + "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\apps\\esa\\ecl\\sot\\sot_impl\\src\\main\\java\\com\\ascom\\ecs3\\ecl\\sot\\nodeoperationstate\\OperationStateManagerImpl.java\" "
                                        + "\"\\main\\refact_structure\\2\" \"create version\" \"checkin\"\n\n"
                                        + "\"20090909.105713\" \"eveter\" "
                                        + "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\apps\\confcmdnet\\ecl\\confcmdnet_webapp\\doc\" "
                                        + "\"\\main\\refact_structure\\13\" \"create directory version\" \"checkin\"\nUncataloged file element \"ConfCmdNet_PendenzenListe.xlsx\".\n"
                                        + "\"20090909.091004\" \"eruegr\" "
                                        + "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\components\\ecc_dal\\dal_impl_hibernate\\src\\main\\java\\com\\ascom\\ecs3\\ecc\\dal\\impl\\hibernate\\ctrl\\SotButtonController.java\" "
                                        + "\"\\main\\refact_structure\\16\" \"create version\" \"checkin\"\n\n"));

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("refact_structure", "", "configspec", "sa-seso-tempusr4__refact_structure__sot", true,
                "load \\ecs3cop\\projects\\buildconfigurations\n" + "load \\ecs3cop\\projects\\apps\\esa\n" + "load \\ecs3cop\\projects\\apps\\tmp\n"
                        + "load \\ecs3cop\\projects\\components\n" + "load \\ecs3cop\\projects\\test\n", false, "", "", false, false, false, "", "", false,
                false, cleartool, clearCaseScmDescriptor);

        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);

        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), scm.getViewPath(variableResolver), scm.generateNormalizedViewName(variableResolver),
                scm.getBranchNames(variableResolver), scm.getViewPaths(null, null, launcher));
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals(
                "File path is incorrect",
                "ecs3cop\\projects\\apps\\esa\\ecl\\sot\\sot_impl\\src\\main\\java\\com\\ascom\\ecs3\\ecl\\sot\\nodeoperationstate\\OperationStateManagerImpl.java",
                entry.getElements().get(0).getFile());
    }

    /**
     * Bug was that we had (pre-1.0) been converting extended view path to lower case whenever we used it or compared against it. I believe this was done
     * because the view drive was manually specified, and so on Windows, the configured value could be, say, m:\ or M:\ and either would be valid. In that
     * context, normalizing to lower-case meant we wouldn't have to worry about how view drive was specified, case-wise. But with 1.0 and later, we're actually
     * getting extended view path directly from cleartool pwv, and it got changed in some places to no longer do toLowerCase() before comparisons, while the
     * setter was still converting to lower-case, which caused any path in a view with upper-case to be rejected by the filters.
     * 
     * Now, we never call toLowerCase() on the extended view path, at any point, since it's just going to be the output of pwv, which will have consistent case
     * usage regardless of what we do.
     */
    @Bug(3666)
    @Test
    public void testCaseSensitivityInViewName() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(project.getName()).thenReturn("Issue3666");
        when(clearCaseScmDescriptor.getLogMergeTimeWindow()).thenReturn(Integer.valueOf(5));
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(cleartool.doesViewExist("Hudson.SAP.ICI.7.6.Quick")).thenReturn(Boolean.TRUE);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(cleartool.pwv(anyString())).thenReturn("Y:\\Hudson.SAP.ICI.7.6.Quick");
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20090909.151109\" \"nugarov\" "
                                        + "\"Y:\\Hudson.SAP.ICI.7.6.Quick\\sapiciadapter\\Tools\\gplus_tt\\gplus_tt_config.py\" \"\\main\\dev-kiev-7.6\\10\" \"create version\" \"checkin\"\nvolatile"));

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("", "", "configspec", "Hudson.SAP.ICI.7.6.Quick", false, "load /sapiciadapter", true, "Y:\\", "", false,
                false, false, "", "", false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), scm.getViewPath(variableResolver), scm.generateNormalizedViewName(variableResolver),
                scm.getBranchNames(variableResolver), scm.getViewPaths(null, null, launcher));
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File path is incorrect", "sapiciadapter\\Tools\\gplus_tt\\gplus_tt_config.py", entry.getElements().get(0).getFile());
    }

    @Test
    public void testCommentWithEmptyLine() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\" \"create directory version\"   \"mkelem\"\ntext\n\nend of comment"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });

        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("Comment is incorrect", "text\n\nend of comment", entry.getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testEmptyComment() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\" \"create directory version\" \"mkelem\"\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });

        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("Comment is incorrect", "", entry.getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testErrorOutput() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070830.084801\"   \"inttest3\"  \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n"
                                        + "cleartool: Error: Branch type not found: \"sit_r6a\".\n"
                                        + "\"20070829.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        assertEquals("First entry is incorrect", "", ((ClearCaseChangeLogEntry) entries.get(0)).getComment());
        assertEquals("Second entry is incorrect", "", ((ClearCaseChangeLogEntry) entries.get(1)).getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    /**
     * Make sure changes are detected upon placing/removing specified label.
     */
    @Bug(7218)
    @Test
    public void testLabelChangesFound() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);

        when(cleartoolLsHistoryWithAnyHistoryFormatAndMinorEvents()).thenReturn(
                new InputStreamReader(AbstractClearCaseScm.class.getResourceAsStream("ct-lshistory-label-1.log")));

        Filter labelFilter = new LabelFilter("USER1_TEST");
        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, labelFilter, 1000);
        assertTrue("Label changes are not detected.", action.hasChanges(new Date(), "viewPath", "viewTag", new String[0], new String[] { "vob1" }));

        verify(cleartool).lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.TRUE));
    }

    @Test
    public void testLabelFilteringIgnoresOtherOperations() throws Exception {

        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(project.getName()).thenReturn("labelTest");
        when(clearCaseScmDescriptor.getLogMergeTimeWindow()).thenReturn(Integer.valueOf(5));
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(cleartool.doesViewExist("someview")).thenReturn(Boolean.TRUE);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(cleartool.pwv(anyString())).thenReturn("D:\\hudson\\jobs\\somejob\\workspace\\someview");
        when(cleartoolLsHistoryWithAnyHistoryFormatAndMinorEvents()).thenReturn(
                new InputStreamReader(AbstractClearCaseScm.class.getResourceAsStream("ct-lshistory-label-1.log")));

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("rel10.4_int", "USER1_TEST", "configspec", "someview", true, "load /vob1\n", false, "", "", false, false,
                false, "", "", false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);

        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), scm.getViewPath(variableResolver),
                scm.generateNormalizedViewName((BuildVariableResolver) variableResolver), scm.getBranchNames(variableResolver),
                scm.getViewPaths(null, null, launcher));
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        for (Entry entry : entries) {

            assertTrue("Incorrect history entry detected.", ((ClearCaseChangeLogEntry) entry).getComment().startsWith("Moved label \"USER1_TEST\""));
        }
    }

    /**
     * Make sure that label names are properly filtered.
     * 
     * The same polling log is analyzed twice, once for label that exists, once for label that doesn't. Only in the first case should changes be detected.
     */
    @Bug(7218)
    @Test
    public void testLabelNameFiltering() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormatAndMinorEvents()).thenReturn(
                new InputStreamReader(AbstractClearCaseScm.class.getResourceAsStream("ct-lshistory-label-2.log")),
                new InputStreamReader(AbstractClearCaseScm.class.getResourceAsStream("ct-lshistory-label-2.log")));

        Filter labelFilter = new LabelFilter("LABEL7344");
        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, labelFilter, 1000);
        assertTrue("Filtered label names were not found.", action.hasChanges(new Date(), "viewPath", "viewTag", new String[0], new String[] { "vob1" }));

        labelFilter = new LabelFilter("USER1_TEST");
        action = new BaseHistoryAction(cleartool, false, labelFilter, 1000);
        assertFalse("Changes were found for non-existing label.", action.hasChanges(new Date(), "viewPath", "viewTag", new String[0], new String[] { "vob1" }));
        verify(cleartool, times(2)).lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.TRUE));
    }

    @Test
    public void testMultiline() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070830.084801\"   \"inttest2\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n"
                                        + "\"20070830.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        List<ChangeLogSet.Entry> changes = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 2, changes.size());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    @Test
    public void testOperation() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"create directory version\"  \"mkelem\"\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        FileElement element = ((ClearCaseChangeLogEntry) entries.get(0)).getElements().get(0);
        assertEquals("Status is incorrect", "mkelem", element.getOperation());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    @Test
    public void testParseNoComment() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070827.084801\" \"inttest14\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\" \"mkelem\"\n\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });

        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File is incorrect", "Source\\Definitions\\Definitions.csproj", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "inttest14", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 48, 1), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\1", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "", entry.getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testParseWithComment() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070827.085901\"   \"aname\"   \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\" \"create version\"   \"mkelem\"\nBUG8949"));
        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949", entry.getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testParseWithLongAction() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat()).thenReturn(
                new StringReader(
                        "\"20070827.085901\"   \"aname\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\" \"create a version\"  \"mkelem\"\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("Action is incorrect", "create a version", entry.getElements().get(0).getAction());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testParseWithTwoLineComment() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070827.085901\"   \"aname\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"   \"create version\"   \"mkelem\"\nBUG8949\nThis fixed the problem"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });

        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949\nThis fixed the problem", entry.getComment());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    @Test
    public void testSorted() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithValidHistoryFormat())
                .thenReturn(
                        new StringReader(
                                "\"20070827.084801\"   \"inttest2\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n"
                                        + "\"20070825.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n"
                                        + "\"20070830.084801\"   \"inttest1\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 10000);
        List<ChangeLogSet.Entry> changes = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 3, changes.size());
        assertEquals("First entry is incorrect", "inttest1", ((ClearCaseChangeLogEntry) changes.get(0)).getUser());
        assertEquals("First entry is incorrect", "inttest2", ((ClearCaseChangeLogEntry) changes.get(1)).getUser());
        assertEquals("First entry is incorrect", "inttest3", ((ClearCaseChangeLogEntry) changes.get(2)).getUser());
        verifyCleartoolLsHistoryWithValidHistoryFormat();
    }

    /**
     * Making sure that load rules using "/" are handled properly on Windows.
     */
    @Bug(4781)
    @Test
    public void testUnixSlashesInWindowsLoadRules() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(project.getName()).thenReturn("Issue4781");
        when(clearCaseScmDescriptor.getLogMergeTimeWindow()).thenReturn(Integer.valueOf(5));
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(cleartool.doesViewExist("someview")).thenReturn(Boolean.TRUE);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(cleartool.pwv(anyString())).thenReturn("D:\\hudson\\jobs\\somejob\\workspace\\someview");
        when(cleartoolLsHistoryWithAnyHistoryFormat()).thenReturn(
                new StringReader("\"20090909.124752\" \"erustt\" " + "\"D:\\hudson\\jobs\\somejob\\workspace\\someview\\some_vob\\path\\to\\file.java\" "
                        + "\"\\main\\some_branch\\2\" \"create version\" \"checkin\"\n\n" + "\"20090909.091004\" \"eruegr\" "
                        + "\"D:\\hudson\\jobs\\somejob\\workspace\\someview\\some_vob\\another\\path\\to\\anotherFile.java\" "
                        + "\"\\main\\some_branch\\16\" \"create version\" \"checkin\"\n\n"));

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("some_branch", "", "configspec", "someview", true, "load /some_vob/path\n" + "load /some_vob/another\n",
                false, "", "", false, false, false, "", "", false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);

        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), scm.getViewPath(variableResolver),
                scm.generateNormalizedViewName((BuildVariableResolver) variableResolver), scm.getBranchNames(variableResolver),
                scm.getViewPaths(null, null, launcher));
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        ClearCaseChangeLogEntry entry = (ClearCaseChangeLogEntry) entries.get(0);
        assertEquals("File path is incorrect", "some_vob\\path\\to\\file.java", entry.getElements().get(0).getFile());
    }

    @Test
    public void testUserOutput() throws Exception {

        when(cleartool.doesViewExist("viewTag")).thenReturn(Boolean.TRUE);
        when(cleartoolLsHistoryWithAnyHistoryFormat()).thenReturn(new InputStreamReader(AbstractClearCaseScm.class.getResourceAsStream("ct-lshistory-1.log")));

        BaseHistoryAction action = new BaseHistoryAction(cleartool, false, null, 1000);
        List<ChangeLogSet.Entry> entries = action.getChanges(new Date(), "viewPath", "viewTag", new String[] { "Release_2_1_int" },
                new String[] { "vobs/projects/Server" });
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        verifyCleartoolLsHistoryWithAnyHistoryFormat();
    }

    private Reader verifyCleartoolLsHistoryWithStandardInput() throws IOException, InterruptedException {
        return verify(cleartool).lshistory((String) notNull(), (Date) isNull(), eq("view"), eq("branch"), eq(new String[] { "vobpath" }), eq(Boolean.FALSE));
    }

    private Reader verifyCleartoolLsHistoryWithValidHistoryFormat() throws IOException, InterruptedException {
        return verify(cleartool).lshistory(eq(VALID_HISTORY_FORMAT), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.FALSE));
    }

    private Reader verifyCleartoolLsHistoryWithAnyHistoryFormat() throws IOException, InterruptedException {
        return verify(cleartool).lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.FALSE));
    }

    private Reader verifyCleartoolLsHistoryWithAnyHistoryFormatAndMinorEvents() throws IOException, InterruptedException {
        return verify(cleartool).lshistory(anyString(), any(Date.class), anyString(), anyString(), any(String[].class), eq(Boolean.TRUE));
    }
}
