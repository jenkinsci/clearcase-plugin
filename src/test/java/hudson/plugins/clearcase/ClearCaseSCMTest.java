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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.Computer;
import hudson.plugins.clearcase.action.SnapshotCheckoutAction;
import hudson.plugins.clearcase.base.BaseHistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearCaseSCMTest extends AbstractWorkspaceTest {

    private Mockery classContext;
    private Mockery context;
    private ClearTool cleartool;
    private AbstractProject project;
    private Build build;
    private Launcher launcher;
    private ClearToolLauncher clearToolLauncher;
    private ClearCaseSCM.ClearCaseScmDescriptor clearCaseScmDescriptor;
    private Computer computer;
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
        clearCaseScmDescriptor = classContext.mock(ClearCaseSCM.ClearCaseScmDescriptor.class);
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
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
    }
    
    @Test
    public void testSnapshotBuildEnvVars() {
        classContext.checking(new Expectations() {
                {
                    ignoring(build).getParent(); will(returnValue(project));
                }
            });
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "configspec", "viewname", true, "", false, "",
                                                         null, false, false,false, "", "", false, false, cleartool,
                                                         clearCaseScmDescriptor, computer);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator +"viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVars() {
        classContext.checking(new Expectations() {
                {
                    ignoring(build).getParent(); will(returnValue(project));
                }
            });
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "configspec", "viewname", true, "", true, "/views",
                                                         null, false, false,false, "", "", false, false, cleartool,
                                                         clearCaseScmDescriptor, computer);
        Map<String, String> env = new HashMap<String, String>();
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/views" + File.separator +"viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVarsNoViewDrive() {
        classContext.checking(new Expectations() {
                {
                    ignoring(build).getParent(); will(returnValue(project));
                }
            });
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "configspec", "viewname", true, "", true, null,
                                                         null, false, false,false, "", "", false, false, cleartool,
                                                         clearCaseScmDescriptor, computer);
        Map<String, String> env = new HashMap<String, String>();
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testGetBranch() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false, false,false);
        assertEquals("The branch isnt correct", "branch", scm.getBranch());
    }
    
    @Test
    public void testGetConfigSpec() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false, false,false);
        assertEquals("The config spec isnt correct", "configspec", scm.getConfigSpec());
    }

    @Test
    public void testIsUseUpdate() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false, false,false);
        assertTrue("The isUpdate isnt correct", scm.isUseUpdate());
        
        scm = new ClearCaseSCM("branch", "configspec", "viewname", false, "", false, "", null, false, false,false);
        assertFalse("The isUpdate isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testIsDynamicView() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "", null, false, false,false);
        assertTrue("The dynamic isnt correct", scm.isUseDynamicView());
        assertFalse("The use update isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testGetViewDrive() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false,false);
        assertEquals("The view drive isnt correct", "/tmp/c", scm.getViewDrive());
    }

    @Test
    public void testGetBranchNames() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false,false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"branchone", "branchtwo"}, scm.getBranchNames());
    }

    @Test
    public void assertEmptyBranchIsReturnedAsABranch() {
        AbstractClearCaseScm scm = new ClearCaseSCM("", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false,false);
        assertArrayEquals("The branch name array is incorrect", new String[]{""}, scm.getBranchNames());
    }

    @Test
    public void assertBranchWithSpaceWorks() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch\\ one", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false,false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"branch one"}, scm.getBranchNames());
    }

    @Test
    public void testGetViewPaths() throws Exception {
        AbstractClearCaseScm scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "load tmp", true, "", null, false, false,false);
        assertEquals("The view paths string is incorrect", "tmp", scm.getViewPaths()[0]);
    }

    @Test
    public void assertExtendedViewPathUsesNormalizedViewName() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(build).getBuildVariables(); will(returnValue(new HashMap()));
                    allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "ClearCase")));
                    allowing(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                    atLeast(1).of(build).getParent(); will(returnValue(project));
                    atLeast(1).of(clearCaseScmDescriptor).getLogMergeTimeWindow(); will(returnValue(5));
                    allowing(launcher).isUnix(); will(returnValue(true));
                }
            });
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher();
                    will(returnValue(launcher));
                    one(cleartool).pwv("viewname-ClearCase");
                    will(returnValue("/view/viewname-ClearCase"));
                    allowing(cleartool).startView("viewname-ClearCase");
                    allowing(cleartool).mountVobs();
                }
            });
        
        ClearCaseSCM scm = new ClearCaseSCMDummy("branchone", "configspec", "viewname-${JOB_NAME}", true, "vob",
                                                 true, "/view", null, false, false, false, null, null,
                                                 false, false, cleartool, clearCaseScmDescriptor, computer);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);
        assertEquals("The extended view path is incorrect", "/view/viewname-ClearCase/", action.getExtendedViewPath());
    }

    @Test
    public void assertConfigSpecCanUseVariables() throws Exception {
        classContext.checking(new Expectations() {
            {
                allowing(build).getParent(); will(returnValue(project));
                allowing(launcher).isUnix(); will(returnValue(true));
                allowing(build).getBuildVariables(); will(returnValue(new HashMap()));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "ClearCase")));
                allowing(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        context.checking(new Expectations() {
            {
                allowing(clearToolLauncher).getLauncher(); will(returnValue(launcher));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCMDummy("branchone", "${JOB_NAME}", "viewname-${JOB_NAME}", true, "vob", false, "/view", null, false, false, false,
                null, null, false, false, cleartool, clearCaseScmDescriptor, computer);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());
        SnapshotCheckoutAction action = (SnapshotCheckoutAction) scm.createCheckOutAction(variableResolver, clearToolLauncher, build);
        assertEquals("Variables haven't been resolved in config spec", "ClearCase", action.getConfigSpec().getRaw());
    }
}
