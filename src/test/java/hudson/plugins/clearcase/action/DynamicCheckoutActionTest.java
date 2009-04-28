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

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false);
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

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", false);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
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
            }
        });

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\r\nspec", true);
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

        DynamicCheckoutAction action = new DynamicCheckoutAction(clearTool, "config\nspec", false);
        boolean success = action.checkout(launcher, workspace, "viewname");
        assertTrue("Checkout method did not return true.", success);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
}
