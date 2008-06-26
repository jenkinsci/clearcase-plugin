package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import hudson.Launcher;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DynamicCheckoutActionTest extends AbstractWorkspaceTest {

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
    public void testChangeInConfigSpecOnUnix() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).startView("viewname");
                one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                one(clearTool).setcs("viewname", "config\nspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(launcher).isUnix(); will(returnValue(true));
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec");
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).startView("viewname");
                one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                one(clearTool).setcs("viewname", "config\r\nspec");
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(launcher).isUnix(); will(returnValue(false));
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec");
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testNoChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).startView("viewname");
                one(clearTool).catcs("viewname"); will(returnValue("config\nspec"));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(launcher).isUnix(); will(returnValue(false));
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec");
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
}
