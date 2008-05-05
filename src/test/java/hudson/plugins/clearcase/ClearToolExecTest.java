package hudson.plugins.clearcase;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import hudson.FilePath;
import hudson.model.BuildListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearToolExecTest extends AbstractWorkspaceTest {
    private Mockery context;
    private ClearToolExec clearToolExec;
    private ClearToolLauncher launcher;
    private BuildListener taskListener;
    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();
        launcher = context.mock(ClearToolLauncher.class);
        taskListener = context.mock(BuildListener.class);
        clearToolExec = new ClearToolImpl(launcher, "commandname");
    }
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testListViews() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(with(equal(new String[] { "commandname", "lsview" })),
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
                one(launcher).run(with(equal(new String[] { "commandname", "lsview" })),
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
                one(launcher).run(with(equal(new String[] { "commandname", "lsvob" })), (InputStream) with(anything()),
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
                one(launcher).run(with(equal(new String[] { "commandname", "lsvob" })), (InputStream) with(anything()),
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
    public void testLshistoryEmptyVobPath() throws Exception {
        workspace.child("viewName").mkdirs();
        workspace.child("viewName").child("vob1").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(
                        with(allOf(hasItemInArray("commandname"), hasItemInArray("lshistory"), hasItemInArray("-r"),
                                hasItemInArray("vob1"))), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });
        clearToolExec.lshistory(mockedCalendar.getTime(), "viewName", "branch", " ");
        context.assertIsSatisfied();
    }
    @Test
    public void testLshistoryNoVobPaths() throws Exception {
        workspace.child("viewName").mkdirs();
        workspace.child("viewName").child("vob1").mkdirs();
        workspace.child("viewName").child("vob2").child("vob2-1").mkdirs();
        workspace.child("viewName").child("vob 4").mkdirs();
        workspace.child("viewName").createTextTempFile("view", ".dat", "text");
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(
                        with(allOf(hasItemInArray("commandname"), hasItemInArray("lshistory"), hasItemInArray("-r"),
                                hasItemInArray("vob1"), hasItemInArray("vob2"), hasItemInArray("vob 4"))),
                        (InputStream) with(anything()), (OutputStream) with(an(OutputStream.class)),
                        with(aNonNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });
        clearToolExec.lshistory(mockedCalendar.getTime(), "viewName", "branch", "");
        context.assertIsSatisfied();
    }
    @Test
    public void testLshistory() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(
                        with(equal(new String[] { "commandname", "lshistory", "-r", "-since", "18-nov.15:05:25",
                                "-fmt", ClearToolHistoryParser.getLogFormat(), "-branch", "brtype:branch", "-nco",
                                "vob1" })), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log")),
                        returnValue(Boolean.TRUE)));
            }
        });
        List<ClearCaseChangeLogEntry> lshistory = clearToolExec.lshistory(mockedCalendar.getTime(),
                "viewName", "branch","vob1");
        assertEquals("The history should contain 3 items", 3, lshistory.size());
        context.assertIsSatisfied();
    }
    @Test
    public void testLshistoryWithVobNames() throws Exception {
        workspace.child("viewName").mkdirs();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(
                        with(equal(new String[] { "commandname", "lshistory", "-r", "-since", "18-nov.15:05:25",
                                "-fmt", ClearToolHistoryParser.getLogFormat(), "-branch", "brtype:branch", "-nco",
                                "vob2/vob2-1", "vob4" })), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });
        clearToolExec.lshistory(mockedCalendar.getTime(), "viewName", "branch", "vob2/vob2-1 vob4");
        context.assertIsSatisfied();
    }
    @Test(expected=hudson.AbortException.class)
    public void testLshistoryNoViewPath() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.set(2007, 10, 18, 15, 05, 25);
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).getListener();
                will(returnValue(taskListener));
                one(taskListener).fatalError(with(any(String.class)));
            }
        });
        clearToolExec.lshistory(mockedCalendar.getTime(), "viewName", "branch", "");
        context.assertIsSatisfied();
    }
    @Test
    public void testCatConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(with(equal(new String[] { "commandname", "catcs", "-tag", "viewname" })), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)), with(aNull(FilePath.class)));
                will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-catcs-1.log")),
                        returnValue(Boolean.TRUE)));
            }
        });
        String configSpec = clearToolExec.catcs("viewname");
        assertEquals("The config spec was not correct", "element * CHECKEDOUT\nelement * ...\\rel2_bugfix\\LATEST\nelement * \\main\\LATEST -mkbranch rel2_bugfix", configSpec);
        
        context.assertIsSatisfied();
    }
    /**
     * Simple impl of ClearToolExec to help testing the methods in the class
     */
    private static class ClearToolImpl extends ClearToolExec {
        public ClearToolImpl(ClearToolLauncher launcher, String clearToolExec) {
            super(launcher, clearToolExec);
        }
        public void checkout(String configSpec, String viewName) throws IOException,
                InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void mkview(String viewName) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void rmview(String viewName) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void setcs(String viewName, String configSpec) throws IOException,
                InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void update(String viewName) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        public void startView(String viewTag) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
        
        @Override
        protected FilePath getRootViewPath(ClearToolLauncher launcher) {
            return launcher.getWorkspace();
        }
        public void update(String viewName, String loadRules) throws IOException, InterruptedException {
            throw new IllegalStateException("Not implemented");
        }
    }
}
