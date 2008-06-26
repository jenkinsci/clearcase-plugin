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

public class SnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    private Mockery classContext;
    private Mockery context;

    private ClearTool clearTool;
    private Launcher launcher;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        launcher = classContext.mock(Launcher.class);
        clearTool = context.mock(ClearTool.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testFirstTimeNotOnUnix() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", null);
                one(clearTool).setcs("viewname", "config\r\nspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(false));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "config\r\nspec", false);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testFirstTimeOnUnix() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", null);
                one(clearTool).setcs("viewname", "config\nspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "config\r\nspec", false);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testFirstTimeUsingUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview("viewname", null);
                one(clearTool).setcs("viewname", "configspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "configspec", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testSecondTimeUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs("viewname"); will(returnValue("configspec"));
                one(clearTool).update("viewname", null);
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "configspec", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testSecondTimeNotUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).rmview("viewname");
                one(clearTool).mkview("viewname", null);
                one(clearTool).setcs("viewname", "configspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "configspec", false);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeNewConfigSpec() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                one(clearTool).rmview("viewname");
                one(clearTool).mkview("viewname", null);
                one(clearTool).setcs("viewname", "configspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "configspec", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
}
