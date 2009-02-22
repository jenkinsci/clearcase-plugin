package hudson.plugins.clearcase.action;

import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UcmSnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    private Mockery context;

    private ClearTool clearTool;

    private Launcher launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();
        clearTool = context.mock(ClearTool.class);
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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "loadrule", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", loadRules, true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
                "stream", "abc/\nabcd", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
                "stream", "abc/\nabcd", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool,
        		"stream", "vobs/base", true);
        action.checkout(launcher, workspace, viewName);

        context.assertIsSatisfied();
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


}
