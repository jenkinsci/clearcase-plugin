package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.MkViewParameters;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class AbstractCheckoutActionTest extends AbstractWorkspaceTest {

    private static class DummyCheckoutAction extends AbstractCheckoutAction {

        @Override
        public boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException,
                InterruptedException {
            return super.cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        }

        public DummyCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate, String viewPath) {
            super(cleartool, loadRules, useUpdate, viewPath, null);
        }

        @Override
        public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {
            return false;
        }

    	public String getUpdtFileName() {
    		// TODO Auto-generated method stub
    		return null;
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
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("path", argument.getValue().getViewPath());
        assertEquals("aViewTag", argument.getValue().getViewTag());
        assertEquals("stream@\\pvob", argument.getValue().getStreamSelector());
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
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("path", argument.getValue().getViewPath());
        assertEquals("aViewTag", argument.getValue().getViewTag());
        assertEquals("stream@\\pvob", argument.getValue().getStreamSelector());
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

        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
        verify(clearTool).rmviewtag("aViewTag");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("path", argument.getValue().getViewPath());
        assertEquals("aViewTag", argument.getValue().getViewTag());
        assertEquals("stream@\\pvob", argument.getValue().getStreamSelector());
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
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("path", argument.getValue().getViewPath());
        assertEquals("aViewTag", argument.getValue().getViewTag());
        assertEquals("stream@\\pvob", argument.getValue().getStreamSelector());
    }
}
