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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DynamicCheckoutActionTest extends AbstractWorkspaceTest {

    @Mock
    private ClearTool           clearTool;
    @Mock
    private Launcher            launcher;
    @Mock
    private AbstractBuild<?, ?> abstractBuild;
    @Mock
    private ClearCaseDataAction clearCaseDataAction;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testChangeInConfigSpecOnUnix() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);
        
        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        
        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs("viewname", SetcsOption.CONFIGSPEC, "config\nspec");
    }

    @Test
    public void testChangeInConfigSpec() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        
        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs("viewname", SetcsOption.CONFIGSPEC, "config\r\nspec");
    }

    @Test
    public void testChangeInConfigSpecDoNotResetConfigSpecEnabled() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("other configspec");
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", true, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        
        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
    }

    @Test
    public void testNoChangeInConfigSpec() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("config\nspec");
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        
        assertTrue("Checkout method did not return true.", success);
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs("viewname", SetcsOption.CURRENT, null);
    }

    @Test
    public void testUseTimeRule() throws Exception {
        when(clearTool.catcs("viewname")).thenReturn("config\nspec");
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);
        when(abstractBuild.getAction(ClearCaseDataAction.class)).thenReturn(clearCaseDataAction);

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, true, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
        
        verify(clearTool).startView("viewname");
        verify(clearTool).catcs("viewname");
        verify(clearTool).setcs(eq("viewname"), eq(SetcsOption.CONFIGSPEC), contains("time "));
    }

}
