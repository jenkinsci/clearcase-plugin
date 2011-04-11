package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class AbstractCheckoutActionTest extends AbstractWorkspaceTest {

    private static class DummyCheckoutAction extends AbstractCheckoutAction {

        @Override
        public boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException,
                InterruptedException {
            return super.cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        }

        public DummyCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate, String viewPath) {
            super(cleartool, loadRules, useUpdate, viewPath);
        }

        @Override
        public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {
            return false;
        }

    }

    @Mock private ClearTool clearTool;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void firstTimeShouldCreate() throws Exception {
        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.FALSE);
        
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, true, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");

        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
    }
    
    @Test
    public void secondTimeWithUseUpdateShouldDoNothing() throws Exception {
        workspace.child("path").mkdirs();
        
        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.TRUE);
        when(clearTool.lscurrentview("path")).thenReturn("aViewTag");
        
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, true, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
        
        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
    }
    
    @Test
    public void secondTimeWithoutUseUpdateRemoveThenCreateView() throws Exception {
        workspace.child("path").mkdirs();
        
        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.TRUE);
        when(clearTool.lscurrentview("path")).thenReturn("aViewTag");
        
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");

        verify(clearTool).rmview("path");
        verify(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
    }
    
    @Test
    public void secondTimeWithInvalidViewShouldRmviewTagMoveFolderThenCreateView() throws Exception {
        workspace.child("path").mkdirs();
        
        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.TRUE);
        when(clearTool.lscurrentview("path")).thenReturn("anotherViewTag");
        
        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
        assertTrue("The existing path should have been renamed", workspace.child("path.keep.1").exists());
        
        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
        verify(clearTool).rmviewtag("aViewTag");
        verify(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
    }
    
    @Test
    public void ifRmViewTagIsNotSupportedCallRmTag() throws Exception {
        workspace.child("path").mkdirs();
        
        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.TRUE);
        when(clearTool.lscurrentview("path")).thenReturn("anotherViewTag");
        doThrow(new IOException()).when(clearTool).rmviewtag("aViewTag");

        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, false, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");
        
        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
        verify(clearTool).rmviewtag("aViewTag");
        verify(clearTool).rmtag("aViewTag");
        verify(clearTool).mkview("path", "aViewTag", "stream@\\pvob");
    }
}
