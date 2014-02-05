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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.Build;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.BaseSnapshotCheckoutAction;
import hudson.plugins.clearcase.base.BaseHistoryAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Bug;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Node.class, AbstractProject.class })
public class ClearCaseSCMTest extends AbstractWorkspaceTest {

    @Mock
    private Build                               build;

    @Mock
    private ClearCaseSCM.ClearCaseScmDescriptor clearCaseScmDescriptor;
    @Mock
    private ClearTool                           cleartool;
    @Mock
    private ClearToolLauncher                   clearToolLauncher;
    @Mock
    private Computer                            computer;
    @Mock
    private Launcher                            launcher;

    @Mock
    private TaskListener                        taskListener;

    private Node                                node;
    private AbstractProject                     project;

    @Mock
    private ViewStorage                         viewStorage;

    @Test
    public void assertBranchWithSpaceWorks() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch\\ one", "label", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        assertArrayEquals("The branch name array is incorrect", new String[] { "branch one" }, scm.getBranchNames(EMPTY_VARIABLE_RESOLVER));
    }

    @Test
    public void assertConfigSpecCanUseVariables() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("ClearCase");
        when(build.getParent()).thenReturn(project);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getBuildVariables()).thenReturn(new HashMap<Object, Object>());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "ClearCase"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branchone", "label", "${JOB_NAME}", "viewname-${JOB_NAME}", true, "vob", false, "/view", null, false,
                false, false, null, null, false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", viewStorage);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        BaseSnapshotCheckoutAction action = (BaseSnapshotCheckoutAction) scm.createCheckOutAction(variableResolver, clearToolLauncher, build);
        assertEquals("Variables haven't been resolved in config spec", "ClearCase", action.getConfigSpec().getRaw());
    }

    @Test
    public void assertEmptyBranchIsReturnedAsABranch() {
        AbstractClearCaseScm scm = new ClearCaseSCM("", "label", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        assertArrayEquals("The branch name array is incorrect", new String[] { "" }, scm.getBranchNames(EMPTY_VARIABLE_RESOLVER));
    }

    @Test
    public void assertEmptyLabelIsReturnedAsALabel() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        assertArrayEquals("The label name array is incorrect", new String[] { "" }, scm.getLabelNames(EMPTY_VARIABLE_RESOLVER));
    }

    @Test
    public void assertExtendedViewPathUsesNormalizedViewName() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("ClearCase");
        when(build.getBuildVariables()).thenReturn(new HashMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "ClearCase"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(clearCaseScmDescriptor.getLogMergeTimeWindow()).thenReturn(5);
        when(launcher.isUnix()).thenReturn(true);
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(cleartool.pwv("viewpath")).thenReturn("/view/viewpath");

        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branchone", "label", "configspec", "viewname-${JOB_NAME}", true, "vob", true, "/view", null, false,
                false, false, null, null, false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", null);
        // Create actions
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build, null, false);
        assertEquals("The extended view path is incorrect", "/view/viewpath/", action.getExtendedViewPath());
        verify(clearCaseScmDescriptor, atLeastOnce()).getLogMergeTimeWindow();
        verify(cleartool).pwv("viewpath");
    }

    @Test
    public void assertLabelCanUseVariables() throws Exception {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getName()).thenReturn("ClearCase");
        when(build.getParent()).thenReturn(project);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>() {
            {
                put("PLATFORM", "17");
            }
        });
        when(computer.getEnvironment()).thenReturn(new EnvVars("PARALLEL", "YES"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        ClearCaseSCM scm = new ClearCaseSCMDummy("branchone", "${PLATFORM}_REQUEST BUILD_PARALLEL_${PARALLEL}", "configspec", "viewname", true, "vob", false,
                "/view", null, false, false, false, null, null, false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", null);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        assertArrayEquals("Variables haven't been resolved in label", new String[] { "17_REQUEST", "BUILD_PARALLEL_YES" }, scm.getLabelNames(variableResolver));
    }

    @Test
    public void assertLabelRequiresMinorEvents() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getName()).thenReturn("ClearCase");
        when(build.getParent()).thenReturn(project);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        when(computer.getEnvironment()).thenReturn(new EnvVars());
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        VariableResolver<String> variableResolver = new BuildVariableResolver(build);
        Filter filters = scm.configureFilters(variableResolver, build, launcher);
        assertTrue("Minor events are required for label filtering to work", filters.requiresMinorEvents());
    }

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        node = PowerMockito.mock(Node.class);
        project = PowerMockito.mock(AbstractProject.class);
    }

    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testCreateChangeLogParser() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
    }

    @Test
    public void testDynamicBuildEnvVars() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(node.createLauncher(any(TaskListener.class))).thenReturn(launcher);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        when(build.getParent()).thenReturn(project);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "label", "configspec", "viewname", true, "", true, "/views", null, false, false, false, "",
                "", false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", null);
        Map<String, String> env = new HashMap<String, String>();
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWTAG wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWTAG_ENVSTR));
        assertEquals("The env var VIEWNAME wasnt set", "viewpath", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/views/viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVarsNoViewDrive() {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(node.createLauncher(any(TaskListener.class))).thenReturn(launcher);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getParent()).thenReturn(project);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "label", "configspec", "viewname", true, "", true, null, null, false, false, false, "", "",
                false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", null);
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWTAG wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWTAG_ENVSTR));
        assertEquals("The env var VIEWNAME wasnt set", "viewpath", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testGetBranch() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertEquals("The branch isn't correct", "branch", scm.getBranch());
    }

    @Test
    public void testGetBranchNames() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branchone branchtwo", "label", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false,
                false);
        assertArrayEquals("The branch name array is incorrect", new String[] { "branchone", "branchtwo" }, scm.getBranchNames(EMPTY_VARIABLE_RESOLVER));
    }

    @Test
    public void testGetConfigSpec() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertEquals("The config spec isn't correct", "configspec", scm.getConfigSpec());
    }

    @Test
    public void testGetLabel() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertEquals("The label isn't correct", "label", scm.getLabel());
    }

    @Test
    public void testGetLabelNames() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "labelone labeltwo", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        assertArrayEquals("The label name array is incorrect", new String[] { "labelone", "labeltwo" }, scm.getLabelNames(EMPTY_VARIABLE_RESOLVER));
    }

    @Test
    public void testGetViewDrive() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", true, "/tmp/c", null, false, false, false);
        assertEquals("The view drive isn't correct", "/tmp/c", scm.getViewDrive());
    }

    @Test
    public void testGetViewPaths() throws Exception {
        AbstractClearCaseScm scm = new ClearCaseSCM("branchone branchtwo", "label", "configspec", "viewname", true, "load tmp", true, "", null, false, false,
                false);
        assertEquals("The view paths string is incorrect", "tmp", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testIsDynamicView() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", true, "", null, false, false, false);
        assertTrue("The dynamic isn't correct", scm.isUseDynamicView());
        assertFalse("The use update isn't correct", scm.isUseUpdate());
    }

    @Test
    public void testIsUseUpdate() {
        AbstractClearCaseScm scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false);
        assertTrue("The isUpdate isn't correct", scm.isUseUpdate());

        scm = new ClearCaseSCM("branch", "label", "configspec", "viewname", false, "", false, "", null, false, false, false);
        assertFalse("The isUpdate isn't correct", scm.isUseUpdate());
    }

    @Test
    public void testSnapshotBuildEnvVars() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(node.createLauncher(any(TaskListener.class))).thenReturn(launcher);
        when(launcher.isUnix()).thenReturn(true);
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        when(build.getParent()).thenReturn(project);
        when(build.getBuildVariables()).thenReturn(new HashMap<String, String>());
        AbstractClearCaseScm scm = new ClearCaseSCMDummy("branch", "label", "configspec", "viewname", true, "", false, "", null, false, false, false, "", "",
                false, false, cleartool, clearCaseScmDescriptor, computer, "viewpath", null);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWTAG wasn't set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWTAG_ENVSTR));
        assertEquals("The env var VIEWNAME wasn't set", "viewpath", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasn't set", "/hudson/jobs/job/workspace/viewpath",
                env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    @Bug(21626)
    public void configSpecWithParameterShouldntBeConsideredChanged() throws Exception {
        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("","","configspec", "", false, "", false, "", "", false, false, false, "", "", false, false, cleartool, clearCaseScmDescriptor, computer, "", null);
        Map<String, String> m = new HashMap<String,String>();
        m.put("name", "jenkins");
        VariableResolver<String> vr = new VariableResolver.ByMap<String>(m);
        scm.setConfigSpec("Hello ${name}");
        when(cleartool.catcs("")).thenReturn("Hello jenkins");
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(launcher.getListener()).thenReturn(taskListener);
        when(taskListener.getLogger()).thenReturn(System.out);
        assertFalse("config spec should be the same",scm.hasNewConfigSpec(vr, clearToolLauncher));
    }

    @Test
    @Bug(21626)
    public void configSpecLoadedFromAFileWithParameterShouldntBeConsideredChanged() throws Exception {
        ClearCaseSCMBuilder builder = new ClearCaseSCMBuilder();
        builder.viewTag = "";
        builder.configspec = "Hello jenkins";
        builder.extractConfigSpec = true;
        builder.configSpecFileName = "src/test/resources/hudson/plugins/clearcase/configspec.txt";
        builder.cleartool = cleartool;
        ClearCaseSCM scm = builder.build();
        Map<String, String> m = new HashMap<String,String>();
        m.put("name", "jenkins");
        VariableResolver<String> vr = new VariableResolver.ByMap<String>(m);
        when(cleartool.catcs("")).thenReturn("Hello jenkins");
        when(clearToolLauncher.getLauncher()).thenReturn(launcher);
        when(launcher.getListener()).thenReturn(taskListener);
        when(taskListener.getLogger()).thenReturn(System.out);
        assertFalse("config spec should be the same",scm.hasNewConfigSpec(vr, clearToolLauncher));
    }
}
