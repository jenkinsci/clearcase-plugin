package hudson.plugins.clearcase.action;

import static org.junit.Assert.assertTrue;
import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AbstractCheckoutActionTest extends AbstractWorkspaceTest {

    private static class DummyCheckoutAction extends AbstractCheckoutAction {

        @Override
        public boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException,
                InterruptedException {
            return super.cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        }

        public DummyCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate) {
            super(cleartool, loadRules, useUpdate);
        }

        @Override
        public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {
            return false;
        }

    }

    private Mockery context;

    private ClearTool clearTool;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new JUnit4Mockery();

        clearTool = context.mock(ClearTool.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void firstTimeShouldCreate() throws Exception {
        context.checking(new Expectations() {
            {
               one(clearTool).doesViewExist("aViewTag"); will(returnValue(false));
               one(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
            }
        });
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, true);
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
    }
    
    @Test
    public void secondTimeWithUseUpdateShouldDoNothing() throws Exception {
        workspace.child("path").mkdirs();
        context.checking(new Expectations() {
            {
                one(clearTool).doesViewExist("aViewTag"); will(returnValue(true));
                one(clearTool).lscurrentview("path"); will(returnValue("aViewTag"));
            }
        });
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, true);
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
    }
    
    @Test
    public void secondTimeWithoutUseUpdateRemoveThenCreateView() throws Exception {
        workspace.child("path").mkdirs();
        context.checking(new Expectations() {
            {
                one(clearTool).doesViewExist("aViewTag"); will(returnValue(true));
                one(clearTool).lscurrentview("path"); will(returnValue("aViewTag"));
                one(clearTool).rmview("path");
                one(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
            }
        });
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false);
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
    }
    
    @Test
    public void secondTimeWithInvalidViewShouldRmviewTagMoveFolderThenCreateView() throws Exception {
        workspace.child("path").mkdirs();
        context.checking(new Expectations() {
            {
                one(clearTool).doesViewExist("aViewTag"); will(returnValue(true));
                one(clearTool).lscurrentview("path"); will(returnValue("anotherViewTag"));
                one(clearTool).rmviewtag("aViewTag");
                one(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
            }
        });
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false);
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
        assertTrue("The existing path should have been renamed", workspace.child("path.keep.1").exists());
    }
    
    @Test
    public void ifRmViewTagIsNotSupportedCallRmTag() throws Exception {
        workspace.child("path").mkdirs();
        context.checking(new Expectations() {
            {
                one(clearTool).doesViewExist("aViewTag"); will(returnValue(true));
                one(clearTool).lscurrentview("path"); will(returnValue("anotherViewTag"));
                one(clearTool).rmviewtag("aViewTag"); will(throwException(new IOException()));
                one(clearTool).rmtag("aViewTag");
                one(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
            }
        });
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false);
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
    }
}
