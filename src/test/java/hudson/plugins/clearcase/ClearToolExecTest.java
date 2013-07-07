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
package hudson.plugins.clearcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.remoting.VirtualChannel;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.internal.matchers.And;

public class ClearToolExecTest extends AbstractWorkspaceTest {
    private ClearToolExec            clearToolExec;

    @Mock
    private ClearToolLauncher        ccLauncher;
    @Mock
    private TaskListener             listener;
    @Mock
    private Launcher                 launcher;
    @Mock
    private VariableResolver<String> resolver;
    @Mock
    private VirtualChannel           channel;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        clearToolExec = new ClearToolImpl(ccLauncher);
    }

    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testListViews() throws Exception {

        when(ccLauncher.run(eq(new String[] { "lsview" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log"), Boolean.TRUE));
        List<String> views = clearToolExec.lsview(false);
        assertEquals("The view list should contain 4 items", 4, views.size());
        assertEquals("The first view name is incorrect", "qaaaabbb_R3A_view", views.get(0));
        assertEquals("The second view name is incorrect", "qccccddd_view", views.get(1));
        assertEquals("The third view name is incorrect", "qeeefff_view", views.get(2));
        assertEquals("The fourth view name is incorrect", "qeeefff_HUDSON_SHORT_CS_TEST", views.get(3));
        verify(ccLauncher).run(eq(new String[] { "lsview" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testListActiveDynamicViews() throws Exception {
        when(ccLauncher.run(eq(new String[] { "lsview" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log"), Boolean.TRUE));

        List<String> views = clearToolExec.lsview(true);
        assertEquals("The view list should contain 1 item", 1, views.size());
        assertEquals("The third view name is incorrect", "qeeefff_view", views.get(0));
        verify(ccLauncher).run(eq(new String[] { "lsview" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testListVobs() throws Exception {
        when(ccLauncher.run(eq(new String[] { "lsvob" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log"), Boolean.TRUE));

        List<String> vobs = clearToolExec.lsvob(false);
        assertEquals("The vob list should contain 6 items", 6, vobs.size());
        assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
        assertEquals("The second vob name is incorrect", "pvoba", vobs.get(1));
        assertEquals("The third vob name is incorrect", "doc", vobs.get(2));
        assertEquals("The fourth vob name is incorrect", "demoa", vobs.get(3));
        assertEquals("The fifth vob name is incorrect", "pvob", vobs.get(4));
        assertEquals("The sixth vob name is incorrect", "bugvob", vobs.get(5));
        verify(ccLauncher).run(eq(new String[] { "lsvob" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testListVobsMounted() throws Exception {
        when(ccLauncher.run(eq(new String[] { "lsvob" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log"), Boolean.TRUE));
        List<String> vobs = clearToolExec.lsvob(true);
        assertEquals("The vob list should contain 3 items", 3, vobs.size());
        assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
        assertEquals("The second vob name is incorrect", "demoa", vobs.get(1));
        assertEquals("The third vob name is incorrect", "pvob", vobs.get(2));
        verify(ccLauncher).run(eq(new String[] { "lsvob" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testLshistory() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'+0000", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String formattedDate = formatter.format(mockedCalendar.getTime()).toLowerCase();

        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getLauncher()).thenReturn(new Launcher.LocalLauncher(null));
        when(
                ccLauncher.run(eq(new String[] { "lshistory", "-all", "-since", formattedDate, "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco", "vob1",
                        "vob2", "\"vob 3\"" }), any(InputStream.class), any(OutputStream.class), (FilePath) notNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log"), Boolean.TRUE));

        Reader reader = clearToolExec.lshistory("FORMAT", mockedCalendar.getTime(), "viewName", "branch", new String[] { "vob1", "vob2\n", "vob 3" }, false, false);
        assertNotNull("Returned console reader can not be null", reader);
        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run(
                eq(new String[] { "lshistory", "-all", "-since", formattedDate, "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco", "vob1", "vob2",
                        "\"vob 3\"" }), any(InputStream.class), any(OutputStream.class), (FilePath) notNull(), eq(true));
    }

    @Test
    public void testMkbl() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkbl", "-comment", "comment", "-incremental", "-view", "viewTag", "myBl" }), any(InputStream.class),
                        any(OutputStream.class), any(FilePath.class), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-mkbl-1.log"), Boolean.TRUE));
        List<Baseline> baselines = clearToolExec.mkbl("myBl", "viewTag", "comment", false, false, null, null, null);
        assertEquals(1, baselines.size());
        Baseline baseline = baselines.get(0);
        assertEquals("mybl", baseline.getBaselineName());
        assertEquals("mycomponent", baseline.getComponentName());
        verify(ccLauncher).run(eq(new String[] { "mkbl", "-comment", "comment", "-incremental", "-view", "viewTag", "myBl" }), any(InputStream.class),
                any(OutputStream.class), any(FilePath.class), eq(true));
    }

    @Test
    public void testLsHistoryBranchNotFound() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'+0000", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String formattedDate = formatter.format(mockedCalendar.getTime()).toLowerCase();

        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getLauncher()).thenReturn(new Launcher.LocalLauncher(null));
        when(
                ccLauncher.run(eq(new String[] { "lshistory", "-all", "-since", formattedDate, "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco", "vob1",
                        "vob2", "\"vob 3\"" }), any(InputStream.class), any(OutputStream.class), (FilePath) notNull(), eq(true))).thenThrow(new IOException());

        Reader reader = clearToolExec.lshistory("FORMAT", mockedCalendar.getTime(), "viewName", "branch", new String[] { "vob1", "vob2\n", "vob 3" }, false, false);
        assertNotNull("Returned console reader cannot be null", reader);
        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run(
                eq(new String[] { "lshistory", "-all", "-since", formattedDate, "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco", "vob1", "vob2",
                        "\"vob 3\"" }), any(InputStream.class), any(OutputStream.class), (FilePath) notNull(), eq(true));
    }

    @Test
    public void testCatConfigSpec() throws Exception {

        when(ccLauncher.run(eq(new String[] { "catcs", "-tag", "viewname" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true)))
                .thenAnswer(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-catcs-1.log"), Boolean.TRUE));

        String configSpec = clearToolExec.catcs("viewname");
        assertEquals("The config spec was not correct",
                "element * CHECKEDOUT\nelement * ...\\rel2_bugfix\\LATEST\nelement * \\main\\LATEST -mkbranch rel2_bugfix", configSpec);
        verify(ccLauncher)
                .run(eq(new String[] { "catcs", "-tag", "viewname" }), any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testRemoveView() throws Exception {

        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(
                ccLauncher.run(eq(new String[] { "rmview", "-force", "viewName" }), (InputStream) isNull(), (OutputStream) notNull(), (FilePath) notNull(),
                        eq(true))).thenReturn(Boolean.TRUE);

        clearToolExec.rmview("viewName");

        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run(eq(new String[] { "rmview", "-force", "viewName" }), (InputStream) isNull(), (OutputStream) notNull(), (FilePath) notNull(),
                eq(true));
    }

    @Test
    public void testForcedRemoveView() throws Exception {
        workspace.child("viewName").mkdirs();
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(
                ccLauncher.run(eq(new String[] { "rmview", "-force", "viewName" }), (InputStream) isNull(), (OutputStream) notNull(), (FilePath) notNull(),
                        eq(true))).thenReturn(Boolean.TRUE);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(new PrintStream(new ByteArrayOutputStream()));

        clearToolExec.rmview("viewName");

        assertFalse("View folder still exists", workspace.child("viewName").exists());
        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run(eq(new String[] { "rmview", "-force", "viewName" }), (InputStream) isNull(), (OutputStream) notNull(), (FilePath) notNull(),
                eq(true));
        verify(ccLauncher).getListener();
        verify(listener).getLogger();
    }

    @Test
    public void testRmTag() throws Exception {
        clearToolExec.rmtag("myViewTag");

        verify(ccLauncher).run(eq(new String[] { "rmtag", "-view", "myViewTag" }), any(InputStream.class), any(OutputStream.class), any(FilePath.class),
                eq(true));
    }

    @Test
    public void testUpdate() throws Exception {
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        ArrayThatStartsWith<String> runArgumentsMatcher = new ArrayThatStartsWith<String>(new String[] { "update", "-force", "-overwrite" });
        when(ccLauncher.run(argThat(runArgumentsMatcher), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false))).thenReturn(
                Boolean.TRUE);

        clearToolExec.update2("viewName", null);

        verify(ccLauncher).getWorkspace();

        verify(ccLauncher).run(argThat(runArgumentsMatcher), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testSetcsCurrent() throws Exception {
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(ccLauncher.getChannel()).thenReturn(channel);
        when(listener.getLogger()).thenReturn(System.out);
        when(ccLauncher.run(eq(new String[] { "setcs", "-current" }), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(true)))
                .thenReturn(Boolean.TRUE);

        clearToolExec.setcs2("viewName", SetcsOption.CURRENT, null);
    }

    @Test
    public void testSetcs() throws Exception {
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getLauncher()).thenReturn(launcher);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        when(
                ccLauncher.run(argThat(new ArrayThatStartsWith<String>(new String[] { "setcs", "-tag", "viewTag" })), (InputStream) notNull(),
                        (OutputStream) notNull(), (FilePath) isNull(), eq(false))).thenReturn(Boolean.TRUE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);

        clearToolExec.setcsTag("viewTag", SetcsOption.CONFIGSPEC, "configspec");

        verify(ccLauncher).run(argThat(new ArrayThatStartsWith<String>(new String[] { "setcs", "-tag", "viewTag" })), (InputStream) notNull(),
                (OutputStream) notNull(), (FilePath) isNull(), eq(false));
    }

    @Test(expected = IOException.class)
    public void testSetcsCurrentBlocked() throws Exception {
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        when(ccLauncher.run(eq(new String[] { "setcs", "-current" }), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false)))
                .thenAnswer(new StreamCopyAction(2, this.getClass().getResourceAsStream("ct-update-2.log"), Boolean.TRUE));

        clearToolExec.setcs2("viewName", SetcsOption.CURRENT, null);

        verify(ccLauncher).run(eq(new String[] { "setcs", "-current" }), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testUpdateWithLoadRulesWindows() throws Exception {
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(ccLauncher.getLauncher()).thenReturn(launcher);
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        ArrayThatStartsWith<String> startsWith = new ArrayThatStartsWith<String>(new String[] { "update", "-force", "-overwrite"});
        ArrayThatEndsWith<String> endsWith = new ArrayThatEndsWith<String>(new String[] { "-add_loadrules", "more_load_rules" });
        List<Matcher> asList = Arrays.asList((Matcher) startsWith, (Matcher) endsWith);
        And argumentsMatcher = new And(asList);
        when(ccLauncher.run((String[]) argThat(argumentsMatcher), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false)))
                .thenReturn(Boolean.TRUE);

        clearToolExec.update2("viewName", new String[] { "\\more_load_rules" });
        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run((String[]) argThat(argumentsMatcher), (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testUpdateWithLoadRules() throws Exception {
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(ccLauncher.getLauncher()).thenReturn(launcher);
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        ArrayThatStartsWith<String> startsWith = new ArrayThatStartsWith<String>(new String[] { "update", "-force", "-overwrite"});
        ArrayThatEndsWith<String> endsWith = new ArrayThatEndsWith<String>(new String[] { "-add_loadrules", "more_load_rules" });
        List<Matcher> asList = Arrays.asList((Matcher)startsWith, (Matcher)endsWith);
        And argumentsMatcher = new And(asList);
        when(
                ccLauncher.run((String [])argThat(argumentsMatcher),
                        (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false))).thenReturn(Boolean.TRUE);

        clearToolExec.update2("viewName", new String[] { "/more_load_rules" });

        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run((String [])argThat(argumentsMatcher),
                (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testUpdateWithLoadRulesWithSpace() throws Exception {

        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(ccLauncher.getLauncher()).thenReturn(launcher);
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        ArrayThatStartsWith<String> startsWith = new ArrayThatStartsWith<String>(new String[] { "update", "-force", "-overwrite"});
        ArrayThatEndsWith<String> endsWith = new ArrayThatEndsWith<String>(new String[] { "-add_loadrules", "\"more load_rules\"" });
        List<Matcher> asList = Arrays.asList((Matcher)startsWith, (Matcher)endsWith);
        And argumentsMatcher = new And(asList);
        when(
                ccLauncher.run((String[]) argThat(argumentsMatcher),
                        (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false))).thenReturn(Boolean.TRUE);

        clearToolExec.update2("viewName", new String[] { "/more load_rules" });

        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run((String[]) argThat(argumentsMatcher),
                (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testUpdateWithLoadRulesWithSpaceWin() throws Exception {
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(ccLauncher.getLauncher()).thenReturn(launcher);
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        ArrayThatStartsWith<String> startsWith = new ArrayThatStartsWith<String>(new String[] { "update", "-force", "-overwrite"});
        ArrayThatEndsWith<String> endsWith = new ArrayThatEndsWith<String>(new String[] { "-add_loadrules", "\"more load_rules\"" });
        List<Matcher> asList = Arrays.asList((Matcher)startsWith, (Matcher)endsWith);
        And argumentsMatcher = new And(asList);
        when(
                ccLauncher.run((String[]) argThat(argumentsMatcher),
                        (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false))).thenReturn(Boolean.TRUE);

        clearToolExec.update2("viewName", new String[] { "\\more load_rules" });

        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run((String[]) argThat(argumentsMatcher),
                (InputStream) notNull(), (OutputStream) notNull(), (FilePath) notNull(), eq(false));
    }

    @Test
    public void testCreateView() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "viewpath" }), (InputStream) isNull(), (OutputStream) isNull(),
                        (FilePath) isNull(), eq(true))).thenReturn(Boolean.TRUE);

        MkViewParameters p = new MkViewParameters();
        p.setViewPath("viewpath");
        p.setViewTag("viewName");
        clearToolExec.mkview(p);

        verify(ccLauncher).run(eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "-stgloc", "-auto", "viewpath" }), (InputStream) isNull(),
                (OutputStream) isNull(), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testCreateViewWithStream() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", "-tag", "viewName", "viewpath" }), (InputStream) isNull(),
                        (OutputStream) isNull(), (FilePath) isNull(), eq(true))).thenReturn(Boolean.TRUE);

        MkViewParameters p = new MkViewParameters();
        p.setViewPath("viewpath");
        p.setViewTag("viewName");
        p.setStreamSelector("streamSelector");
        clearToolExec.mkview(p);

        verify(ccLauncher).run(eq(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", "-tag", "viewName", "-stgloc", "-auto", "viewpath" }),
                (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testCreateViewExtraParams() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "-anextraparam", "-anotherparam", "viewpath" }),
                        (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true))).thenReturn(Boolean.TRUE);

        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher, "-anextraparam -anotherparam");
        MkViewParameters p = new MkViewParameters();
        p.setViewPath("viewpath");
        p.setViewTag("viewName");
        p.setAdditionalParameters("-anextraparam -anotherparam");
        clearToolExec.mkview(p);

        verify(ccLauncher).run(
                eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "-anextraparam", "-anotherparam", "-stgloc", "-auto", "viewpath" }),
                (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testCreateUcmViewWithOptionalParams() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", "-tag", "viewName", "-anextraparam", "-anotherparam",
                        "viewpath" }), (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true))).thenReturn(Boolean.TRUE);

        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher, "-anextraparam -anotherparam");
        MkViewParameters p = new MkViewParameters();
        p.setViewPath("viewpath");
        p.setViewTag("viewName");
        p.setStreamSelector("streamSelector");
        p.setAdditionalParameters("-anextraparam -anotherparam");
        clearToolExec.mkview(p);

        verify(ccLauncher).run(
                eq(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", "-tag", "viewName", "-anextraparam", "-anotherparam", "-stgloc", "-auto",
                        "viewpath" }), (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testCreateViewExtraParamsEvaluated() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "-anextraparam", "Test", "viewpath" }), (InputStream) isNull(),
                        (OutputStream) isNull(), (FilePath) isNull(), eq(true))).thenReturn(Boolean.TRUE);
        when(resolver.resolve("COMPUTERNAME")).thenReturn("Test");

        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher, "-anextraparam $COMPUTERNAME");
        MkViewParameters p = new MkViewParameters();
        p.setViewPath("viewpath");
        p.setViewTag("viewName");
        p.setAdditionalParameters("-anextraparam $COMPUTERNAME");
        clearToolExec.mkview(p);

        verify(ccLauncher).run(eq(new String[] { "mkview", "-snapshot", "-tag", "viewName", "-anextraparam", "Test", "-stgloc", "-auto", "viewpath" }),
                (InputStream) isNull(), (OutputStream) isNull(), (FilePath) isNull(), eq(true));
        verify(resolver, atLeastOnce()).resolve("COMPUTERNAME");
    }

    @Test
    public void testDescribe() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "desc", "-fmt", "format", "stream:stream_selector@\\a_vob" }), any(InputStream.class),
                        any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-desc-1.log"), Boolean.TRUE));

        Reader reader = clearToolExec.describe("format", null, "stream:stream_selector@\\a_vob");
        assertNotNull("Returned console reader cannot be null", reader);
        verify(ccLauncher).run(eq(new String[] { "desc", "-fmt", "format", "stream:stream_selector@\\a_vob" }), any(InputStream.class),
                any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void testDescribeObjectSelectorWithSpaces() throws Exception {
        when(
                ccLauncher.run(eq(new String[] { "desc", "-fmt", "format",
                        "D:\\slave-ci\\workspace\\jobname\\view\\vob1\\component\\path@@\\main\branch\\67\\A path with spaces.p12\\main\\branch\\1" }),
                        any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-desc-1.log"), Boolean.TRUE));

        Reader reader = clearToolExec.describe("format", null,
                "D:\\slave-ci\\workspace\\jobname\\view\\vob1\\component\\path@@\\main\branch\\67\\A path with spaces.p12\\main\\branch\\1");
        assertNotNull("Returned console reader cannot be null", reader);
        verify(ccLauncher).run(
                eq(new String[] { "desc", "-fmt", "format",
                        "D:\\slave-ci\\workspace\\jobname\\view\\vob1\\component\\path@@\\main\branch\\67\\A path with spaces.p12\\main\\branch\\1" }),
                any(InputStream.class), any(OutputStream.class), (FilePath) isNull(), eq(true));
    }

    @Test
    public void assertLsactivityReturnsReader() throws Exception {
        workspace.child("viewName").mkdirs();
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(
                ccLauncher.run(eq(new String[] { "lsactivity", "-fmt", "ACTIVITY_FORMAT", "ACTIVITY@VOB" }), any(InputStream.class), any(OutputStream.class),
                        any(FilePath.class), eq(true))).thenAnswer(
                new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsactivity-1.log"), Boolean.TRUE));

        Reader reader = clearToolExec.lsactivity("ACTIVITY@VOB", "ACTIVITY_FORMAT", "VIEW_NAME");
        assertNotNull("Returned console reader can not be null", reader);
        verify(ccLauncher).getWorkspace();
        verify(ccLauncher).run(eq(new String[] { "lsactivity", "-fmt", "ACTIVITY_FORMAT", "ACTIVITY@VOB" }), any(InputStream.class), any(OutputStream.class),
                any(FilePath.class), eq(true));
    }

    @Test
    public void testStartview() throws Exception {
        clearToolExec.startView("viewName");
        verify(ccLauncher).run(argThat(new ArrayThatStartsWith<String>(new String[] { "startview", "viewName" })), (InputStream) isNull(),
                (OutputStream) isNull(), (FilePath) isNull(), eq(true));
    }

    /**
     * Make sure that if we call setcs with a null or empty string for the config spec, we get a call to cleartool setcs -current.
     */
    @Test
    public void testSetcsTag() throws Exception {
        when(ccLauncher.getWorkspace()).thenReturn(workspace);
        when(ccLauncher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        when(
                ccLauncher.run(argThat(new ArrayThatStartsWith<String>(new String[] { "setcs", "-tag", "viewName", "-current" })), any(InputStream.class),
                        any(OutputStream.class), any(FilePath.class), eq(false))).thenReturn(Boolean.TRUE);

        clearToolExec.setcsTag("viewName", SetcsOption.CURRENT, null);
        verify(ccLauncher).run(argThat(new ArrayThatStartsWith<String>(new String[] { "setcs", "-tag", "viewName", "-current" })), any(InputStream.class),
                any(OutputStream.class), any(FilePath.class), eq(false));
    }

    /**
     * Simple impl of ClearToolExec to help testing the methods in the class
     */
    private static class ClearToolImpl extends ClearToolExec {

        public ClearToolImpl(ClearToolLauncher launcher) {
            super(null, launcher, null);
        }

        @Override
        protected FilePath getRootViewPath(ClearToolLauncher launcher) {
            return launcher.getWorkspace();
        }
    }
}
