/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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
import static org.junit.Assert.assertTrue;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.PollingResult.Change;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class AbstractClearCaseScmTest extends AbstractWorkspaceTest {
    private Mockery classContext;
    private Mockery context;

    private BuildListener taskListener;
    private Launcher launcher;
    private AbstractProject project;
    private Build build;
    private Computer computer;
    private Node node;

    private CheckOutAction checkOutAction;
    private HistoryAction historyAction;

    private String[] branchArray = new String[] {"branch"};
    public SaveChangeLogAction saveChangeLogAction;
    private AbstractClearCaseSCMRevisionState scmRevisionState;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new JUnit4Mockery();
        classContext = new JUnit4Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        checkOutAction = context.mock(CheckOutAction.class);
        historyAction = context.mock(HistoryAction.class);
        saveChangeLogAction = context.mock(SaveChangeLogAction.class);
        launcher = classContext.mock(Launcher.class);
        taskListener = context.mock(BuildListener.class);
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
        scmRevisionState = classContext.mock(AbstractClearCaseSCMRevisionState.class);
        computer = classContext.mock(Computer.class);
        node = classContext.mock(Node.class);
        Map<String, String> systemProperties = new HashMap<String, String>();
        systemProperties.put("user.name", "henrik");
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testSupportsPolling() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertTrue("The ClearCase SCM supports polling but is reported not to", scm.supportsPolling());
    }

    @Test
    public void assertWorkspaceisRequiredForPolling() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertTrue("The ClearCase SCM needs a workspace to poll but is reported no to require one", scm.requiresWorkspaceForPolling());
    }

    @Test
    public void assertFilteringOutDestroySubBranchEventProperty() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "", true);
        assertTrue("The ClearCase SCM is not filtering out destroy sub branch events", scm.isFilteringOutDestroySubBranchEvent());
    }

    @Test
    public void testGetViewName() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertEquals("The view name isnt correct", "viewname", scm.getViewName());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetViewNameNonNull() {
        new AbstractClearCaseScmDummy(null, "vob", "");
    }

    @Test
    public void testGetLoadRules() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "loadrules", null, false, "viewpath");
        assertEquals("The load rules arent correct", "loadrules", scm.getLoadRules());
    }

    @Test
    public void testGetLoadRulesWithSpaces() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load rules", null, false, "viewpath");
        assertEquals("The load rules arent correct", "load rules", scm.getLoadRules());
    }

    @Test
    public void testGetViewPaths() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "loadrules", null, false, "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths()[0]);
    }

    @Test
    public void testGetViewPathsWithSpaces() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "test rules", null, false, "viewpath");
        assertEquals("The view paths aren't correct", "test rules", scm.getViewPaths()[0]);
    }

    @Test
    public void testGetViewPathsNoLoad() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load loadrules", null, false, "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths()[0]);
    }

    @Test
    public void testGetViewPathsLeadingSlash() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "/loadrules", null, false, "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths()[0]);
    }

    @Test
    public void testGetViewPathsLeadingSlashAndLoad() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load /loadrules", null, false, "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths()[0]);
    }

    @Test
    public void testGetExcludedRegions() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "", false, "excludedone\nexcludedtwo");
        assertArrayEquals("The excluded regions array is incorrect", new String[] { "excludedone", "excludedtwo" }, scm.getExcludedRegionsNormalized());
    }

    @Test
    public void assertViewNameMacrosAreWorking() throws IOException, InterruptedException {
        context.checking(new Expectations() {
            {
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getBuiltOn(); will(returnValue(node));
                allowing(build).getProject(); will(returnValue(project));
                allowing(project).getName(); will(returnValue("Hudson"));
                one(node).toComputer(); will(returnValue(computer));
                one(node).getNodeName(); will(returnValue("test-node"));
                one(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                one(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                one(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson")));
            }
        });
        String username = System.getProperty("user.name");
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("${JOB_NAME}-${USER_NAME}-${NODE_NAME}-view", "vob", "", true);
        assertEquals("The macros were not replaced in the normalized view name", "Hudson-" + username + "-test-node-view", scm
                .generateNormalizedViewName(build));
    }

    @Test
    public void testViewNameMacrosUsingBuildEnv() throws IOException, InterruptedException {
        classContext.checking(new Expectations() {
            {
                one(build).getBuiltOn(); will(returnValue(node));
                allowing(build).getProject(); will(returnValue(project));
                allowing(project).getName(); will(returnValue("Hudson"));
                one(node).toComputer(); will(returnValue(computer));
                one(node).getNodeName(); will(returnValue("test-node"));
                allowing(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                one(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("${JOB_NAME}-${TEST_VARIABLE}-view", "vob", "", true);
        assertEquals("The macros were not replaced in the normalized view name", "Hudson-result-of-test-view", scm.generateNormalizedViewName(build));
    }

    @Test
    public void assertNormalizedViewNameDoesNotContainInvalidChars() throws IOException, InterruptedException {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view  with\\-/-:-?-*-|-,", "vob", "", true);
        assertEquals("The invalid view name chars were not removed from the view name", "view_with_-_-_-_-_-_-,", scm.generateNormalizedViewName(build));
    }

    @Test
    public void testGetMkviewOptionalParam() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "extra params");
        assertEquals("The MkviewOptionalParam isnt correct", "extra params", scm.getMkviewOptionalParam());
    }

    @Test
    public void testBuildEnvVars() throws IOException, InterruptedException {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator + "viewname", env
                .get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testBuildEnvVarsNoWorkspaceVar() throws IOException, InterruptedException {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertTrue("The env var VIEWNAME wasnt set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void assertBuildEnvVarsUsesNormalizedViewName() throws IOException, InterruptedException {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                allowing(build).getProject(); will(returnValue(project));
                allowing(project).getName(); will(returnValue("CCHudson"));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                allowing(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname-${JOB_NAME}-${NODE_NAME}", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        env.put("NODE_NAME", "test-node");
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasn't set", "viewname-CCHudson-test-node", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasn't set", "/hudson/jobs/job/workspace" + File.separator + "viewname-CCHudson-test-node", env
                .get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testFirstBuild() throws Exception {
        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname");
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                one(build).getPreviousBuild(); will(returnValue(null));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
    }

    @Test
    public void assertCheckoutWithChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");
        
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname"); will(returnValue(true));

                // normal changelog
                one(historyAction).getChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(list));
                one(saveChangeLogAction).saveChangeLog(changelogFile, list);

            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                // normal changelog
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);
    }

    @Test
    public void assertCheckoutWithChangesWithBuffer() throws Exception {
        workspace.child("viewpath").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");
        
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(1000000);
        final Date bufferedDate = new Date(mockedCalendar.getTimeInMillis() - (1000 * 60 * 5));

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname"); will(returnValue(true));

                // normal changelog
                one(historyAction).getChanges(with(equal(bufferedDate)), with(equal("viewpath")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(list));
                one(saveChangeLogAction).saveChangeLog(changelogFile, list);

            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                // normal changelog
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                allowing(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", false, false, false, "", false, "", "vob", "5", false, "viewpath");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);
    }

    @Test
    public void assertCheckoutUsesNormalizedViewName() throws Exception {
        workspace.child("viewname-CCHudson").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname-CCHudson-test-node"); will(returnValue(true));
                ignoring(historyAction).getChanges(with(any(Date.class)), with(equal("viewname-CCHudson-test-node")), with(equal("viewname-CCHudson-test-node")), with(any(String[].class)), with(any(String[].class)));
                will(returnValue(new ArrayList<ClearCaseChangeLogEntry>()));

            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                allowing(build).getProject(); will(returnValue(project));
                allowing(project).getName(); will(returnValue("CCHudson"));
                allowing(build).getBuildVariables(); will(returnValue(new HashMap()));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "CCHudson", "NODE_NAME", "test-node")));
                allowing(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                allowing(computer).getName(); will(returnValue("test-node"));
                // normal changelog
                ignoring(build).getPreviousBuild(); will(returnValue(build));
                ignoring(build).getTimestamp(); will(returnValue(Calendar.getInstance()));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname-${JOB_NAME}-${NODE_NAME}", "vob", "");
        scm.checkout(build, launcher, workspace, taskListener, changelogFile);
    }

    @Test
    public void assertCheckoutWithNoChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname"); will(returnValue(true));
                one(historyAction).getChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                ignoring(build).getEnvironment(with(any(TaskListener.class))); will(returnValue(new EnvVars()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
    }

    @Test
    public void assertCheckoutWithMultipleBranches() throws Exception {
        branchArray = new String[] { "branchone", "branchtwo" };
        workspace.child("viewname").mkdirs();

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace, "viewname"); will(returnValue(true));
                one(historyAction).getChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")),
                        with(equal("viewname")), with(equal(new String[] { "branchone", "branchtwo" })), with(equal(new String[] { "vob" })));
                will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getPreviousBuild(); will(returnValue(build));
                ignoring(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                ignoring(build).getEnvironment(with(any(TaskListener.class))); will(returnValue(new EnvVars()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
    }

    @Test
    public void testPollChanges() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {

                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
                allowing(build).getTimestamp(); will(returnValue(mockedCalendar));
                allowing(computer).getName(); will(returnValue("test-node"));
                one(project).getLastBuild(); will(returnValue(build));
                one(build).getPreviousBuild(); will(returnValue(build));
            }
        });
        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        scm.setFirstBuild(true);
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);
        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);
    }

    @Test
    public void testPollChangesWithBuffer() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(1000000);
        final Date bufferedDate = new Date(mockedCalendar.getTimeInMillis() - (1000 * 60 * 5));
        context.checking(new Expectations() {
            {

                one(historyAction).hasChanges(with(equal(bufferedDate)), with(equal("viewpath")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
                allowing(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });
        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "", false, false, false, "", false, "", "vob", "5", false, "viewpath");
        scm.setFirstBuild(true);
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);
        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);
    }

    @Test
    public void assertPollChangesUsesNormalizedViewName() throws Exception {
        createWorkspace();
        context.checking(new Expectations() {
            {

                one(historyAction).hasChanges(with(any(Date.class)), with(equal("view-CCHudson-test-node")), with(equal("view-CCHudson-test-node")),
                        with(any(String[].class)), with(any(String[].class)));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                allowing(build).getProject(); will(returnValue(project));
                allowing(project).getName(); will(returnValue("CCHudson"));
                ignoring(project).getLastBuild(); will(returnValue(build));
                ignoring(build).getTimestamp(); will(returnValue(Calendar.getInstance()));
                allowing(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                allowing(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "CCHudson")));
                allowing(scmRevisionState).getBuildTime(); will(returnValue(new Date()));
                allowing(computer).getSystemProperties(); will(returnValue(System.getProperties()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view-${JOB_NAME}-${NODE_NAME}", "vob", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
    }

    @Test
    public void testPollChangesFirstTime() throws Exception {
        classContext.checking(new Expectations() {
            {
                one(project).getLastBuild();
                will(returnValue(null));
            }
        });

        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);
        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);
    }

    @Test
    public void testPollChangesWithNoHistory() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {

                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob" })));
                will(returnValue(false));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        assertEquals("There shouldn't be any change", Change.NONE, pr.change);
    }

    @Test
    public void testPollChangesWithMultipleBranches() throws Exception {
        branchArray = new String[] { "branchone", "branchtwo" };
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")),
                        with(equal("viewname")), with(equal(new String[] { "branchone", "branchtwo" })), with(equal(new String[] { "vob" })));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        assertEquals("The first time should always have a significant change", Change.SIGNIFICANT, pr.change);
    }

    @Test
    public void testPollChangesMultipleVobPaths() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(equal(new String[] { "vob1", "vob2/vob2-1", "vob\\ 3" })));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob1\nvob2/vob2-1\nvob\\ 3", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
    }

    @Test
    public void testPollChangesNoBranch() throws Exception {
        branchArray = new String[] { "" };
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "" })), with(aNull(String[].class)));
                will(returnValue(false));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
    }

    @Test
    public void testPollChangesWithMatrixProject() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        context.checking(new Expectations() {
            {
                one(historyAction).hasChanges(with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal("viewname")),
                        with(equal(new String[] { "branch" })), with(aNull(String[].class)));
                will(returnValue(true));
            }
        });
        final MatrixBuild matrixBuild = classContext.mock(MatrixBuild.class);
        classContext.checking(new Expectations() {
            {
                ignoring(matrixBuild).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(matrixBuild).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(matrixBuild).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(matrixBuild).getParent(); will(returnValue(project));
                one(project).getLastBuild(); will(returnValue(matrixBuild));
                one(matrixBuild).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(build).addAction(with(any(ClearCaseDataAction.class)));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
    }
    
    @Test
    public void assertGetModuleRootReturnsViewFolderSnapshot() throws Exception {
        // Must initiate a pollChanges() or checkout() to update the normalizedViewName
        createWorkspace();
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                ignoring(historyAction).hasChanges(with(any(Date.class)), with(any(String.class)), with(any(String.class)), with(any(String[].class)), with(any(String[].class)));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(project).getLastBuild(); will(returnValue(build));
                ignoring(project).getName(); will(returnValue("CCHudson"));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view name", "", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertEquals("The module root path is incorrect", "view_name", moduleRoot.getName());

        FilePath[] moduleRoots = scm.getModuleRoots(workspace);
        assertEquals("The number of module roots are incorrect", 1, moduleRoots.length);
        assertEquals("The module root path is incorrect", "view_name", moduleRoots[0].getName());
    }
    
    @Test
    public void assertGetModuleRootReturnsViewFolderDynamic() throws Exception {
        // Must initiate a pollChanges() or checkout() to update the normalizedViewName
        createWorkspace();
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                ignoring(historyAction).hasChanges(with(any(Date.class)), with(any(String.class)), with(any(String.class)), with(any(String[].class)), with(any(String[].class)));
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getBuiltOn(); will(returnValue(node));
                ignoring(node).toComputer(); will(returnValue(computer));
                ignoring(node).getNodeName(); will(returnValue("test-node"));
                ignoring(build).getBuildVariables(); will(returnValue(new HashMap<String, String>()));
                ignoring(build).getEnvironment(with(any(LogTaskListener.class))); will(returnValue(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test")));
                ignoring(computer).getSystemProperties(); will(returnValue(System.getProperties()));
                
                ignoring(build).getParent(); will(returnValue(project));
                ignoring(build).getTimestamp(); will(returnValue(mockedCalendar));
                ignoring(project).getLastBuild(); will(returnValue(build));
                ignoring(project).getName(); will(returnValue("CCHudson"));
                one(scmRevisionState).getBuildTime(); will(returnValue(mockedCalendar.getTime()));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view name", null, true, true, true, null, true, "M:", null, null, false, null);
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertEquals("The module root path is incorrect", "view_name", moduleRoot.getName());

        FilePath[] moduleRoots = scm.getModuleRoots(workspace);
        assertEquals("The number of module roots are incorrect", 1, moduleRoots.length);
        assertEquals("The module root path is incorrect", "view_name", moduleRoots[0].getName());
    }
    
    private class AbstractClearCaseScmDummy extends AbstractClearCaseScm {

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent) {
            this(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, false, false, "", false, "", vobPaths, null, false, viewName);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent,
                String excludedRegions) {
            this(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, false, false, excludedRegions, false, "", vobPaths, null, false, viewName);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam) {
            this(viewName, mkviewOptionalParam, false, false, false, "", false, "", vobPaths, null, false, viewName);
        }

        public AbstractClearCaseScmDummy(String viewName, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent, boolean useUpdate,
                boolean rmviewonrename, String excludedRegions, boolean useDynamicView, String viewDrive, String loadRules, String multiSitePollBuffer,
                boolean createDynView, String viewpath) {
            super(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, excludedRegions, useDynamicView, viewDrive,
                    loadRules, multiSitePollBuffer, createDynView, "", "", createDynView, createDynView, viewpath);
        }

        @Override
        public Computer getCurrentComputer() {
            return computer;
        }

        @Override
        public Computer getBuildComputer(AbstractBuild<?, ?> build) {
            return computer;
        }

        @Override
        public SCMDescriptor<?> getDescriptor() {
            throw new IllegalStateException("GetDescriptor() can not be used in tests");
        }

        @Override
        public ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
            return null;
        }

        @Override
        public String[] getBranchNames() {
            return branchArray;
        }

        @Override
        protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
            return saveChangeLogAction;
        }

        @Override
        public ChangeLogParser createChangeLogParser() {
            return null;
        }

        @Override
        protected CheckOutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
            // TODO Auto-generated method stub
            return checkOutAction;
        }

        @Override
        protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
            // TODO Auto-generated method stub
            return historyAction;
        }

        @Override
        public String[] getBranchNames(VariableResolver<String> variableResolver) {
            // TODO Auto-generated method stub
            return branchArray;
        }

        @Override
        public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> arg0, Launcher arg1, TaskListener arg2) throws IOException, InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public SCMRevisionState calcRevisionsFromPoll(AbstractBuild<?, ?> build, Launcher launcher, TaskListener taskListener) throws IOException,
                InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }
        
        private boolean firstBuild;

        @Override
        protected boolean isFirstBuild(SCMRevisionState baseline) {
            return firstBuild;
        }
        
        public void setFirstBuild(boolean firstBuild) {
            this.firstBuild = firstBuild;
        }
    }
}
