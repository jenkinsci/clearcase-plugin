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
                atLeast(1).of(launcher).isUnix(); will(returnValue(false));
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
            	atLeast(1).of(launcher).isUnix(); will(returnValue(true));
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
            	atLeast(1).of(launcher).isUnix(); will(returnValue(true));
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
        classContext.checking(new Expectations() {
            {
            	atLeast(1).of(launcher).isUnix(); will(returnValue(true));
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
            	atLeast(1).of(launcher).isUnix(); will(returnValue(true));
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
                one(clearTool).setcs("viewname", "configspec");
            }
        });
        classContext.checking(new Expectations() {
            {
            	atLeast(1).of(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "configspec", true);
        action.checkout(launcher, workspace, "viewname");

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
}
