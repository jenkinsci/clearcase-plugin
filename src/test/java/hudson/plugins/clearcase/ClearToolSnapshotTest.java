package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import hudson.FilePath;
import hudson.model.TaskListener;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the cleartool snapshot view
 */
public class ClearToolSnapshotTest extends AbstractWorkspaceTest {

    private Mockery context;

    private ClearTool clearToolExec;
    private ClearToolLauncher launcher;

    private TaskListener listener;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();

        clearToolExec = new ClearToolSnapshot("commandname");
        launcher = context.mock(ClearToolLauncher.class);
        listener = context.mock(TaskListener.class);
    }

    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testSetcs() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(with(Matchers.hasItemInArray("commandname")), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.setcs(launcher, "viewName", "configspec");
        context.assertIsSatisfied();
    }

    @Test
    public void testRemoveView() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(with(equal(new String[] { "commandname", "rmview", "-force", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.rmview(launcher, "viewName");
        context.assertIsSatisfied();
    }

    @Test
    public void testForcedRemoveView() throws Exception {
        workspace.child("viewName").mkdirs();

        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(with(equal(new String[] { "commandname", "rmview", "-force", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
                one(launcher).getListener();
                will(returnValue(listener));
                one(listener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
            }
        });

        clearToolExec.rmview(launcher, "viewName");
        assertFalse("View folder still exists", workspace.child("viewName").exists());
        context.assertIsSatisfied();
    }

    @Test
    public void testUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "commandname", "update", "-force", "-log", "NUL", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.update(launcher, "viewName");
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateView() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher)
                        .run(
                                with(equal(new String[] { "commandname", "mkview", "-snapshot", "-tag", "viewName",
                                        "viewName" })), with(aNull(InputStream.class)),
                                with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.mkview(launcher, "viewName");
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateViewExtraParams() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher)
                        .run(
                                with(equal(new String[] { "commandname", "mkview", "-snapshot", "-tag", "viewName",
                                        "-anextraparam", "-anotherparam", "viewName" })), with(aNull(InputStream.class)),
                                with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec = new ClearToolSnapshot("commandname", "-anextraparam -anotherparam");
        clearToolExec.mkview(launcher, "viewName");
        context.assertIsSatisfied();
    }
}
