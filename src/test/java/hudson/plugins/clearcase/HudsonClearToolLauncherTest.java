package hudson.plugins.clearcase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.BuildListener;
import hudson.util.ForkOutputStream;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class HudsonClearToolLauncherTest extends AbstractWorkspaceTest {
    private Mockery classContext;
    private Mockery context;

    private BuildListener taskListener;
    private Launcher launcher;
    private Proc proc;
    
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
        proc = classContext.mock(Proc.class);
        taskListener = context.mock(BuildListener.class);
    }

    @After
    public void teardown() throws Exception {
        deleteWorkspace();
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

        ClearToolLauncher launcherImpl = new HudsonClearToolLauncher("exec", "ccscm", taskListener, workspace, launcher);
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

        ClearToolLauncher launcherImpl = new HudsonClearToolLauncher("exec", "ccscm", taskListener, workspace, launcher);
        launcherImpl.run(new String[] { "a" }, null, new ByteArrayOutputStream(), null);
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }

    @Test(expected=IOException.class)
    public void testBadReturnCode() throws Exception {
        final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

        context.checking(new Expectations() {
            {
                one(taskListener).getLogger(); will(returnValue(mockedStream));
                one(taskListener).fatalError(with(any(String.class)));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).launch(with(any(String[].class)), with(any(String[].class)),
                        with(aNull(InputStream.class)), with(any(ForkOutputStream.class)), with(any(FilePath.class)));
                    will(returnValue(proc));
                one(proc).join(); will(returnValue(1));
            }
        });

        ClearToolLauncher launcherImpl = new HudsonClearToolLauncher("exec", "ccscm", taskListener, workspace, launcher);
        launcherImpl.run(new String[] { "a", "b" }, null, new ByteArrayOutputStream(), null);
    }

    /**
     * Assert that the Hudson cleartool launcher adds the clear tool exectuable
     * to the command array.
     */
    @Test
    public void assertClearToolExecutableIsSet() throws Exception {
        final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

        context.checking(new Expectations() {
            {
                ignoring(taskListener).getLogger(); will(returnValue(mockedStream));
            }
        });
        classContext.checking(new Expectations() {
            {
                one(launcher).launch(with(equal(new String[]{"exec", "command"})), with(any(String[].class)),
                        with(aNull(InputStream.class)), with(same(mockedStream)), with(any(FilePath.class)));
            }
        });

        ClearToolLauncher launcherImpl = new HudsonClearToolLauncher("exec", "ccscm", taskListener, workspace, launcher);
        launcherImpl.run(new String[] { "command" }, null, null, null);
        classContext.assertIsSatisfied();
        context.assertIsSatisfied();
    }
}
