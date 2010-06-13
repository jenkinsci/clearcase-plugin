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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertTrue;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearCaseDataAction;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.SetcsOption;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class DynamicCheckoutActionTest extends AbstractWorkspaceTest {

    private Mockery classContext;
    private Mockery context;

    private ClearTool clearTool;
    private Launcher launcher;
    private AbstractBuild<?, ?> abstractBuild;
    private ClearCaseDataAction clearCaseDataAction;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new JUnit4Mockery();
        classContext = new JUnit4Mockery() {
                {
                    setImposteriser(ClassImposteriser.INSTANCE);
                }
            };

        launcher = classContext.mock(Launcher.class);
        clearTool = context.mock(ClearTool.class);
        abstractBuild = classContext.mock(AbstractBuild.class);
        clearCaseDataAction = classContext.mock(ClearCaseDataAction.class);
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
                    one(clearTool).setcs("viewname", SetcsOption.CONFIGSPEC, "config\nspec");
                }
            });
        classContext.checking(new Expectations() {

                {
                    ignoring(launcher).isUnix(); will(returnValue(true));
                    ignoring(abstractBuild).getAction(ClearCaseDataAction.class); will(returnValue(clearCaseDataAction));
                    ignoring(clearCaseDataAction).setCspec("config\nspec");
                }
            });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
    }
    
    @Test
    public void testChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).startView("viewname");
                    one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                    one(clearTool).setcs("viewname", SetcsOption.CONFIGSPEC, "config\r\nspec");
                }
            });
        classContext.checking(new Expectations() {
                {
                    ignoring(launcher).isUnix(); will(returnValue(false));
                    ignoring(abstractBuild).getAction(ClearCaseDataAction.class); will(returnValue(clearCaseDataAction));
                    ignoring(clearCaseDataAction).setCspec("config\r\nspec");
                }
            });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
    }
    
    @Test
    public void testChangeInConfigSpecDoNotResetConfigSpecEnabled() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).startView("viewname");
                    one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                }
            });
        classContext.checking(new Expectations() {
                {
                    ignoring(launcher).isUnix(); will(returnValue(false));
                    ignoring(abstractBuild).getAction(ClearCaseDataAction.class); will(returnValue(clearCaseDataAction));
                    ignoring(clearCaseDataAction).setCspec("other configspec");
                }
            });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", true, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
    }

    @Test
    public void testNoChangeInConfigSpec() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).startView("viewname");
                    one(clearTool).catcs("viewname"); will(returnValue("config\nspec"));
                    one(clearTool).setcs("viewname", SetcsOption.CURRENT, null);
                }
            });
        classContext.checking(new Expectations() {
                {
                    ignoring(launcher).isUnix(); will(returnValue(false));
                    ignoring(abstractBuild).getAction(ClearCaseDataAction.class); will(returnValue(clearCaseDataAction));
                    ignoring(clearCaseDataAction).setCspec("config\nspec");
                }
            });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, false, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
    }

    @Test
    public void testUseTimeRule() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).startView("viewname");
                    one(clearTool).catcs("viewname"); will(returnValue("config\nspec"));
                    one(clearTool).setcs(with(equal("viewname")), with(equal(SetcsOption.CONFIGSPEC)), with(containsString("time ")));
                }
            });
        classContext.checking(new Expectations() {
                {
                    ignoring(launcher).isUnix(); will(returnValue(false));
                    ignoring(abstractBuild).getAction(ClearCaseDataAction.class); will(returnValue(clearCaseDataAction));
                    ignoring(clearCaseDataAction).setCspec(with(containsString("time ")));
                }
            });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false, true, false, null, null, abstractBuild);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);
    }

}
