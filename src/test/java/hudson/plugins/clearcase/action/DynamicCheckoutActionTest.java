package hudson.plugins.clearcase.action;

import static org.junit.Assert.*;
import hudson.Launcher;
import hudson.model.BuildListener;
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
    private BuildListener taskListener;
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
        taskListener = context.mock(BuildListener.class);
        clearTool = context.mock(ClearTool.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).setView(with(equal("viewname")));
                one(clearTool).catcs(with(equal("viewname"))); will(returnValue("other configspec"));
                one(clearTool).setcs(with(equal("viewname")), with(equal("configspec")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "viewname", "configspec");
        boolean success = action.checkout(launcher, workspace, taskListener);
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testNoChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).setView(with(equal("viewname")));
                one(clearTool).catcs(with(equal("viewname"))); will(returnValue("config\nspec"));
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "viewname", "config\nspec");
        boolean success = action.checkout(launcher, workspace, taskListener);
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
}
