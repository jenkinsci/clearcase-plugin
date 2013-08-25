/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.MkViewParameters;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class AbstractCheckoutActionTest extends AbstractWorkspaceTest {

    private static class DummyCheckoutAction extends SnapshotCheckoutAction {

        public DummyCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate, String viewPath) {
            super(cleartool, loadRules, useUpdate, viewPath, null);
        }

        @Override
        public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {
            return false;
        }

        @Override
        public boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException,
        InterruptedException {
            return super.cleanAndCreateViewIfNeeded(workspace, viewTag, viewPath, streamSelector);
        }
    }

    @Mock
    private ClearTool         clearTool;
    @Mock
    private ClearToolLauncher ctLauncher;
    @Mock
    private TaskListener      taskListener;

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
    public void secondTimeWithUseUpdateShouldDoNothing() throws Exception {
        workspace.child("path").mkdirs();

        when(clearTool.doesViewExist("aViewTag")).thenReturn(Boolean.TRUE);
        when(clearTool.lscurrentview("path")).thenReturn("aViewTag");

        DummyCheckoutAction action = new DummyCheckoutAction(clearTool, new String[] { "aLoadRule" }, true, "");
        action.cleanAndCreateViewIfNeeded(workspace, "aViewTag", "path", "stream@\\pvob");

        verify(clearTool).doesViewExist("aViewTag");
        verify(clearTool).lscurrentview("path");
    }

    @Before
    public void setUp() throws Exception {
        when(clearTool.getLauncher()).thenReturn(ctLauncher);
        when(ctLauncher.getListener()).thenReturn(taskListener);
        when(taskListener.getLogger()).thenReturn(System.out);
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }
}
