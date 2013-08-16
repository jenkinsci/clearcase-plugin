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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.MkViewParameters;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class DynamicCheckoutActionTest extends AbstractWorkspaceTest {

    @Mock
    private AbstractBuild<?, ?> abstractBuild;
    @Mock
    private ClearCaseDataAction clearCaseDataAction;
    @Mock
    private ClearTool           clearTool;
    @Mock
    private ClearToolLauncher   ctLauncher;
    @Mock
    private Launcher            launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testChangeInConfigSpec() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(false);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\r\nspec", false, false, false, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");

        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs2("viewname", SetcsOption.CONFIGSPEC, "config\r\nspec");
    }

    @Test
    public void testChangeInConfigSpecDoNotResetConfigSpecEnabled() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(false);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\r\nspec", true, false, false, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");

        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
    }

    @Test
    public void testChangeInConfigSpecOnUnix() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(true);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, abstractBuild);
        assertTrue("Checkout method did not return true.", action.checkout(launcher, workspace, "viewname"));
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs2("viewname", SetcsOption.CONFIGSPEC, "config\nspec");
    }

    @Test
    public void testCreateViewFirstTime() throws IOException, InterruptedException {
        when(clearTool.doesViewExist("viewname")).thenReturn(false);
        when(clearTool.getLauncher()).thenReturn(ctLauncher);
        when(ctLauncher.getLauncher()).thenReturn(launcher);
        when(launcher.isUnix()).thenReturn(true);
        when(clearTool.catcs("viewname")).thenReturn("other configspec");

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\nspec", false, false, true, null, abstractBuild);
        assertTrue(action.checkout(launcher, workspace, "viewname"));

        verify(clearTool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("viewname", argument.getValue().getViewPath());
    }

    @Test
    public void testCreateViewSecondTime() throws IOException, InterruptedException {
        when(clearTool.doesViewExist("viewname")).thenReturn(true);
        when(clearTool.getLauncher()).thenReturn(ctLauncher);
        when(ctLauncher.getLauncher()).thenReturn(launcher);
        when(launcher.isUnix()).thenReturn(true);
        when(clearTool.catcs("viewname")).thenReturn("other configspec");

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\nspec", false, false, true, null, abstractBuild);
        assertTrue(action.checkout(launcher, workspace, "viewname"));

        verify(clearTool).doesViewExist("viewname");
        verify(clearTool).rmviewtag("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(clearTool).mkview(argument.capture());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("viewname", argument.getValue().getViewPath());
    }

    @Test
    public void testNoChangeInConfigSpec() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("config\nspec");
        when(launcher.isUnix()).thenReturn(false);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");

        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs2("viewname", SetcsOption.CURRENT, null);
    }

    @Test
    public void testUseTimeRule() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("config\nspec");
        when(launcher.isUnix()).thenReturn(false);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        CheckoutAction action = new BaseDynamicCheckoutAction(clearTool, "config\nspec", false, true, false, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs2(eq("viewname"), eq(SetcsOption.CONFIGSPEC), contains("time "));
    }

}
