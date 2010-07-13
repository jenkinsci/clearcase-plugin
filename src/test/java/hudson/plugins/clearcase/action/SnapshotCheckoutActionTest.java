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

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.ClearTool.SetcsOption;

import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    private JUnit4Mockery classContext;
    private JUnit4Mockery context;

    private BuildListener taskListener;
    private ClearTool clearTool;
    private Launcher launcher;
    
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
        taskListener = context.mock(BuildListener.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testFirstTimeNotOnUnix() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(false));
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\r\nspec\r\nload \\foo\r\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(false));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        AbstractCheckoutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("config\r\nspec", false), new String[]{"foo"}, false, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
    @Test
    public void testFirstTimeOnUnix() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(false));
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("config\r\nspec", true), new String[]{"foo"}, false, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }

    @Test
    public void testFirstTimeViewTagExists() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).rmviewtag("viewname");
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("config\r\nspec", true), new String[]{"foo"}, false, "viewpath");
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");
        Assert.assertTrue("Build should succeed.", checkoutResult);
    }
    
    @Test
    public void testFirstTimeViewPathExists() throws Exception {
        workspace.child("viewpath").mkdirs();
        
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(false));
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("config\r\nspec", true), new String[]{"foo"}, false, "viewpath");
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");
        List<FilePath> directories = workspace.listDirectories();
        boolean foundRenamedDirectory = false;
        for (FilePath directory : directories) {
            if (directory.getName().contains("viewpath.keep.")) {
                foundRenamedDirectory = true;
                break;
            }
        }
        Assert.assertTrue("The existing path should have been renamed.", foundRenamedDirectory);
        Assert.assertTrue("Checkout should succeed.", checkoutResult);
    }
    
    @Test
    public void testFirstTimeViewPathAndViewTagExist() throws Exception {
        workspace.child("viewpath").mkdirs();
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("otherviewtag"));
                    one(clearTool).rmviewtag("viewname");
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "config\nspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("config\r\nspec", true), new String[]{"foo"}, false, "viewpath");
        boolean checkoutResult = action.checkout(launcher, workspace, "viewname");
        List<FilePath> directories = workspace.listDirectories();
        boolean foundRenamedDirectory = false;
        for (FilePath directory : directories) {
            if (directory.getName().contains("viewpath.keep.")) {
                foundRenamedDirectory = true;
                break;
            }
        }
        Assert.assertTrue("The existing path should have been renamed.", foundRenamedDirectory);
        Assert.assertTrue("Checkout should succeed.", checkoutResult);
    }

    @Test
    public void testFirstTimeUsingUpdate() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(false));
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"foo"}, true, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
    @Test
    public void testSecondTimeUsingUpdate() throws Exception {
        workspace.child("viewpath").mkdirs();

        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("viewname"));
                    one(clearTool).catcs("viewname"); will(returnValue("configspec\nload /foo\n"));
                    one(clearTool).update("viewpath", null);
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"/foo"}, true, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
    @Test
    public void testSecondTimeNotUsingUpdate() throws Exception {
        workspace.child("viewpath").mkdirs();

        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("viewname"));
                    one(clearTool).rmview("viewpath");
                    one(clearTool).mkview("viewpath", "viewname", null);
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });
        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"/foo"}, false, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }

    @Test
    public void testSecondTimeNewConfigSpec() throws Exception {
        workspace.child("viewpath").mkdirs();

        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("viewname"));
                    one(clearTool).catcs("viewname"); will(returnValue("other configspec"));
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /foo\n");
                    one(clearTool).update("viewpath", null);
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"foo"}, true, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
    @Test
    public void testSecondTimeNewLoadRule() throws Exception {
        workspace.child("viewpath").mkdirs();

        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("viewname"));
                    one(clearTool).catcs("viewname"); will(returnValue("configspec\nload /foo\n"));
                    one(clearTool).update("viewpath", new String[] {"/bar"});
                    one(clearTool).update("viewpath", null);
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"/foo", "/bar"}, true, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
    @Test
    public void testSecondTimeRemovedLoadRule() throws Exception {
        workspace.child("viewpath").mkdirs();

        context.checking(new Expectations() {
                {
                    one(clearTool).doesViewExist("viewname"); will(returnValue(true));
                    one(clearTool).lscurrentview("viewpath"); will(returnValue("viewname"));
                    one(clearTool).catcs("viewname"); will(returnValue("configspec\nload /foo\nload /bar\n"));
                    one(clearTool).setcs("viewpath", SetcsOption.CONFIGSPEC, "configspec\nload /bar\n");
                    one(clearTool).update("viewpath", null);
                    allowing(taskListener).getLogger(); will(returnValue(System.out));
                }
            });
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(launcher).isUnix(); will(returnValue(true));
                    allowing(launcher).getListener(); will(returnValue(taskListener));
                }
            });

        CheckOutAction action = new SnapshotCheckoutAction(clearTool, new ConfigSpec("configspec", true), new String[]{"bar"}, true, "viewpath");
        action.checkout(launcher, workspace, "viewname");
    }
    
}
