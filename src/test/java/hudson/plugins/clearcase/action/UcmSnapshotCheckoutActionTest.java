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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool, "stream", "loadrule");
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }
    
    @Test
    public void testSecondTime() throws Exception {
        workspace.child("viewname").mkdirs();
        
        context.checking(new Expectations() {
            {
                one(clearTool).rmview("viewname");
                one(clearTool).mkview("viewname", "stream");
                one(clearTool).update("viewname", "loadrule");
            }
        });

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool, "stream", "loadrule");
        action.checkout(launcher, workspace, "viewname");

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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool, "stream", "loadrule\r\nanother\t loadrule");
        action.checkout(launcher, workspace, "viewname");

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

        CheckOutAction action = new UcmSnapshotCheckoutAction(clearTool, "stream", "\\ \\Windows\n\\\\C:\\System32");
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
    }    
    
}
