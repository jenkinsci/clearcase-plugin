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
package hudson.plugins.clearcase;

import static org.junit.Assert.assertFalse;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.VariableResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the cleartool snapshot view
 */
public class ClearToolSnapshotTest extends AbstractWorkspaceTest {

    private Mockery context;
    private Mockery classContext;
    private ClearTool clearToolExec;
    private ClearToolLauncher clearToolLauncher;
    private VariableResolver resolver;
    private TaskListener listener;
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
        
        clearToolLauncher = context.mock(ClearToolLauncher.class);
        listener = context.mock(TaskListener.class);
        resolver = context.mock(VariableResolver.class);
        clearToolExec = new ClearToolSnapshot(resolver, clearToolLauncher);
        
        launcher = classContext.mock(Launcher.class);
    }
    
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testRemoveView() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "rmview", "-force",
                                                                         "viewName" })), with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.rmview("viewName");
    }
    
    @Test
    public void testForcedRemoveView() throws Exception {
        workspace.child("viewName").mkdirs();
        
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "rmview", "-force",
                                                                         "viewName" })), with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                    one(clearToolLauncher).getListener();
                    will(returnValue(listener));
                    one(listener).getLogger();
                    will(returnValue(new PrintStream(new ByteArrayOutputStream())));
                }
            });
        
        clearToolExec.rmview("viewName");
        assertFalse("View folder still exists", workspace.child("viewName")
                    .exists());
    }
    
    @Test
    public void testUpdate() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "update", "-force", "-overwrite", "-log",
                                                                         "NUL" })),
                                               with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", null);
    }

    @Test
    public void testSetcsCurrent() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "setcs", "-current" })),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.setcs("viewName", null);
    }

    @Test(expected=IOException.class)
    public void testUpdateBlocked() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "update", "-force", "-overwrite", "-log",
                                                                         "NUL"})),
                                               with(aNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolSnapshotTest.class.getResourceAsStream("ct-update-2.log")),
                               returnValue(Boolean.TRUE)));
                }
            });
        
        clearToolExec.update("viewName", null);
    }

    @Test(expected=IOException.class)
    public void testSetcsCurrentBlocked() throws Exception {
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getWorkspace();
                    will(returnValue(workspace));
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "setcs", "-current" })),
                                               with(aNonNull(InputStream.class)),
                                               with(aNonNull(OutputStream.class)),
                                               with(aNonNull(FilePath.class)));
                    will(doAll(new StreamCopyAction(2, ClearToolSnapshotTest.class.getResourceAsStream("ct-update-2.log")),
                               returnValue(Boolean.TRUE)));
                }
            });

        clearToolExec.setcs("viewName", null);
    }

    
    @Test
    public void testUpdateWithLoadRulesWindows() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(false));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher(); will(returnValue(launcher));
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "more_load_rules" })),
                             with(aNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"\\more_load_rules"});
    }

        @Test
    public void testUpdateWithLoadRules() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(true));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher(); will(returnValue(launcher));
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "more_load_rules" })),
                             with(aNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"/more_load_rules"});
    }

    @Test
    public void testUpdateWithLoadRulesWithSpace() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(true));
                }
            });
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher(); will(returnValue(launcher));
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    one(clearToolLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "\"more load_rules\"" })),
                             with(aNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"/more load_rules"});
    }

    @Test
    public void testUpdateWithLoadRulesWithSpaceWin() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(launcher).isUnix(); will(returnValue(false));
                }
            });
        
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).getWorkspace(); will(returnValue(workspace));
                    allowing(clearToolLauncher).getLauncher(); will(returnValue(launcher));
                    one(clearToolLauncher)
                        .run(
                             with(equal(new String[] {
                                         "update",
                                         "-force",
                                         "-overwrite",
                                         "-log",
                                         "NUL",
                                         "-add_loadrules",
                                         "\"more load_rules\"" })),
                             with(aNull(InputStream.class)),
                             with(aNonNull(OutputStream.class)),
                             with(aNonNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.update("viewName", new String[] {"\\more load_rules"});
    }
    
    @Test
    public void testCreateView() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "viewName" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        clearToolExec.mkview("viewName", null);
    }
    
    @Test
    public void testCreateViewWithStream() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-stream", "streamSelector", "-tag",
                                                                         "viewName", "viewName" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec.mkview("viewName", "streamSelector");
    }

    @Test
    public void testCreateViewExtraParams() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "-anextraparam",
                                                                         "-anotherparam", "viewName" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec = new ClearToolSnapshot(resolver, clearToolLauncher,
                                              "-anextraparam -anotherparam");
        clearToolExec.mkview("viewName", null);
    }

    @Test
    public void testCreateUcmViewWithOptionalParams() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-stream", "streamSelector", "-tag",
                                                                         "viewName", "-anextraparam", "-anotherparam",
                                                                         "viewName" })), with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });

        clearToolExec = new ClearToolSnapshot(resolver, clearToolLauncher,
                                              "-anextraparam -anotherparam");
        clearToolExec.mkview("viewName", "streamSelector");
    }

    @Test
    public void testCreateViewExtraParamsEvaluated() throws Exception {
        context.checking(new Expectations() {
                {
                    one(clearToolLauncher).run(
                                               with(equal(new String[] { "mkview", "-snapshot",
                                                                         "-tag", "viewName", "-anextraparam",
                                                                         "Test", "viewName" })),
                                               with(aNull(InputStream.class)),
                                               with(aNull(OutputStream.class)),
                                               with(aNull(FilePath.class)));
                    will(returnValue(Boolean.TRUE));
                }
            });
        
        context.checking(new Expectations() {
                {
                    atLeast(1).of(resolver).resolve("COMPUTERNAME");
                    will(returnValue("Test"));
                }
            });
        clearToolExec = new ClearToolSnapshot(resolver, clearToolLauncher,
                                              "-anextraparam $COMPUTERNAME");
        clearToolExec.mkview("viewName", null);
    }
}
