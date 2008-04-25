package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.io.File;
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
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.PollAction;
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
    
    private CheckOutAction checkOutAction;
    private PollAction pollAction;

    private String[] branchArray = new String[] {"branch"};
    
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
    public void testCreateChangeLogParser() {
        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vobs/ avob", "");
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
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
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile, 10);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 5);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testCheckout() throws Exception {
        workspace.child("viewname").mkdirs();
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace); will(returnValue(true));
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branch", "vob"); will(returnValue(list));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile, 10);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 20);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutWithMultipleBranches() throws Exception {
        branchArray = new String[]{"branchone", "branchtwo"};
        workspace.child("viewname").mkdirs();
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(checkOutAction).checkout(launcher, workspace);
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branchone", "vob"); will(returnValue(new ArrayList<ClearCaseChangeLogEntry>()));
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branchtwo", "vob"); will(returnValue(list));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob", "");
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile, 10);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 20);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testPollChanges() throws Exception {
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branch", "vob"); will(returnValue(list));
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
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branch", "vob"); will(returnValue(list));
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
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branchone", "vob"); will(returnValue(new ArrayList<Object[]>()));
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branchtwo", "vob"); will(returnValue(list));
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
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branch", "vob1 vob2/vob2-1 vob\\ 3"); will(returnValue(new ArrayList<ClearCaseChangeLogEntry>()));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp(); will(returnValue(mockedCalendar));
                one(project).getLastBuild(); will(returnValue(build));
            }
        });

        AbstractClearCaseScm scm = new AbstractClearCaseScmDummy("viewname", "vob1 vob2/vob2-1 vob\\ 3", "");
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
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "", ""); will(returnValue(new ArrayList<Object[]>()));
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
                one(pollAction).getChanges(mockedCalendar.getTime(), "viewname", "branch", ""); will(returnValue(new ArrayList<Object[]>()));
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

        private final String vobPaths;

        public AbstractClearCaseScmDummy(String viewName, String vobPaths, String mkviewOptionalParam) {
            super(viewName, mkviewOptionalParam);
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
        public String getVobPaths() {
            return vobPaths;
        }
    }    
}
