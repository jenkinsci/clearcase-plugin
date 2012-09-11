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

import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class UcmSnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    @Mock
    private ClearTool     cleartool;

    @Mock
    private BuildListener taskListener;
    @Mock
    private Launcher      launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testFirstTime() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "loadrule" });
    }

    @Test
    public void testFirstTimeViewTagExists() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).rmviewtag("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "loadrule" });
    }

    @Test
    public void testFirstTimeViewPathExists() throws Exception {
        workspace.child("viewpath").mkdirs();

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        boolean checkout = action.checkout(launcher, workspace, "viewname");

        Assert.assertTrue("Checkout should succeed.", checkout);
        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "loadrule" });
    }

    @Test
    public void testFirstTimeViewPathAndViewTagExist() throws Exception {
        workspace.child("viewpath").mkdirs();

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("otherviewtag");
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        boolean checkout = action.checkout(launcher, workspace, "viewname");
        Assert.assertTrue("Checkout should succeed.", checkout);
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).rmviewtag("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "loadrule" });
    }

    @Test
    public void testSecondTime() throws Exception {
        workspace.child("viewpath").mkdirs();

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("ucm configspec");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).update("viewpath", new String[] { "/loadrule" });
        verify(cleartool).update("viewpath", null);
        verify(cleartool, atLeastOnce()).catcs("viewname");
        verify(launcher, atLeastOnce()).isUnix();

    }

    @Test
    public void testSecondTimeWithLoadRulesSatisfied() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.catcs("viewname")).thenReturn("load " + "/abc/");
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "abc/" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");

        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).catcs("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).update("viewpath", null);
    }

    @Test
    public void testSecondTimeWithMultipleLoadRulesSatisfied() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.catcs("viewname")).thenReturn("load /abc/\nload /abcd");
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);
        
        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "abc/", "abcd" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).catcs("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).update("viewpath", null);
    }

    @Test
    public void testSecondTimeWithMultipleLoadRulesNotSatisfied() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn("ucm configspec\nload /abc/\n");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "abc/", "abcd" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).update("viewpath", null);
        verify(cleartool).update("viewpath", new String[] { "/abcd" });
    }

    @Test
    public void testMultipleLoadRules() throws Exception {
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        
        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule", "another\t loadrule" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "loadrule", "another\t loadrule" });
    }

    @Test
    public void testSecondTimeWithRealMultipleLoadRulesSatisfied() throws Exception {
        final String catcsOutput = "ucm\nidentity UCM.Stream oid:b689a462.74e011dd.8df7.00:16:35:7e:e1:93@vobuuid:9851d17f.c5aa11dc.9e7d.00:16:35:7e:e1:93 2\n"
                + "# ONLY EDIT THIS CONFIG SPEC IN THE INDICATED \"CUSTOM\" AREAS\n#\n# This config spec was automatically generated by the UCM stream\n"
                + "# \"base_1_4_Integration\" at 2008-09-02T16:12:44+02.\n#\n\n\n\n# Select checked out versions\nelement * CHECKEDOUT\n# Component selection rules..."
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" .../base_1_4_Integration/LATEST\n"
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" base_Main_080902_bjsa01_dsme_deliver -mkbranch base_1_4_Integration\n"
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" /main/0 -mkbranch base_1_4_Integration\n"
                + "\n\n\nend ucm\n\n#UCMCustomElemBegin - DO NOT REMOVE - ADD CUSTOM ELEMENT RULES AFTER THIS LINE\n#UCMCustomElemEnd - DO NOT REMOVE - END CUSTOM ELEMENT RULES"
                + "\n# Non-included component backstop rule: no checkouts\nelement * /main/0 -ucm -nocheckout\n\n#UCMCustomLoadBegin - DO NOT REMOVE - ADD CUSTOM LOAD RULES AFTER THIS LINE\n"
                + "load /vobs/base";

        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.catcs("viewname")).thenReturn(catcsOutput);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "vobs/base" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).catcs("viewname");
        verify(cleartool).update("viewpath", null);
        verify(cleartool).lscurrentview("viewpath");
    }

    @Test
    public void testMultipleWindowsLoadRules() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(launcher.isUnix()).thenReturn(Boolean.FALSE);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "\\ \\Windows", "\\\\C:\\System32" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool).update("viewpath", new String[] { "\\ \\Windows", "\\\\C:\\System32" });
    }

    @Test
    public void testFirstTimeWithNoUpdate() throws Exception {
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.FALSE);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool, atLeastOnce()).update("viewpath", new String[] { "loadrule" });
    }

    @Test
    public void testSecondTimeWithNoUpdate() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "loadrule" }, false, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).rmview("viewpath");
        ArgumentCaptor<MkViewParameters> argument = ArgumentCaptor.forClass(MkViewParameters.class);
        verify(cleartool).mkview(argument.capture());
        assertEquals("viewpath", argument.getValue().getViewPath());
        assertEquals("viewname", argument.getValue().getViewTag());
        assertEquals("stream", argument.getValue().getStreamSelector());
        verify(cleartool, atLeastOnce()).update("viewpath", new String[] { "loadrule" });
        verify(cleartool).lscurrentview("viewpath");
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
        
        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "foo", "bar" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).update("viewpath", new String[] { "/bar" });
        verify(cleartool).update("viewpath", null);
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

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "bar" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).lscurrentview("viewpath");
        verify(cleartool).catcs("viewname");
        verify(cleartool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /bar\n");
        verify(cleartool).update("viewpath", null);
    }

    @Test
    public void testCrossOSLoadRulesOnWindows() throws Exception {
        final String catcsOutput = "ucm\r\nidentity UCM.Stream oid:19c4bc38.514b4432.b8a6.65:da:92:41:9f:df@vobuuid:a10d9aff.8c1349a8.829e.24:09:ef:18:ad:e6 16\r\n\r\n"
                + "# ONLY EDIT THIS CONFIG SPEC IN THE INDICATED \"CUSTOM\" AREAS\r\n"
                + "#\r\n\r\n# This config spec was automatically generated by the UCM stream\r\n# \"WindowsForms_Int\" at 8/1/2007 12:36:35 PM.\r\n"
                + "#\r\n\r\n\r\n\r\n# checked out versions\r\nelement * CHECKEDOUT\r\n\r\n"
                + "# Component selection rules...\r\n\r\n"
                + "element \"[67e2eb701aee11d4bc5e00c04f424ddc=\\swtools]/...\" SWTools_HBO_6_19_2007.4946 -nocheckout\r\n\r\n"
                + "element \"[b48af254a0484bd5bb844790fdb66fc9=\\COTS]/NUnit/...\" NUNIT_2.4.0_R2_NET_2.0 -nocheckout\r\n\r\n"
                + "element \"[b48af254a0484bd5bb844790fdb66fc9=\\COTS]/NUnit_Forms/...\" NUNIT_FORMS_2.2.7_14-FEB-2007.7642 -nocheckout\r\n\r\n"
                + "element \"[cdcd50de68ad453bb336aeecac9d42f0=\\SharedUI]/Windows.Forms/...\" .../WindowsForms_Int/LATEST\r\n\r\n"
                + "element \"[cdcd50de68ad453bb336aeecac9d42f0=\\SharedUI]/Windows.Forms/...\" /main/0 -mkbranch WindowsForms_Int\r\n\r\n"
                + "element \"[cdcd50de68ad453bb336aeecac9d42f0=\\SharedUI]/Windows.Forms/...\" /main/0 -mkbranch WindowsForms_Int\r\n\r\n"
                + "element \"[fa118f5513484f90b4330078280a4fe6=\\PRODUCT]/WINDOWSFORMS_PRODUCT_INFO/...\" .../WindowsForms_Int/LATEST\r\n"
                + "element \"[fa118f5513484f90b4330078280a4fe6=\\PRODUCT]/WINDOWSFORMS_PRODUCT_INFO/...\" /main/0 -mkbranch WindowsForms_Int\r\n"
                + "element \"[fa118f5513484f90b4330078280a4fe6=\\PRODUCT]/WINDOWSFORMS_PRODUCT_INFO/...\" /main/0 -mkbranch WindowsForms_Int\r\n\r\n\r\n"
                + "end ucm\r\n\r\n"
                + "#UCMCustomElemBegin - DO NOT REMOVE - ADD CUSTOM ELEMENT RULES AFTER THIS LINE\r\n"
                + "#UCMCustomElemEnd - DO NOT REMOVE - END CUSTOM ELEMENT RULES\r\n\r\n"
                + "# Non-included component backstop rule: no checkouts\r\n"
                + "element * /main/0 -ucm -nocheckout\r\n\r\n"
                + "#UCMCustomLoadBegin - DO NOT REMOVE - ADD CUSTOM LOAD RULES AFTER THIS LINE\r\n"
                + "load \\PRODUCT\r\n" + "load \\COTS\\NUnit\r\n";

        when(cleartool.doesViewExist("viewname")).thenReturn(Boolean.TRUE);
        when(cleartool.lscurrentview("viewpath")).thenReturn("viewname");
        when(cleartool.catcs("viewname")).thenReturn(catcsOutput);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(launcher.isUnix()).thenReturn(Boolean.TRUE);
        when(launcher.getListener()).thenReturn(taskListener);

        workspace.child("viewpath").mkdirs();

        CheckoutAction action = new UcmSnapshotCheckoutAction(cleartool, "stream", new String[] { "PRODUCT", "COTS\\NUnit" }, true, "viewpath", null);
        action.checkout(launcher, workspace, "viewname");
        
        verify(cleartool).doesViewExist("viewname");
        verify(cleartool).catcs("viewname");
        verify(cleartool).update("viewpath", null);
        verify(cleartool).lscurrentview("viewpath");
        
    }

}
