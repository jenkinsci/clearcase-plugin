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
package hudson.plugins.clearcase.action;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.MkViewParameters;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.ConfigSpec;

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class SnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    @Mock private BuildListener taskListener;
    @Mock private ClearTool     cleartool;
    @Mock private Launcher      launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testFirstTimeNotOnUnix() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("config\r\nspec", false), new String[] { "foo" }, false,
                "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\r\nspec\r\nload \\foo\r\n");
    }

    @Test
    public void testFirstTimeOnUnix() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("config\r\nspec", true), new String[] { "foo" }, false, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
    }

    @Test
    public void testFirstTimeViewTagExists() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("config\r\nspec", true), new String[] { "foo" }, false, "viewpath", null);
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");

        Assert.assertTrue("Build should succeed.", checkoutResult);
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).rmviewtag("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
    }

    @Test
    public void testFirstTimeViewPathExists() throws Exception {
        workspace.child("viewpath").mkdirs();

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("config\r\nspec", true), new String[] { "foo" }, false, "viewpath", null);
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");

        Assert.assertTrue("Checkout should succeed.", checkoutResult);

        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
    }

    @Test
    public void testFirstTimeViewPathAndViewTagExist() throws Exception {
        workspace.child("viewpath").mkdirs();
        when(cleartool.getUpdtFileName()).thenReturn(null);

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("otherviewtag");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("config\r\nspec", true), new String[] { "foo" }, false, "viewpath", null);
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");
        Assert.assertTrue("Checkout should succeed.", checkoutResult);
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).rmviewtag("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");

    }

    @Test
    public void testFirstTimeUsingUpdate() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "foo" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
    }

    @Test
    public void testSecondTimeUsingUpdate() throws Exception {
        workspace.child("viewpath").mkdirs();

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("configspec\nload /foo\n");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "/foo" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).setcs("viewpath", SetcsOption.CURRENT, null);
    }

    @Test
    public void testSecondTimeNotUsingUpdate() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");        
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);
        
        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "/foo" }, false, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).rmview("viewpath");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
    }

    @Test
    public void testSecondTimeNewConfigSpec() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("other configspec");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "foo" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");        
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
        // anb0s: TODO: why setcs must be executed second time???
        //verify(cleartool).setcs("viewpath", SetcsOption.CURRENT, null);
    }

    @Test
    public void testSecondTimeNewLoadRule() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("configspec\nload /foo\n");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);
        
        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "/foo", "/bar" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).update("viewpath", new String[] { "/bar" });
        // anb0s: TODO: why setcs must be executed after update???
        //verify(cleartool).setcs("viewpath", SetcsOption.CURRENT, null);
    }

    @Test
    public void testSecondTimeRemovedLoadRule() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("configspec\nload /foo\nload /bar\n");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        when(cleartool.getUpdtFileName()).thenReturn(null);

        CheckoutAction action = new BaseSnapshotCheckoutAction(cleartool, new ConfigSpec("configspec", true), new String[] { "bar" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /bar\n");
        // anb0s: TODO: why setcs must be executed second time???
        //verify(cleartool).setcs("viewpath", SetcsOption.CURRENT, null);
    }

}
