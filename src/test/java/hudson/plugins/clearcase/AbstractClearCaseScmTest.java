package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.matrix.MatrixBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.PollAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.action.TaggingAction;
import hudson.plugins.clearcase.util.EventRecordFilter;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCMDescriptor;

import org.jmock.Expectations;
import org.jmock.Mockery;
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
    
    private EventRecordFilter filter;
    
    private CheckOutAction checkOutAction;
    private PollAction pollAction;

    private String[] branchArray = new String[] {"branch"};
    public ChangeLogAction changeLogAction;
    public SaveChangeLogAction saveChangeLogAction;
    
    @Before
    public void setUp() throws Exception {
        createWorkspace();
        context = new Mockery();
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        checkOutAction = context.mock(CheckOutAction.class);
        pollAction = context.mock(PollAction.class);
        saveChangeLogAction = context.mock(SaveChangeLogAction.class);
        changeLogAction = context.mock(ChangeLogAction.class);
        launcher = classContext.mock(Launcher.class);
        taskListener = context.mock(BuildListener.class);
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
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
    public void assertFilteringOutDestroySubBranchEventProperty() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "", true);
        assertTrue("The ClearCase SCM is not filtering out destroy sub branch events", scm.isFilteringOutDestroySubBranchEvent());
    }

    @Test
    public void testGetViewName() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        assertEquals("The view name isnt correct", "viewname", scm.getViewName());
    }

    @Test
    public void testGetViewNameNonNull() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy(null, "vob", "");
        assertNotNull("The view name can not be null", scm.getViewName());
    }

    @Test
    public void testGetMkviewOptionalParam() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "extra params");
        assertEquals("The MkviewOptionalParam isnt correct", "extra params", scm.getMkviewOptionalParam());
    }

    @Test
    public void testBuildEnvVars() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.buildEnvVars(null, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator +"viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }
    
    @Test
    public void testBuildEnvVarsNoViewName() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy(null, "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.buildEnvVars(null, env);
        assertFalse("The env var VIEWNAME was set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testBuildEnvVarsNoWorkspaceVar() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(null, env);
        assertTrue("The env var VIEWNAME wasnt set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }
    
    @Test
    public void testFirstBuild() throws Exception {
        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace); will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getPreviousBuild(); will(returnValue(null));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void assertCheckoutWithChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");
        final File extendedChangelogFile = new File(parentFile, "extended-changelog.xml");
        
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace); 
                will(returnValue(true));
                    
                // normal changelog
                one(changeLogAction).getChanges(with(any(EventRecordFilter.class)), with(equal(mockedCalendar.getTime())), with(equal("viewname")), with(equal(new String[] {"branch"})), with(equal(new String[]{"vob"})));
                    will(returnValue(list));
                one(saveChangeLogAction).saveChangeLog(changelogFile, list);
                
            }
        });
        classContext.checking(new Expectations() {
            {
                // normal changelog
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void assertCheckoutWithNoChanges() throws Exception {
        workspace.child("viewname").mkdirs();
        final File changelogFile = new File(parentFile, "changelog.xml");
        
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {   
                one(checkOutAction).checkout(launcher, workspace); 
                    will(returnValue(true));
                one(changeLogAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[] {"branch"})), 
                        with(equal(new String[]{"vob"})));
                    will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void assertCheckoutWithMultipleBranches() throws Exception {
        branchArray = new String[]{"branchone", "branchtwo"};
        workspace.child("viewname").mkdirs();

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {   
                one(checkOutAction).checkout(launcher, workspace);
                one(changeLogAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[] {"branchone", "branchtwo"})), 
                        with(equal(new String[]{"vob"})));
                    will(returnValue(null));
            }
        });
        classContext.checking(new Expectations() {
            {
                ignoring(build).getPreviousBuild(); will(returnValue(build));
                ignoring(build).getTimestamp(); will(returnValue(mockedCalendar));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testPollChanges() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[] {"branch"})), 
                        with(equal(new String[]{"vob"}))); 
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertTrue("The first time should always return true", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesFirstTime() throws Exception {
        classContext.checking(new Expectations() {
            {
                one(project).getLastBuild(); will(returnValue(null));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertTrue("The first time should always return true", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesWithNoHistory() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[]{"branch"})), 
                        with(equal(new String[]{"vob"}))); 
                will(returnValue(false));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertFalse("pollChanges() should return false", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesWithMultipleBranches() throws Exception {
        branchArray = new String[]{"branchone", "branchtwo"};
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[]{"branchone", "branchtwo"})), 
                        with(equal(new String[]{"vob"}))); 
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertTrue("The first time should always return true", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesMultipleVobPaths() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[]{"branch"})), 
                        with(equal(new String[]{"vob1", "vob2/vob2-1", "vob\\ 3"}))); 
                will(returnValue(true));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", new String[]{"vob1", "vob2/vob2-1", "vob\\ 3"}, "");
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesNoBranch() throws Exception {
        branchArray = new String[] {""};  
        final Calendar mockedCalendar = Calendar.getInstance();
        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[]{""})), 
                        with(equal(new String[]{""}))); 
                will(returnValue(false));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesWithMatrixProject() throws Exception {
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(with(any(EventRecordFilter.class)), 
                        with(equal(mockedCalendar.getTime())), 
                        with(equal("viewname")), 
                        with(equal(new String[]{"branch"})), 
                        with(equal(new String[]{""}))); 
                will(returnValue(true));
            }
        });
        final MatrixBuild matrixBuild = classContext.mock(MatrixBuild.class);
        classContext.checking(new Expectations() {
            {
                one(project).getLastBuild(); will(returnValue(matrixBuild));
                one(matrixBuild).getTimestamp(); will(returnValue(mockedCalendar));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "", "");
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    private class AbstractClearCaseScmDummy extends AbstractClearCaseScm {

        private final String[] vobPaths;

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam,
                boolean filterOutDestroySubBranchEvent) {
            this (viewName, new String[]{vobPaths}, mkviewOptionalParam, filterOutDestroySubBranchEvent);
        }

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam) {
            this (viewName, new String[]{vobPaths}, mkviewOptionalParam);
        }

        public AbstractClearCaseScmDummy(String viewName, String[] vobPaths, String mkviewOptionalParam) {
            this( viewName, vobPaths, mkviewOptionalParam, false);
        }

        public AbstractClearCaseScmDummy(String viewName, String[] vobPaths, String mkviewOptionalParam,
                boolean filterOutDestroySubBranchEvent) {
            super( viewName, mkviewOptionalParam, filterOutDestroySubBranchEvent);
            this.vobPaths = vobPaths;
        }

        @Override
        public SCMDescriptor<?> getDescriptor() {
            throw new IllegalStateException("GetDescriptor() can not be used in tests");
        }

        @Override
        protected ClearToolLauncher createClearToolLauncher(TaskListener listener, FilePath workspace, Launcher launcher) {
            return null;
        }

        @Override
        protected CheckOutAction createCheckOutAction(ClearToolLauncher launcher) {
            return checkOutAction;
        }

        @Override
        protected PollAction createPollAction(ClearToolLauncher launcher) {
            return pollAction;
        }

        @Override
        public String[] getBranchNames() {
            return branchArray;
        }

        @Override
        public String[] getViewPaths(FilePath viewPath) throws IOException, InterruptedException {
            return vobPaths;
        }

        @Override
        protected ChangeLogAction createChangeLogAction(ClearToolLauncher launcher) {
            return changeLogAction;
        }

        @Override
        protected SaveChangeLogAction createSaveChangeLogAction(ClearToolLauncher launcher) {
            return saveChangeLogAction;
        }

        @Override
        protected TaggingAction createTaggingAction(ClearToolLauncher clearToolLauncher) {
            return null;
        }

        @Override
        public ChangeLogParser createChangeLogParser() {
            return null;
        }
    }    
}
