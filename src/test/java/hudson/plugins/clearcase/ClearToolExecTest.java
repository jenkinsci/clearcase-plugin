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
import static org.junit.Assert.assertNotNull;
import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool.SetcsOption;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private Launcher launcher;
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
        context.assertIsSatisfied();
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
        context.assertIsSatisfied();
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
        context.assertIsSatisfied();
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
        context.assertIsSatisfied();
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
        context.assertIsSatisfied();
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
        assertNotNull("Returned console reader can not be null", reader);
        context.assertIsSatisfied();
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
        
        context.assertIsSatisfied();
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
        context.assertIsSatisfied();
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
    public void testSyncronizeViewWithStream() throws Exception {
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
