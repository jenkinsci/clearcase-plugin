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

import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UcmSnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    private Mockery context;
    private Mockery classContext;

    private ClearTool clearTool;

    private Launcher launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();
        clearTool = context.mock(ClearTool.class);
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        launcher = classContext.mock(Launcher.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testFirstTime() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", "stream");
                one(clearTool).update("viewname", "loadrule");
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "loadrule", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }

    @Test
    public void testSecondTime() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs("viewname");
                one(clearTool).rmview("viewname");
                one(clearTool).mkview("viewname", "stream");
                one(clearTool).update("viewname", "loadrule");
            }
        });

        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "loadrule", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeWithLoadRulesSatisfied() throws Exception {
        final String viewName = "viewname";
        final String loadRules = "abc/";
        workspace.child(viewName).mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(viewName);
                will(returnValue("load " + loadRules));
                one(clearTool).update(viewName, loadRules);
            }
        });

        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", loadRules, true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeWithMultipleLoadRulesSatisfied() throws Exception {
        final String viewName = "viewname";

        workspace.child(viewName).mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(viewName);
                will(returnValue("load abc/\nload abcd"));
                one(clearTool).update(viewName, "abc/");
                one(clearTool).update(viewName, "abcd");
            }
        });
        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
                "stream", "abc/\nabcd", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeWithMultipleLoadRulesNotSatisfied()
            throws Exception {
        final String viewName = "viewname";
        workspace.child(viewName).mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(viewName);
                will(returnValue("load abc/"));
                one(clearTool).rmview(viewName);
                one(clearTool).mkview(viewName, "stream");
                one(clearTool).update(viewName, "abc/");
                one(clearTool).update(viewName, "abcd");
            }
        });
        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
                "stream", "abc/\nabcd", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testMultipleLoadRules() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", "stream");
                one(clearTool).update("viewname", "loadrule");
                one(clearTool).update("viewname", "another\t loadrule");
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
                "stream", "loadrule\r\nanother\t loadrule", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeWithRealMultipleLoadRulesSatisfied()
            throws Exception {
        final String viewName = "viewname";

        final String catcsOutput = "ucm\nidentity UCM.Stream oid:b689a462.74e011dd.8df7.00:16:35:7e:e1:93@vobuuid:9851d17f.c5aa11dc.9e7d.00:16:35:7e:e1:93 2\n"
                + "# ONLY EDIT THIS CONFIG SPEC IN THE INDICATED \"CUSTOM\" AREAS\n#\n# This config spec was automatically generated by the UCM stream\n"
                + "# \"base_1_4_Integration\" at 2008-09-02T16:12:44+02.\n#\n\n\n\n# Select checked out versions\nelement * CHECKEDOUT\n# Component selection rules..."
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" .../base_1_4_Integration/LATEST\n"
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" base_Main_080902_bjsa01_dsme_deliver -mkbranch base_1_4_Integration\n"
                + "element \"[7781d0a2c5aa11dc9e670016357ee193=\base]/...\" /main/0 -mkbranch base_1_4_Integration\n"
                + "\n\n\nend ucm\n\n#UCMCustomElemBegin - DO NOT REMOVE - ADD CUSTOM ELEMENT RULES AFTER THIS LINE\n#UCMCustomElemEnd - DO NOT REMOVE - END CUSTOM ELEMENT RULES"
                + "\n# Non-included component backstop rule: no checkouts\nelement * /main/0 -ucm -nocheckout\n\n#UCMCustomLoadBegin - DO NOT REMOVE - ADD CUSTOM LOAD RULES AFTER THIS LINE\n"
                + "load /vobs/base";

        workspace.child(viewName).mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(viewName);
                will(returnValue(catcsOutput));
                one(clearTool).update(viewName, "vobs/base");
            }
        });
        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "vobs/base", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testMultipleWindowsLoadRules() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", "stream");
                one(clearTool).update("viewname", "\\ \\Windows");
                one(clearTool).update("viewname", "\\\\C:\\System32");
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "\\ \\Windows\n\\\\C:\\System32", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }
    

    @Test
    public void testFirstTimeWithNoUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", "stream");
                atLeast(1).of(clearTool).update(with(any(String.class)), with(any(String.class)));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "loadrule", false);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeWithNoUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).rmview("viewname");
                one(clearTool).mkview("viewname", "stream");
                atLeast(1).of(clearTool).update(with(any(String.class)), with(any(String.class)));
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "loadrule", false);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
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
            + "load \\PRODUCT\r\n"
            + "load \\COTS\\NUnit\r\n";

        context.checking(new Expectations() {
                {
                    one(clearTool).catcs("viewname");
                    will(returnValue(catcsOutput));
                    one(clearTool).update("viewname", "PRODUCT");
                    one(clearTool).update("viewname", "COTS\\NUnit");
                }
            });
        classContext.checking(new Expectations() {
            {
                atLeast(1).of(launcher).isUnix(); will(returnValue(false));
            }
        });

        workspace.child("viewname").mkdirs();

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "PRODUCT\nCOTS\\NUnit", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

}
