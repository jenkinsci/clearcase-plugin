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

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearToolExecTest extends AbstractWorkspaceTest {
    private Mockery context;
    private Mockery classContext;
    private ClearToolExec clearToolExec;
    private ClearToolLauncher ccLauncher;
    private TaskListener listener;
    private Launcher launcher;
    private VariableResolver<String> resolver;
    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new JUnit4Mockery();
        classContext = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        ccLauncher = context.mock(ClearToolLauncher.class);
        clearToolExec = new ClearToolImpl(ccLauncher);
        launcher = classContext.mock(Launcher.class);
        listener = context.mock(TaskListener.class);
        resolver = context.mock(VariableResolver.class);
    }
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testListViews() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "lsview" })),
                                      (InputStream) with(anything()), (OutputStream) with(an(OutputStream.class)),
                                      with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        List<String> views = clearToolExec.lsview(false);
        assertEquals("The view list should contain 4 items", 4, views.size());
        assertEquals("The first view name is incorrect", "qaaaabbb_R3A_view", views.get(0));
        assertEquals("The second view name is incorrect", "qccccddd_view", views.get(1));
        assertEquals("The third view name is incorrect", "qeeefff_view", views.get(2));
        assertEquals("The fourth view name is incorrect", "qeeefff_HUDSON_SHORT_CS_TEST", views.get(3));
    }
    @Test
    public void testListActiveDynamicViews() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "lsview" })),
                                      (InputStream) with(anything()), (OutputStream) with(an(OutputStream.class)),
                                      with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        List<String> views = clearToolExec.lsview(true);
        assertEquals("The view list should contain 1 item", 1, views.size());
        assertEquals("The third view name is incorrect", "qeeefff_view", views.get(0));
    }
    @Test
    public void testListVobs() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "lsvob" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        List<String> vobs = clearToolExec.lsvob(false);
        assertEquals("The vob list should contain 6 items", 6, vobs.size());
        assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
        assertEquals("The second vob name is incorrect", "pvoba", vobs.get(1));
        assertEquals("The third vob name is incorrect", "doc", vobs.get(2));
        assertEquals("The fourth vob name is incorrect", "demoa", vobs.get(3));
        assertEquals("The fifth vob name is incorrect", "pvob", vobs.get(4));
        assertEquals("The sixth vob name is incorrect", "bugvob", vobs.get(5));
    }
    @Test
    public void testListVobsMounted() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "lsvob" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        List<String> vobs = clearToolExec.lsvob(true);
        assertEquals("The vob list should contain 3 items", 3, vobs.size());
        assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
        assertEquals("The second vob name is incorrect", "demoa", vobs.get(1));
        assertEquals("The third vob name is incorrect", "pvob", vobs.get(2));
    }
    

    @Test
    public void testLshistory() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'+0000", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String formattedDate = formatter.format(mockedCalendar.getTime()).toLowerCase();
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace();
                    will(returnValue(workspace));
                    allowing(ccLauncher).getLauncher();
                    will(returnValue(new Launcher.LocalLauncher(null)));
                    one(ccLauncher).run(
                                      with(equal(new String[] { "lshistory", "-all", "-since", formattedDate,
                                                                "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco",
                                                                "vob1", "vob2", "\"vob 3\"" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        Reader reader = clearToolExec.lshistory("FORMAT",
                                                mockedCalendar.getTime(), "viewName","branch", new String[]{ "vob1", "vob2\n", "vob 3"});
        assertNotNull("Returned console reader can not be null", reader);
    }

    @Test
    public void testMkbl() throws Exception {
        context.checking(new Expectations() {
            {
                one(ccLauncher).run(
                        with(equal(new String[] { "mkbl", "-comment",
                                "comment", "-incremental", "-view", "viewTag",
                                "myBl" })), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)),
                        with(any(FilePath.class)));
                will(doAll(
                        new StreamCopyAction(2, ClearToolExecTest.class
                                .getResourceAsStream("ct-mkbl-1.log")),
                        returnValue(Boolean.TRUE)));
            }
        });
        List<Baseline> baselines = clearToolExec.mkbl("myBl", "viewTag",
                "comment", false, false, null, null, null);
        assertEquals(1, baselines.size());
        Baseline baseline = baselines.get(0);
        assertEquals("mybl", baseline.getBaselineName());
        assertEquals("mycomponent", baseline.getComponentName());
    }
    
    @Test
    public void testLsHistoryBranchNotFound() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'+0000", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        final String formattedDate = formatter.format(mockedCalendar.getTime()).toLowerCase();
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace();
                    will(returnValue(workspace));
                    allowing(ccLauncher).getLauncher();
                    will(returnValue(new Launcher.LocalLauncher(null)));
                    one(ccLauncher).run(
                                      with(equal(new String[] { "lshistory", "-all", "-since", formattedDate,
                                                                "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco",
                                                                "vob1", "vob2", "\"vob 3\"" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log")),
                               throwException(new IOException())));
                }
            });
        Reader reader = clearToolExec.lshistory("FORMAT",
                                                mockedCalendar.getTime(), "viewName","branch", new String[]{ "vob1", "vob2\n", "vob 3"});
        assertNotNull("Returned console reader cannot be null", reader);
    }

    @Test
    public void testCatConfigSpec() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "catcs", "-tag", "viewname" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-catcs-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        String configSpec = clearToolExec.catcs("viewname");
        assertEquals("The config spec was not correct", "element * CHECKEDOUT\nelement * ...\\rel2_bugfix\\LATEST\nelement * \\main\\LATEST -mkbranch rel2_bugfix", configSpec);
    }
    
    @Test
    public void testRemoveView() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "rmview", "-force",
                                                                         "viewName" })), with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.rmview("viewName");
    }
    
    @Test
    public void testForcedRemoveView() throws Exception {
        workspace.child("viewName").mkdirs();
        
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "rmview", "-force",
                                                                         "viewName" })), with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                    one(ccLauncher).getListener();
                    will(returnValue(listener));
                    one(listener).getLogger();
                    will(returnValue(new PrintStream(new ByteArrayOutputStream())));
                }
            });
        
        clearToolExec.rmview("viewName");
        assertFalse("View folder still exists", workspace.child("viewName")
                    .exists());
    }
    
    @Test
    public void testRmTag() throws Exception {
        context.checking(new Expectations() {
            {
                one(ccLauncher).run(with(equal(new String[] {"rmtag", "-view", "myViewTag"})),
                                               with(any(InputStream.class)),
                                               with(any(OutputStream.class)),
                                               with(any(FilePath.class)));
            }
        });
        clearToolExec.rmtag("myViewTag");
    }
    
    @Test
    public void testUpdate() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "update", "-force", "-overwrite", "-log",
                                                                         "NUL" })),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", null);
    }

    @Test
    public void testSetcsCurrent() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "setcs", "-current" })),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.setcs("viewName", SetcsOption.CURRENT, null);
    }

    @Test(expected=IOException.class)
    public void testUpdateBlocked() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "update", "-force", "-overwrite", "-log",
                                                                         "NUL"})),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, this.getClass().getResourceAsStream("ct-update-2.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        
        clearToolExec.update("viewName", null);
    }
    
    @Test
    public void testSetcs() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher).run(
                                      with(allOf(hasItemInArray("setcs"),
                                                 hasItemInArray("-tag"),
                                                 hasItemInArray("viewTag"))),
                                      with(any(InputStream.class)),
                                      with(any(OutputStream.class)),
                                      with(any(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                    
                }
            });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        clearToolExec.setcsTag("viewTag", SetcsOption.CONFIGSPEC, "configspec");
    }

    @Test(expected=IOException.class)
    public void testSetcsCurrentBlocked() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(ccLauncher).run(
                                               with(equal(new String[] { "setcs", "-current" })),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, this.getClass().getResourceAsStream("ct-update-2.log")),
                               returnValue(Boolean.TRUE)));
                }
            });

        clearToolExec.setcs("viewName", SetcsOption.CURRENT, null);
    }

    
    @Test
    public void testUpdateWithLoadRulesWindows() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(false));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "more_load_rules" })),
                             with(aNonNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"\\more_load_rules"});
    }

        @Test
    public void testUpdateWithLoadRules() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(true));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "more_load_rules" })),
                             with(aNonNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"/more_load_rules"});
    }

    @Test
    public void testUpdateWithLoadRulesWithSpace() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(true));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    one(ccLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "\"more load_rules\"" })),
                             with(aNonNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"/more load_rules"});
    }

    @Test
    public void testUpdateWithLoadRulesWithSpaceWin() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(false));
                }
            });
        
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace(); will(returnValue(workspace));
                    allowing(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "\"more load_rules\"" })),
                             with(aNonNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"\\more load_rules"});
    }
    
    @Test
    public void testCreateView() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "viewpath" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.mkview("viewpath", "viewName", null);
    }
    
    @Test
    public void testCreateViewWithStream() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-stream", "streamSelector", "-tag",
                                                                         "viewName", "viewpath" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec.mkview("viewpath", "viewName", "streamSelector");
    }

    @Test
    public void testCreateViewExtraParams() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "-anextraparam",
                                                                         "-anotherparam", "viewpath" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher,
                                              "-anextraparam -anotherparam");
        clearToolExec.mkview("viewpath", "viewName", null);
    }

    @Test
    public void testCreateUcmViewWithOptionalParams() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-stream", "streamSelector", "-tag",
                                                                         "viewName", "-anextraparam", "-anotherparam",
                                                                         "viewpath" })), with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher,
                                              "-anextraparam -anotherparam");
        clearToolExec.mkview("viewpath", "viewName", "streamSelector");
    }

    @Test
    public void testCreateViewExtraParamsEvaluated() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "-anextraparam",
                                                                         "Test", "viewpath" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        context.checking(new Expectations() {
                {
                    atLeast(1).of(resolver).resolve("COMPUTERNAME");
                    will(returnValue("Test"));
                }
            });
        clearToolExec = new ClearToolSnapshot(resolver, ccLauncher,
                                              "-anextraparam $COMPUTERNAME");
        clearToolExec.mkview("viewpath", "viewName", null);
    }
    
    @Test
    public void testDescribe() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(with(equal(new String[] { "desc", "-fmt", "format", "stream:stream_selector@\\a_vob" })), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), with(aNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-desc-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        Reader reader = clearToolExec.describe("format", "stream:stream_selector@\\a_vob");
        assertNotNull("Returned console reader cannot be null", reader);
    }
    
    @Test
    public void assertLsactivityReturnsReader() throws Exception {
        workspace.child("viewName").mkdirs();
        context.checking(new Expectations() {
                {
                    one(ccLauncher).getWorkspace();
                    will(returnValue(workspace));                
                    one(ccLauncher).run(
                                      with(equal(new String[] { "lsactivity", "-fmt", "ACTIVITY_FORMAT", 
                                                                "ACTIVITY@VOB"})), (InputStream) with(anything()),
                                      (OutputStream) with(an(OutputStream.class)), (FilePath) with(an(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lsactivity-1.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        Reader reader = clearToolExec.lsactivity("ACTIVITY@VOB", "ACTIVITY_FORMAT","VIEW_NAME");
        assertNotNull("Returned console reader can not be null", reader);
    }
    
    @Test
    public void testStartview() throws Exception {
        context.checking(new Expectations() {
                {
                    one(ccLauncher).run(
                                      with(allOf(hasItemInArray("startview"),
                                                 hasItemInArray("viewName"))),
                                      with(aNull(InputStream.class)),
                                      with(aNull(OutputStream.class)),
                                      with(aNull(FilePath.class)));
                }
            });
        clearToolExec.startView("viewName");
    }
    
    /**
     * Make sure that if we call setcs with a null or empty string for the config spec,
     * we get a call to cleartool setcs -current.
     */
    @Test
    public void testSetcsTag() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(ccLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(ccLauncher).getLauncher(); will(returnValue(launcher));
                    one(ccLauncher).run(
                                      with(allOf(hasItemInArray("setcs"),
                                                 hasItemInArray("-tag"),
                                                 hasItemInArray("viewName"),
                                                 hasItemInArray("-current"))),
                                      with(any(InputStream.class)),
                                      with(any(OutputStream.class)),
                                      with(any(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });
        
        clearToolExec.setcsTag("viewName", SetcsOption.CURRENT, null);
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
