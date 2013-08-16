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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.plugins.clearcase.action.CheckoutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.PollingResult.Change;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.Entry;
import hudson.util.LogTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Node.class, AbstractProject.class, MatrixProject.class })
public class AbstractClearCaseScmTest extends AbstractWorkspaceTest {

    private class AbstractClearCaseScmDummy extends AbstractClearCaseScm {

        private boolean firstBuild;

        public AbstractClearCaseScmDummy(String viewName, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent, boolean useUpdate,
                boolean rmviewonrename, String excludedRegions, boolean useDynamicView, String viewDrive, String loadRules, String multiSitePollBuffer,
                boolean createDynView, String viewpath) {
            super(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, useUpdate, rmviewonrename, excludedRegions, useDynamicView, viewDrive, false,
                    loadRules, false, null, multiSitePollBuffer, createDynView, createDynView, createDynView, viewpath, ChangeSetLevel.defaultLevel(), null);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam) {
            this(viewName, mkviewOptionalParam, false, false, false, "", false, "", vobPaths, null, false, viewName);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent) {
            this(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, false, false, "", false, "", vobPaths, null, false, viewName);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam, boolean filterOutDestroySubBranchEvent,
                String excludedRegions) {
            this(viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent, false, false, excludedRegions, false, "", vobPaths, null, false, viewName);
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

        @Override
        public ChangeLogParser createChangeLogParser() {
            return null;
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
        public String[] getBranchNames(VariableResolver<String> variableResolver) {
            // TODO Auto-generated method stub
            return branchArray;
        }

        @Override
        public Computer getBuildComputer(AbstractBuild<?, ?> build) {
            return computer;
        }

        @Override
        public Computer getCurrentComputer() {
            return computer;
        }

        @Override
        public SCMDescriptor<?> getDescriptor() {
            throw new IllegalStateException("GetDescriptor() can not be used in tests");
        }

        public void setFirstBuild(boolean firstBuild) {
            this.firstBuild = firstBuild;
        }

        @Override
        protected CheckoutAction createCheckOutAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build) {
            // TODO Auto-generated method stub
            return checkOutAction;
        }

        @Override
        protected HistoryAction createHistoryAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher, AbstractBuild<?, ?> build,
                boolean useRecurse) {
            // TODO Auto-generated method stub
            return historyAction;
        }

        @Override
        protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
            return saveChangeLogAction;
        }

        @Override
        protected boolean hasNewConfigSpec(VariableResolver<String> variableResolver, ClearToolLauncher cclauncher) throws IOException, InterruptedException {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected void inspectConfigAction(VariableResolver<String> variableResolver, ClearToolLauncher launcher) throws IOException, InterruptedException {
            // TODO Auto-generated method stub
        }

        @Override
        protected boolean isFirstBuild(SCMRevisionState baseline) {
            return firstBuild;
        }
    }
    private String[]                          branchArray = new String[] { "branch" };

    @Mock
    private Build                             build;
    @Mock
    private CheckoutAction                    checkOutAction;
    @Mock
    private Computer                          computer;

    @Mock
    private HistoryAction                     historyAction;

    @Mock
    private Launcher                          launcher;

    @Mock
    private TaskListener                      listener;
    private Node                              node;

    private AbstractProject                   project;

    @Mock
    private SaveChangeLogAction               saveChangeLogAction;

    @Mock
    private AbstractClearCaseSCMRevisionState scmRevisionState;

    @Mock
    private BuildListener                     taskListener;

    @Test
    public void assertBuildEnvVarsUsesNormalizedViewName() throws IOException, InterruptedException {

        when(build.getBuiltOn()).thenReturn(node);
        when(build.getProject()).thenReturn(project);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(project.getFullName()).thenReturn("CCHudson");

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname-${JOB_NAME}-${NODE_NAME}", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        env.put("NODE_NAME", "test-node");
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasn't set", "viewname-CCHudson-test-node", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasn't set", "/hudson/jobs/job/workspace" + File.separator + "viewname-CCHudson-test-node",
                env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void assertCheckoutUsesNormalizedViewName() throws Exception {
        workspace.child("viewname-CCHudson").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        when(checkOutAction.isViewValid(workspace, "viewname-CCHudson-test-node")).thenReturn(Boolean.TRUE);
        when(checkOutAction.checkout(launcher, workspace, "viewname-CCHudson-test-node")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(
                historyAction.getChanges(any(Date.class), eq("viewname-CCHudson-test-node"), eq("viewname-CCHudson-test-node"), any(String[].class),
                        any(String[].class))).thenReturn(new ArrayList<ChangeLogSet.Entry>());
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("CCHudson");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "NODE_NAME", "test-node"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(computer.getName()).thenReturn("test-node");
        when(build.getPreviousBuild()).thenReturn(build);
        when(build.getTimestamp()).thenReturn(Calendar.getInstance());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname-${JOB_NAME}-${NODE_NAME}", "vob", "");
        scm.checkout(build, launcher, workspace, taskListener, changelogFile);

        verify(checkOutAction).isViewValid(workspace, "viewname-CCHudson-test-node");
        verify(checkOutAction).checkout(launcher, workspace, "viewname-CCHudson-test-node");
    }

    @Test
    public void assertCheckoutWithChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        final List<Entry> list = new ArrayList<Entry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        when(checkOutAction.isViewValid(workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(checkOutAction.checkout(launcher, workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(historyAction.getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), eq(new String[] { "vob" })))
        .thenReturn(list);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getPreviousBuild()).thenReturn(build, build);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);

        assertTrue("The first time should always return true", hasChanges);
        verify(checkOutAction).isViewValid(workspace, "viewname");
        verify(checkOutAction).checkout(launcher, workspace, "viewname");
        verify(historyAction).getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }),
                eq(new String[] { "vob" }));
        verify(historyAction).getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }),
                eq(new String[] { "vob" }));
        verify(saveChangeLogAction).saveChangeLog(changelogFile, list);
        verify(build, times(2)).getPreviousBuild();
        verify(build).getTimestamp();
    }

    @Test
    public void assertCheckoutWithChangesWithBuffer() throws Exception {
        workspace.child("viewpath").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        final List<ChangeLogSet.Entry> list = new ArrayList<ChangeLogSet.Entry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(1000000);
        final Date bufferedDate = new Date(mockedCalendar.getTimeInMillis() - (1000 * 60 * 5));

        when(checkOutAction.isViewValid(workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(checkOutAction.checkout(launcher, workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);

        when(historyAction.getChanges(eq(bufferedDate), eq("viewpath"), eq("viewname"), eq(new String[] { "branch" }), eq(new String[] { "vob" }))).thenReturn(
                list);

        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());

        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getPreviousBuild()).thenReturn(build);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", false, false, false, "", false, "", "vob", "5", false, "viewpath");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);
        verify(checkOutAction).isViewValid(workspace, "viewname");
        verify(checkOutAction).checkout(launcher, workspace, "viewname");
        verify(historyAction).getChanges(eq(bufferedDate), eq("viewpath"), eq("viewname"), eq(new String[] { "branch" }), eq(new String[] { "vob" }));
        verify(saveChangeLogAction).saveChangeLog(changelogFile, list);
        verify(build, times(2)).getPreviousBuild();
    }

    @Test
    public void assertCheckoutWithMultipleBranches() throws Exception {
        branchArray = new String[] { "branchone", "branchtwo" };
        workspace.child("viewname").mkdirs();

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);
        when(checkOutAction.isViewValid(workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(checkOutAction.checkout(launcher, workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);

        when(
                historyAction.getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branchone", "branchtwo" }),
                        eq(new String[] { "vob" }))).thenReturn(null);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getPreviousBuild()).thenReturn(build);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        verify(checkOutAction).isViewValid(workspace, "viewname");
        verify(checkOutAction).checkout(launcher, workspace, "viewname");
        verify(historyAction).getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branchone", "branchtwo" }),
                eq(new String[] { "vob" }));
    }

    @Test
    public void assertCheckoutWithNoChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        when(checkOutAction.isViewValid(workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(checkOutAction.checkout(launcher, workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);

        when(historyAction.getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), eq(new String[] { "vob" })))
        .thenReturn(null);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getPreviousBuild()).thenReturn(build);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        verify(checkOutAction).isViewValid(workspace, "viewname");
        verify(checkOutAction).checkout(launcher, workspace, "viewname");
        verify(historyAction).getChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }),
                eq(new String[] { "vob" }));
        verify(build, times(2)).getPreviousBuild();
        verify(build).getTimestamp();
    }

    @Test
    public void assertFilteringOutDestroySubBranchEventProperty() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "", true);
        assertTrue("The ClearCase SCM is not filtering out destroy sub branch events", scm.isFilteringOutDestroySubBranchEvent());
    }

    @Test
    public void assertGetModuleRootReturnsViewFolderDynamic() throws Exception {
        // Must initiate a pollChanges() or checkout() to update the normalizedViewName
        createWorkspace();
        final Calendar mockedCalendar = Calendar.getInstance();
        when(taskListener.getLogger()).thenReturn(System.out);
        when(historyAction.hasChanges(any(Date.class), anyString(), anyString(), any(String[].class), any(String[].class))).thenReturn(Boolean.TRUE);

        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        when(build.getParent()).thenReturn(project);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(project.getName()).thenReturn("CCHudson");
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] {});

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view name", null, true, true, true, null, true, "M:", null, null, false, null);
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertEquals("The module root path is incorrect", "view_name", moduleRoot.getName());

        FilePath[] moduleRoots = scm.getModuleRoots(workspace);
        assertEquals("The number of module roots are incorrect", 1, moduleRoots.length);
        assertEquals("The module root path is incorrect", "view_name", moduleRoots[0].getName());
    }

    @Test
    public void assertGetModuleRootReturnsViewFolderSnapshot() throws Exception {
        // Must initiate a pollChanges() or checkout() to update the normalizedViewName
        createWorkspace();
        final Calendar mockedCalendar = Calendar.getInstance();
        when(taskListener.getLogger()).thenReturn(System.out);

        when(historyAction.hasChanges(any(Date.class), anyString(), anyString(), any(String[].class), any(String[].class))).thenReturn(Boolean.TRUE);

        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        when(build.getParent()).thenReturn(project);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(project.getName()).thenReturn("CCHudson");
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] {});

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view name", "", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        FilePath moduleRoot = scm.getModuleRoot(workspace);
        assertEquals("The module root path is incorrect", "view_name", moduleRoot.getName());

        FilePath[] moduleRoots = scm.getModuleRoots(workspace);
        assertEquals("The number of module roots are incorrect", 1, moduleRoots.length);
        assertEquals("The module root path is incorrect", "view_name", moduleRoots[0].getName());
    }

    @Test
    public void assertNormalizedViewNameDoesNotContainInvalidChars() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view  with\\-/-:-?-*-|-,", "vob", "", true);
        assertEquals("The invalid view name chars were not removed from the view name", "view_with_-_-_-_-_-_-,", scm.generateNormalizedViewName(build));
    }

    @Test
    public void assertPollChangesUsesNormalizedViewName() throws Exception {
        createWorkspace();
        when(
                historyAction.hasChanges(any(Date.class), eq("view-MatrixProject_CCHudson-test-node"), eq("view-MatrixProject_CCHudson-test-node"),
                        any(String[].class), any(String[].class))).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("MatrixProject/CCHudson");
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(build.getTimestamp()).thenReturn(Calendar.getInstance());
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(scmRevisionState.getBuildTime()).thenReturn(new Date());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] {});
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("view-${JOB_NAME}-${NODE_NAME}", "vob", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        verify(historyAction).hasChanges(any(Date.class), eq("view-MatrixProject_CCHudson-test-node"), eq("view-MatrixProject_CCHudson-test-node"),
                any(String[].class), any(String[].class));
    }

    @Test
    public void assertViewNameMacrosAreWorking() throws IOException, InterruptedException {

        when(build.getBuiltOn()).thenReturn(node);
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("Hudson");
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson"));

        String username = System.getProperty("user.name").replaceAll(AbstractClearCaseScm.REGEX_WHITESPACE, "_");
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("${JOB_NAME}-${USER_NAME}-${NODE_NAME}-view", "vob", "", true);
        assertEquals("The macros were not replaced in the normalized view name", "Hudson-" + username + "-test-node-view",
                scm.generateNormalizedViewName(build));
    }

    @Test
    public void assertWorkspaceisRequiredForPolling() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertTrue("The ClearCase SCM needs a workspace to poll but is reported no to require one", scm.requiresWorkspaceForPolling());
    }

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        node = PowerMockito.mock(Node.class);
        project = PowerMockito.mock(AbstractProject.class);

        Map<String, String> systemProperties = new HashMap<String, String>();
        systemProperties.put("user.name", "henrik");
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testBuildEnvVars() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator + "viewname",
                env.get(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testBuildEnvVarsNoWorkspaceVar() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        scm.generateNormalizedViewName(build);
        scm.buildEnvVars(build, env);
        assertTrue("The env var VIEWNAME wasnt set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(AbstractClearCaseScm.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testFirstBuild() throws Exception {

        when(checkOutAction.checkout(launcher, workspace, "viewname")).thenReturn(Boolean.TRUE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getPreviousBuild()).thenReturn(null);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() == 0);
    }

    @Test
    public void testGetExcludedRegions() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "", false, "excludedone\nexcludedtwo");
        assertArrayEquals("The excluded regions array is incorrect", new String[] { "excludedone", "excludedtwo" }, scm.getExcludedRegionsNormalized());
    }

    @Test
    public void testGetLoadRules() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "loadrules", null, false,
                "viewpath");
        assertEquals("The load rules arent correct", "loadrules", scm.getLoadRules());
    }

    @Test
    public void testGetLoadRulesWithSpaces() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load rules", null, false,
                "viewpath");
        assertEquals("The load rules arent correct", "load rules", scm.getLoadRules());
    }

    @Test
    public void testGetMkviewOptionalParam() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "extra params");
        assertEquals("The MkviewOptionalParam isnt correct", "extra params", scm.getMkviewOptionalParam());
    }

    @Test
    public void testGetViewName() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertEquals("The view name isnt correct", "viewname", scm.getViewName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetViewNameNonNull() {
        new AbstractClearCaseScmDummy(null, "vob", "");
    }

    @Test
    public void testGetViewPaths() throws Exception {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "loadrules", null, false,
                "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testGetViewPathsLeadingSlash() throws Exception {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "/loadrules", null, false,
                "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testGetViewPathsLeadingSlashAndLoad() throws Exception {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load /loadrules", null, false,
                "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testGetViewPathsNoLoad() throws Exception {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "load loadrules", null, false,
                "viewpath");
        assertEquals("The view paths aren't correct", "loadrules", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testGetViewPathsWithSpaces() throws Exception {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "viewparams", false, false, false, "", false, "", "test rules", null, false,
                "viewpath");
        assertEquals("The view paths aren't correct", "test rules", scm.getViewPaths(null, null, launcher)[0]);
    }

    @Test
    public void testPollChanges() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);
        when(node.getNodeName()).thenReturn("test-node");
        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        scm.setFirstBuild(true);
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);

        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);

    }

    @Test
    public void testPollChangesFirstTime() throws Exception {
        when(project.getSomeBuildWithWorkspace()).thenReturn(null);
        when(taskListener.getLogger()).thenReturn(System.out);
        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);
        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);
        verify(project).getSomeBuildWithWorkspace();
    }

    @Test
    public void testPollChangesMultipleVobPaths() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        when(taskListener.getLogger()).thenReturn(System.out);
        when(
                historyAction.hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), eq(new String[] { "vob1",
                        "vob2/vob2-1", "vob\\ 3" }))).thenReturn(Boolean.TRUE);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getParent()).thenReturn(project);
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] { "vob1", "vob2/vob2-1", "vob\\ 3" });
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob1\nvob2/vob2-1\nvob\\ 3", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        verify(historyAction).hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }),
                eq(new String[] { "vob1", "vob2/vob2-1", "vob\\ 3" }));
        verify(project).getSomeBuildWithWorkspace();
        verify(scmRevisionState).getBuildTime();
    }

    @Test
    public void testPollChangesNoBranch() throws Exception {
        branchArray = new String[] { "" };
        final Calendar mockedCalendar = Calendar.getInstance();
        when(taskListener.getLogger()).thenReturn(System.out);
        when(historyAction.hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "" }), (String[]) isNull())).thenReturn(
                Boolean.FALSE);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(null);
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        verify(historyAction).hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "" }), (String[]) isNull());
        verify(project).getSomeBuildWithWorkspace();
        verify(scmRevisionState).getBuildTime();
    }

    @Test
    public void testPollChangesWithBuffer() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(1000000);
        final Date bufferedDate = new Date(mockedCalendar.getTimeInMillis() - (1000 * 60 * 5));
        when(taskListener.getLogger()).thenReturn(System.out);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(build.getParent()).thenReturn(project);
        AbstractClearCaseScmDummy scm = new AbstractClearCaseScmDummy("viewname", "", false, false, false, "", false, "", "vob", "5", false, "viewpath");
        scm.setFirstBuild(true);
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, null);
        assertEquals("The first time should always have a significant change", PollingResult.BUILD_NOW, pr);
    }

    @Test
    public void testPollChangesWithMatrixProject() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(historyAction.hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), (String[]) isNull()))
        .thenReturn(Boolean.TRUE);

        MatrixBuild matrixBuild = mock(MatrixBuild.class);
        MatrixProject matrixProject = PowerMockito.mock(MatrixProject.class);

        when(matrixBuild.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(matrixBuild.getBuildVariables()).thenReturn(new HashMap<String, String>());
        when(matrixBuild.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(matrixBuild.getParent()).thenReturn(matrixProject);
        when(matrixProject.getSomeBuildWithWorkspace()).thenReturn(matrixBuild);
        when(matrixBuild.getTimestamp()).thenReturn(mockedCalendar);
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(null);
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.compareRemoteRevisionWith(matrixProject, launcher, workspace, taskListener, scmRevisionState);
        verify(historyAction).hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), (String[]) isNull());
        verify(matrixProject).getSomeBuildWithWorkspace();
        verify(scmRevisionState).getBuildTime();
    }

    @Test
    public void testPollChangesWithMultipleBranches() throws Exception {
        branchArray = new String[] { "branchone", "branchtwo" };
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(
                historyAction.hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branchone", "branchtwo" }),
                        eq(new String[] { "vob" }))).thenReturn(Boolean.TRUE);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(build.getParent()).thenReturn(project);
        when(build.getTimestamp()).thenReturn(mockedCalendar);
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] { "vob" });
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);
        assertEquals("The first time should always have a significant change", Change.SIGNIFICANT, pr.change);
        verify(historyAction).hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branchone", "branchtwo" }),
                eq(new String[] { "vob" }));
        verify(project).getSomeBuildWithWorkspace();
        verify(scmRevisionState).getBuildTime();
    }

    @Test
    public void testPollChangesWithNoHistory() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        when(historyAction.hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), any(String[].class)))
        .thenReturn(Boolean.FALSE);
        when(taskListener.getLogger()).thenReturn(System.out);
        when(build.getBuiltOn()).thenReturn(node);
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getProject()).thenReturn(project);
        when(build.getParent()).thenReturn(project);
        when(project.getName()).thenReturn("CCHudson");
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(project.getSomeBuildWithWorkspace()).thenReturn(build);
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "CCHudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getSystemProperties()).thenReturn(System.getProperties());
        when(scmRevisionState.getBuildTime()).thenReturn(mockedCalendar.getTime());
        when(scmRevisionState.getLoadRules()).thenReturn(new String[] { "vob" });
        when(checkOutAction.isViewValid(any(FilePath.class), anyString())).thenReturn(true);

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        PollingResult pr = scm.compareRemoteRevisionWith(project, launcher, workspace, taskListener, scmRevisionState);

        assertEquals("There shouldn't be any change", Change.NONE, pr.change);
        verify(historyAction).hasChanges(eq(mockedCalendar.getTime()), eq("viewname"), eq("viewname"), eq(new String[] { "branch" }), any(String[].class));
        verify(project).getSomeBuildWithWorkspace();
        verify(scmRevisionState).getBuildTime();
    }

    @Test
    public void testSupportsPolling() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertTrue("The ClearCase SCM supports polling but is reported not to", scm.supportsPolling());
    }

    @Test
    public void testViewNameMacrosUsingBuildEnv() throws IOException, InterruptedException {
        when(build.getBuiltOn()).thenReturn(node);
        when(build.getProject()).thenReturn(project);
        when(project.getFullName()).thenReturn("Hudson");
        when(node.toComputer()).thenReturn(computer);
        when(node.getNodeName()).thenReturn("test-node");
        when(build.getBuildVariables()).thenReturn(Collections.emptyMap());
        when(build.getEnvironment(any(LogTaskListener.class))).thenReturn(new EnvVars("JOB_NAME", "Hudson", "TEST_VARIABLE", "result-of-test"));
        when(computer.getEnvironment()).thenReturn(new EnvVars());
        when(computer.getSystemProperties()).thenReturn(System.getProperties());

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("${JOB_NAME}-${TEST_VARIABLE}-view", "vob", "", true);
        assertEquals("The macros were not replaced in the normalized view name", "Hudson-result-of-test-view", scm.generateNormalizedViewName(build));
    }
}
