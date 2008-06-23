package hudson.plugins.clearcase;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import hudson.FilePath;
import hudson.model.BuildListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
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
        clearToolExec = new ClearToolImpl(launcher);
    }
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testListViews() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(with(equal(new String[] { "lsview" })),
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
                one(launcher).run(with(equal(new String[] { "lsview" })),
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
                one(launcher).run(with(equal(new String[] { "lsvob" })), (InputStream) with(anything()),
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
                one(launcher).run(with(equal(new String[] { "lsvob" })), (InputStream) with(anything()),
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
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(
                        with(equal(new String[] { "lshistory", "-r", "-since", "18-nov.15:05:25",
                                "-fmt", "FORMAT", "-branch", "brtype:branch", "-nco",
                                "vob1", "vob2", "vob 3" })), (InputStream) with(anything()),
                        (OutputStream) with(an(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(doAll(new StreamCopyAction(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log")),
                        returnValue(Boolean.TRUE)));
            }
        });
        Reader reader = clearToolExec.lshistory("FORMAT",
                mockedCalendar.getTime(), "viewName","branch", new String[]{ "vob1", "vob2", "vob 3"});
        assertNotNull("Returned console reader can not be null", reader);
        context.assertIsSatisfied();
    }

    @Test
    public void testCatConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(with(equal(new String[] { "catcs", "-tag", "viewname" })), (InputStream) with(anything()),
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
                one(launcher).getWorkspace();
                will(returnValue(workspace));                
                one(launcher).run(
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
    
    /**
     * Simple impl of ClearToolExec to help testing the methods in the class
     */
    private static class ClearToolImpl extends ClearToolExec {
        public ClearToolImpl(ClearToolLauncher launcher) {
            super(launcher);
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
