package hudson.plugins.clearcase.action;

import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.ClearTool;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SnapshotCheckoutActionTest extends AbstractWorkspaceTest {

    private Mockery classContext;
    private Mockery context;

    private ClearTool clearTool;
    private BuildListener taskListener;
    private Launcher launcher;

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
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
    }
    @Test
    public void testFirstTimeNotUsingUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview(with(equal("viewname")));
                one(clearTool).setcs(with(equal("viewname")), with(equal("configspec")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "viewname", "configspec", false);
        action.checkout(launcher, workspace, taskListener);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testFirstTimeUsingUpdate() throws Exception {
        context.checking(new Expectations() {
            {
                one(clearTool).mkview(with(equal("viewname")));
                one(clearTool).setcs(with(equal("viewname")), with(equal("configspec")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "viewname", "configspec", true);
        action.checkout(launcher, workspace, taskListener);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testSecondTimeUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(equal("viewname"))); will(returnValue("configspec"));
                one(clearTool).update(with(equal("viewname")));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "viewname", "configspec", true);
        action.checkout(launcher, workspace, taskListener);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
    
    @Test
    public void testSecondTimeNotUsingUpdate() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).rmview(with(equal("viewname")));
                one(clearTool).mkview(with(equal("viewname")));
                one(clearTool).setcs(with(equal("viewname")), with(equal("configspec")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "viewname", "configspec", false);
        action.checkout(launcher, workspace, taskListener);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }

    @Test
    public void testSecondTimeNewConfigSpec() throws Exception {
        workspace.child("viewname").mkdirs();

        context.checking(new Expectations() {
            {
                one(clearTool).catcs(with(equal("viewname"))); will(returnValue("other configspec"));
                one(clearTool).rmview(with(equal("viewname")));
                one(clearTool).mkview(with(equal("viewname")));
                one(clearTool).setcs(with(equal("viewname")), with(equal("configspec")));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).isUnix(); will(returnValue(true));
            }
        });

        SnapshotCheckoutAction action = new SnapshotCheckoutAction(clearTool, "viewname", "configspec", true);
        action.checkout(launcher, workspace, taskListener);

        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
/*
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

        ClearCaseSCM scm = new ClearCaseSCM(this, "branch", "configspec", "viewname", false, "vob", false, "", null);
        File changelogFile = new File(parentFile, "changelog.xml");
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
                "", null);
        File changelogFile = new File(parentFile, "changelog.xml");
        boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
        assertTrue("The first time should always return true", hasChanges);

        FilePath changeLogFilePath = new FilePath(changelogFile);
        assertTrue("The change log file is empty", changeLogFilePath.length() > 20);
        context.assertIsSatisfied();
        classContext.assertIsSatisfied();
    }
*/
}
