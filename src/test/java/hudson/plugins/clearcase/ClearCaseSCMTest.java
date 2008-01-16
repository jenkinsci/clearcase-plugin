package hudson.plugins.clearcase;

import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.util.ForkOutputStream;
import hudson.matrix.MatrixBuild;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearCaseSCMTest extends AbstractWorkspaceTest implements ClearToolFactory {

    private Mockery classContext;
    private Mockery context;

    private ClearTool clearTool;
    private BuildListener taskListener;
    private Launcher launcher;
    private AbstractProject project;
    private Build build;

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
        taskListener = context.mock(BuildListener.class);
        clearTool = context.mock(ClearTool.class);
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }

    @Test
    public void testBuildEnvVars() {
        ClearCaseSCM scm = createSimpleScm();
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(null, env);
        assertEquals("The env var wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
    }

    @Test
    public void testGetConfigSpec() {
        ClearCaseSCM scm = createSimpleScm();
        assertEquals("The config spec isnt correct", "configspec", scm.getConfigSpec());
    }

    @Test
    public void testGetViewName() {
        ClearCaseSCM scm = createSimpleScm();
        assertEquals("The view name isnt correct", "viewname", scm.getViewName());
    }

    @Test
    public void testGetViewNameNonNull() {
        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", null, true, "", false, "");
        assertNotNull("The view name can not be null", scm.getViewName());
    }

    @Test
    public void testGetBranch() {
        ClearCaseSCM scm = createSimpleScm();
        assertEquals("The branch isnt correct", "branch", scm.getBranch());
    }

    @Test
    public void testIsUseUpdate() {
        ClearCaseSCM scm = createSimpleScm();
        assertTrue("The isUpdate isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testGetVobPaths() {
        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "vobs/ avob", false, "");
        assertEquals("The vob paths isnt correct", "vobs/ avob", scm.getVobPaths());
    }

    @Test
    public void testIsDynamicView() {
        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "", true, "");
        assertTrue("The dynamic isnt correct", scm.isUseDynamicView());
        assertFalse("The use update isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testGetViewDrive() {
        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "", true, "/tmp/c");
        assertEquals("The view drive isnt correct", "/tmp/c", scm.getViewDrive());
    }

    @Test
    public void testCheckoutDynamicWithNewConfigSpec() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                will(returnValue("other configspec"));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix();
                will(returnValue(true));
                one(build).getPreviousBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "", true, "drive");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutDynamic() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                will(returnValue("config\nspec"));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getPreviousBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "config\r\nspec", "viewname", false, "", true, "drive");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutFirstTimeNotUsingUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix();
                will(returnValue(true));
                one(build).getPreviousBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "", false, "");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutFirstTimeUsingUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix();
                will(returnValue(true));
                one(build).getPreviousBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = createSimpleScm();
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutSecondTimeUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                will(returnValue("configspec"));
                one(clearTool).update(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getPreviousBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = createSimpleScm();
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutSecondTimeUsingUpdateWithNewConfigSpec() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                will(returnValue("other configspec"));
                one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getPreviousBuild();
                will(returnValue(null));
                one(launcher).isUnix();
                will(returnValue(true));
            }
        });

        ClearCaseSCM scm = createSimpleScm();
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testCheckoutSecondTimeNotUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getPreviousBuild();
                will(returnValue(null));
                one(launcher).isUnix();
                will(returnValue(true));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "", false, "");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSnapshotCheckoutWithHistory() throws Exception {
        workspace.child("viewname").mkdirs();
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branch")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild();
                will(returnValue(build));
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
                one(launcher).isUnix();
                will(returnValue(true));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "vob", false, "");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 20);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSnapshotCheckoutWithMultipleBranches() throws Exception {
        workspace.child("viewname").mkdirs();
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                one(clearTool).setcs(with(any(ClearToolLauncher.class)), with(equal("viewname")),
                        with(equal("configspec")));
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branchone")));
                will(returnValue(new ArrayList<ClearCaseChangeLogEntry>()));
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branchtwo")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild();
                will(returnValue(build));
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
                one(launcher).isUnix();
                will(returnValue(true));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branchone branchtwo", "configspec", "viewname", false, "vob", false,
                "");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 20);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testDynamicCheckoutWithHistory() throws Exception {
        workspace.child("viewname").mkdirs();
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));
        list.add(new ClearCaseChangeLogEntry(new Date(12), "user", "comment"));

        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(100000);

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(any(ClearToolLauncher.class)), with(equal("viewname")));
                will(returnValue("configspec"));
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branch")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                exactly(2).of(build).getPreviousBuild();
                will(returnValue(build));
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "vob", true, "");
        File changelogFile = new File(PARENT_FILE, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
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
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branch")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
                one(project).getLastBuild();
                will(returnValue(build));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "vob", false, "");
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
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branch")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
                one(project).getLastBuild();
                will(returnValue(build));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "vob", false, "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertFalse("pollChanges() should return false", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesWithMultipleBranches() throws Exception {
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);

        context.checking(new Expectations() {
            {
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branchone")));
                will(returnValue(new ArrayList<Object[]>()));
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(equal(mockedCalendar.getTime())),
                        with(equal("viewname")), with(equal("branchtwo")));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp();
                will(returnValue(mockedCalendar));
                one(project).getLastBuild();
                will(returnValue(build));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branchone branchtwo", "configspec", "viewname", true, "vob", false,
                "");
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertTrue("The first time should always return true", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesFirstTime() throws Exception {
        classContext.checking(new Expectations() {
            {
                one(project).getLastBuild();
                will(returnValue(null));
            }
        });

        ClearCaseSCM scm = createSimpleScm();
        boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
        assertTrue("The first time should always return true", hasChanges);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testPollChangesWithMatrixProject() throws Exception {
        final ArrayList<Object[]> list = new ArrayList<Object[]>();
        list.add(new String[] { "A" });
        final Calendar mockedCalendar = Calendar.getInstance();
        mockedCalendar.setTimeInMillis(400000);
        context.checking(new Expectations() {
            {
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(any(Date.class)),
                        with(any(String.class)), with(any(String.class)));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        final MatrixBuild matrixBuild = classContext.mock(MatrixBuild.class);
        classContext.checking(new Expectations() {
            {
                one(project).getLastBuild();
                will(returnValue(matrixBuild));
                one(matrixBuild).getTimestamp();
                will(returnValue(mockedCalendar));
            }
        });

        ClearCaseSCM scm = createSimpleScm();
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testSupportsPolling() {
        ClearCaseSCM scm = createSimpleScm();
        assertTrue("The ClearCase SCM supports polling but is reported not to", scm.supportsPolling());
    }

    @Test
    public void testMultipleVobPaths() throws Exception {
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        context.checking(new Expectations() {
            {
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(any(Date.class)),
                        with(any(String.class)), with(any(String.class)));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("vob1 vob2/vob2-1 vob\\ 3")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp();
                will(returnValue(Calendar.getInstance()));
                one(project).getLastBuild();
                will(returnValue(build));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "vob1 vob2/vob2-1 vob\\ 3",
                false, "");
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testNoVobPaths() throws Exception {
        final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
        context.checking(new Expectations() {
            {
                one(clearTool).lshistory(with(any(ClearToolLauncher.class)), with(any(Date.class)),
                        with(any(String.class)), with(any(String.class)));
                will(returnValue(list));
                one(clearTool).setVobPaths(with(equal("")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(build).getTimestamp();
                will(returnValue(Calendar.getInstance()));
                one(project).getLastBuild();
                will(returnValue(build));
            }
        });

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewName", true, "", false, "");
        scm.pollChanges(project, launcher, workspace, taskListener);

        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testClearToolLauncherImplWithNullStreams() throws Exception {
        final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

        context.checking(new Expectations() {
            {
                one(taskListener).getLogger();
                will(returnValue(mockedStream));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).launch(with(any(String[].class)), with(any(String[].class)),
                        with(aNull(InputStream.class)), with(same(mockedStream)), with(any(FilePath.class)));
            }
        });

        ClearCaseSCM.ClearToolLauncherImpl launcherImpl = new ClearCaseSCM.ClearToolLauncherImpl(taskListener,
                workspace, launcher);
        launcherImpl.run(new String[] { "a" }, null, null, null);
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test
    public void testClearToolLauncherImplWithOutput() throws Exception {
        final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

        context.checking(new Expectations() {
            {
                one(taskListener).getLogger();
                will(returnValue(mockedStream));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).launch(with(any(String[].class)), with(any(String[].class)),
                        with(aNull(InputStream.class)), with(any(ForkOutputStream.class)), with(any(FilePath.class)));
            }
        });

        ClearCaseSCM.ClearToolLauncherImpl launcherImpl = new ClearCaseSCM.ClearToolLauncherImpl(taskListener,
                workspace, launcher);
        launcherImpl.run(new String[] { "a" }, null, new ByteArrayOutputStream(), null);
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    private ClearCaseSCM createSimpleScm() {
        return new ClearCaseSCM(this, "branch", "configspec", "viewname", true, "", false, "");
    }

    public ClearTool create(ClearCaseSCM scm, TaskListener listener) {
        return clearTool;
    }

    public ChangeLogEntryMerger createChangeLogEntryMerger(ClearCaseSCM scm) {
        return new ChangeLogEntryMerger();
    }
}
