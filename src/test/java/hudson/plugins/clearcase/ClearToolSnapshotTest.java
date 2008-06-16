package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
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

        launcher = context.mock(ClearToolLauncher.class);
        listener = context.mock(TaskListener.class);
        clearToolExec = new ClearToolSnapshot(launcher);
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
                one(launcher).run(with(Matchers.hasItemInArray("setcs")), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNonNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.setcs("viewName", "configspec");
        context.assertIsSatisfied();
    }

    @Test
    public void testRemoveView() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(with(equal(new String[] { "rmview", "-force", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.rmview("viewName");
        context.assertIsSatisfied();
    }

    @Test
    public void testForcedRemoveView() throws Exception {
        workspace.child("viewName").mkdirs();

        context.checking(new Expectations() {
            {
                one(launcher).getWorkspace();
                will(returnValue(workspace));
                one(launcher).run(with(equal(new String[] { "rmview", "-force", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
                one(launcher).getListener();
                will(returnValue(listener));
                one(listener).getLogger();
                will(returnValue(new PrintStream(new ByteArrayOutputStream())));
            }
        });

        clearToolExec.rmview("viewName");
        assertFalse("View folder still exists", workspace.child("viewName").exists());
        context.assertIsSatisfied();
    }

    @Test
    public void testUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "update", "-force", "-log", "NUL", "viewName" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.update("viewName", null);
        context.assertIsSatisfied();
    }

    @Test
    public void testUpdateWithLoadRules() throws Exception {

        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "update", "-force", "-log", "NUL", "-add_loadrules", "viewName" + File.separator + "more_load_rules" })),
                        with(aNull(InputStream.class)), with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.update("viewName", "more_load_rules");
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateView() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "mkview", "-snapshot", "-tag", "viewName",
                                        "viewName" })), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.mkview("viewName", null);
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateViewWithStream() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", 
                                "-tag", "viewName", "viewName" })), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec.mkview("viewName", "streamSelector");
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateViewExtraParams() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "mkview", "-snapshot", "-tag", "viewName",
                                        "-anextraparam", "-anotherparam", "viewName" })), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec = new ClearToolSnapshot(launcher, "-anextraparam -anotherparam");
        clearToolExec.mkview("viewName", null);
        context.assertIsSatisfied();
    }

    @Test
    public void testCreateUcmViewWithOptionalParams() throws Exception {
        context.checking(new Expectations() {
            {
                one(launcher).run(
                        with(equal(new String[] { "mkview", "-snapshot", "-stream", "streamSelector", 
                                "-tag", "viewName", "-anextraparam", "-anotherparam", "viewName" })), with(aNull(InputStream.class)),
                        with(aNull(OutputStream.class)), with(aNull(FilePath.class)));
                will(returnValue(Boolean.TRUE));
            }
        });

        clearToolExec = new ClearToolSnapshot(launcher, "-anextraparam -anotherparam");
        clearToolExec.mkview("viewName", "streamSelector");
        context.assertIsSatisfied();
    }
}
