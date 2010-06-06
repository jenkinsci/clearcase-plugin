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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Computer;
import hudson.plugins.clearcase.ucm.UcmHistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.util.HashMap;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class ClearCaseUcmSCMTest extends AbstractWorkspaceTest {
    private Mockery classContext;
    private Mockery context;
    private ClearTool cleartool;
    private AbstractProject project;
    private Build build;
    private Launcher launcher;
    private ClearToolLauncher clearToolLauncher;
    private Computer computer;
    private ClearCaseUcmSCM.ClearCaseUcmScmDescriptor clearCaseUcmScmDescriptor;
    
    @Before
    public void setUp() throws Exception {
        createWorkspace();
        classContext = new JUnit4Mockery() {
                {
                    setImposteriser(ClassImposteriser.INSTANCE);
                }
            };
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
        launcher = classContext.mock(Launcher.class);
        computer = classContext.mock(Computer.class);
        clearCaseUcmScmDescriptor = classContext.mock(ClearCaseUcmSCM.ClearCaseUcmScmDescriptor.class);
        context = new JUnit4Mockery();
        cleartool = context.mock(ClearTool.class);
        clearToolLauncher = context.mock(ClearToolLauncher.class);
        
    }
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testCreateChangeLogParser() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "", false, null, null, false, false, false);
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
    }
    
    @Test
    public void testGetStream() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("The stream isn't correct", "stream", scm.getStream());
    }

    @Test
    public void testGetOverrideBranchName() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "override-branch", false, null, null, false, false, false);
        assertEquals("The override branch isn't correct", "override-branch", scm.getOverrideBranchName());
    }

    @Test
    public void testGetBranchNamesDefault() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "", false, null, null, false, false, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    @Test
    public void testGetBranchNamesWithOverride() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "override-branch", false, null, null, false, false, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"override-branch"}, scm.getBranchNames());
    }

    /**
     * The stream cant be used as a branch name directly if it contains a vob selector.
     * cleartool lshistory -r <snip/> -branch brtype:shared_development_2_1@/vobs/UCM_project 
     * cleartool: Error: Object is in unexpected VOB: "brtype:shared_development_2_1@/vobs/UCM_project".
     */
    @Test
    public void testGetBranchNamesWithVobSelector() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream@/vob/paths", "loadrules", "viewname", false, "viewdrive",
                                                  "option", false, false, false, "", null, "", false, null, null, false, false, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    @Test
    public void testGetViewPaths() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", false, "viewdrive", "option",
                                                  false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("The view path is not the same as the load rules", "loadrules", scm.getViewPaths()[0]);
    }
    
    /**
     * Test for (issue 1706).
     * VOBPaths are used by the lshistory command, and should not start with a 
     * "\\" or "/" as that would make the cleartool command think the view is
     * located by an absolute path and not an relative path.
     */
    @Test
    public void assertLoadRuleIsConvertedToRelativeViewPath() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\\\loadrule\\one\n/loadrule/two", "viewname", false,
                                                  "viewdrive", "option", false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("The first view path is not correct", "loadrule\\one", scm.getViewPaths()[0]);
        assertEquals("The second view path is not correct", "loadrule/two", scm.getViewPaths()[1]);
    }
    
    /**
     * Test for (issue 1706) with Dynamic view.
     * VOBPaths are used by the lshistory command, and should not start with a 
     * "\\" or "/" as that would make the cleartool command think the view is
     * located by an absolute path and not an relative path.
     *
     * 7/10/09, abayer - Commenting out for now, because I can't actually see why we'd want this behavior to differ for dynamic views.
     */
    /*    @Test
          public void assertLoadRuleIsConvertedToRelativeViewPathIfNotDynamic() throws Exception {
          ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\\\loadrule\\one\n/loadrule/two", "viewname", true, "viewdrive", "option", false, false, false);
          assertEquals("The first view path is not correct", "\\\\loadrule\\one", scm.getViewPaths()[0]);
          assertEquals("The second view path is not correct", "/loadrule/two", scm.getViewPaths()[1]);
          }*/

    @Test
    public void testGetVobPathsWithSpaces() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "file with space\nanotherfile", "viewname", false,
                                                  "viewdrive", "option", false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("The vob path is not the same as the load rules", "file with space", scm.getViewPaths()[0]);
        assertEquals("The vob path is not the same as the load rules", "anotherfile", scm.getViewPaths()[1]);
    }
    
    /**
     * Test for (issue 1707).
     * If the UCM load rule field is set to this, then path used in lshistory will be "\projectdata".
     * Somehow the "\" char is removed, and "\\" replaced with "\".
     */
    @Test
    public void testGetWindowsVobPaths() throws Exception {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\ \\ Windows\n\\\\C\\System\\\\32", "viewname", false,
                                                  "viewdrive", "option", false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("The vob path is not the same as the load rules", " \\ Windows", scm.getViewPaths()[0]);
        assertEquals("The vob path is not the same as the load rules", "C\\System\\\\32", scm.getViewPaths()[1]);
    }  

    @Test 
        public void testShortenStreamName() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream:mystream", "file with space\nanotherfile", "viewname", false,
                                                  "viewdrive", "option", false, false, false, "", null, "", false, null, null, false, false, false);
        assertEquals("stream name not shortenen correctly", "mystream",scm.getStream());
    }

    @Test
    public void assertExtendedViewPathUsesNormalizedViewName() throws Exception {
        classContext.checking(new Expectations() {
                {
                    atLeast(1).of(build).getParent(); will(returnValue(project));
                    allowing(launcher).isUnix(); will(returnValue(true));
                    allowing(build).getBuildVariables(); will(returnValue(new HashMap()));
                    allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "ClearCase")));
                    allowing(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                }
            });
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher();
                    will(returnValue(launcher));
                    one(cleartool).pwv("viewname-ClearCase");
                    will(returnValue("/view/viewname-ClearCase"));
                }
            });
        
        ClearCaseUcmSCM scm = new ClearCaseUcmSCMDummy("stream:mystream", "somefile", "viewname-${JOB_NAME}", true, "/view",
                                                       null, true, false, false, null, null, null, false, cleartool,
                                                       clearCaseUcmScmDescriptor);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());
        UcmHistoryAction action = (UcmHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);
        assertEquals("The extended view path is incorrect", "/view/viewname-ClearCase/", action.getExtendedViewPath());
    }
}
