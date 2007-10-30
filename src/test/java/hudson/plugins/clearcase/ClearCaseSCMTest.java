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
import java.util.HashMap;
import java.util.Map;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.util.ForkOutputStream;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearCaseSCMTest {

	private static final File PARENT_FILE = new File(System.getProperty("java.io.tmpdir"), "cc-files");
	
	private Mockery classContext;
	private Mockery context;
	
	private ClearTool clearTool;
	private BuildListener taskListener;
	private Launcher launcher;
	private FilePath workspace;
	private AbstractProject project;
	private Build build;

	@Before
	public void setUp() throws Exception {
		workspace = new FilePath(PARENT_FILE);
		workspace.mkdirs();

		context = new Mockery();
		classContext = new Mockery() {{
	        setImposteriser(ClassImposteriser.INSTANCE);
	    }};
		
		launcher = classContext.mock(Launcher.class);
		taskListener = context.mock(BuildListener.class);
		clearTool = context.mock(ClearTool.class);
		project = classContext.mock(AbstractProject.class);
		build = classContext.mock(Build.class);
	}
	
	@After
	public void teardown() throws Exception {
		workspace.deleteRecursive();
	}
	
	@Test
	public void testBuildEnvVars() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		Map<String, String> env = new HashMap<String, String>();
		scm.buildEnvVars(null, env);
		assertEquals("The env var wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
	}

	@Test
	public void testGetConfigSpec() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		assertEquals("The config spec isnt correct", "configspec", scm.getConfigSpec());
	}

	@Test
	public void testGetViewName() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		assertEquals("The view name isnt correct", "viewname", scm.getViewName());
	}

	@Test
	public void testGetViewNameNonNull() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", null, true);
		assertNotNull("The view name can not be null", scm.getViewName());
	}

	@Test
	public void testGetBranch() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		assertEquals("The branch isnt correct", "branch", scm.getBranch());
	}

	@Test
	public void testIsUseUpdate() {
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		assertTrue("The isUpdate isnt correct", scm.isUseUpdate());
	}

	@Test
	public void testCheckoutFirstTimeNotUsingUpdate() throws Exception {
		context.checking(new Expectations() {{
		    one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).setcs(with(any(ClearToolLauncher.class)), 
		    		with(equal("viewname")), 
		    		with(equal("configspec")));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getPreviousBuild(); will(returnValue(null));
		}});
				
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", false);
		File changelogFile = new File(PARENT_FILE, "changelog.xml");
		boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
		assertTrue("The first time should always return true", hasChanges);
			
		context.assertIsSatisfied();
		classContext.assertIsSatisfied();
	}

	@Test
	public void testCheckoutFirstTimeUsingUpdate() throws Exception {		
		context.checking(new Expectations() {{
		    one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).setcs(with(any(ClearToolLauncher.class)), 
		    		with(equal("viewname")), 
		    		with(equal("configspec")));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getPreviousBuild(); will(returnValue(null));
		}});
				
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		File changelogFile = new File(PARENT_FILE, "changelog.xml");
		boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
		assertTrue("The first time should always return true", hasChanges);
		
		context.assertIsSatisfied();
		classContext.assertIsSatisfied();
	}

	@Test
	public void testCheckoutSecondTimeUsingUpdate() throws Exception {
		workspace.child("viewname").mkdirs();

		context.checking(new Expectations() {{
		    one(clearTool).update(with(any(ClearToolLauncher.class)), 
		    		with(equal("viewname")));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getPreviousBuild(); will(returnValue(null));
		}});
				
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		File changelogFile = new File(PARENT_FILE, "changelog.xml");
		boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
		assertTrue("The first time should always return true", hasChanges);
		
		context.assertIsSatisfied();
		classContext.assertIsSatisfied();
	}

	@Test
	public void testCheckoutSecondTimeNotUsingUpdate() throws Exception {
		workspace.child("viewname").mkdirs();

		context.checking(new Expectations() {{
		    one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).setcs(with(any(ClearToolLauncher.class)), 
		    		with(equal("viewname")), 
		    		with(equal("configspec")));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getPreviousBuild(); will(returnValue(null));
		}});		
				
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", false);
		File changelogFile = new File(PARENT_FILE, "changelog.xml");
		boolean hasChanges = scm.checkout(build, launcher, workspace, taskListener, changelogFile);
		assertTrue("The first time should always return true", hasChanges);
		
		context.assertIsSatisfied();
		classContext.assertIsSatisfied();
	}

	@Test
	public void testCheckoutWithHistory() throws Exception {
		workspace.child("viewname").mkdirs();
		final ArrayList<ClearCaseChangeLogEntry> list = new ArrayList<ClearCaseChangeLogEntry>();
		list.add(new ClearCaseChangeLogEntry());
		list.add(new ClearCaseChangeLogEntry());

		final Calendar mockedCalendar = Calendar.getInstance();
		mockedCalendar.setTimeInMillis(100000);
		
		context.checking(new Expectations() {{
		    one(clearTool).rmview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).mkview(with(any(ClearToolLauncher.class)), with(equal("viewname")));
		    one(clearTool).setcs(with(any(ClearToolLauncher.class)), 
		    		with(equal("viewname")), 
		    		with(equal("configspec")));
			one(clearTool).lshistory(with(any(ClearToolLauncher.class)),
		    		with(equal(mockedCalendar.getTime())),
		    		with(equal("viewname")), 
		    		with(equal("branch"))); will(returnValue(list));
		}});
		classContext.checking(new Expectations() {{
		    exactly(2).of(build).getPreviousBuild(); will(returnValue(build));
		    one(build).getTimestamp(); will(returnValue(mockedCalendar));
		}});
						
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", false);
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
		list.add(new String[] {"A"});
		final Calendar mockedCalendar = Calendar.getInstance();
		mockedCalendar.setTimeInMillis(400000);

		context.checking(new Expectations() {{
			one(clearTool).lshistory(with(any(ClearToolLauncher.class)),
		    		with(equal(mockedCalendar.getTime())),
		    		with(equal("viewname")), 
		    		with(equal("branch"))); will(returnValue(list));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getTimestamp(); will(returnValue(mockedCalendar));
		    one(project).getLastBuild(); will(returnValue(build));
		}});

		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
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

		context.checking(new Expectations() {{
			one(clearTool).lshistory(with(any(ClearToolLauncher.class)),
		    		with(equal(mockedCalendar.getTime())),
		    		with(equal("viewname")), 
		    		with(equal("branch"))); will(returnValue(list));
		}});
		classContext.checking(new Expectations() {{
		    one(build).getTimestamp(); will(returnValue(mockedCalendar));
		    one(project).getLastBuild(); will(returnValue(build));
		}});
	
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
		assertFalse("pollChanges() should return false", hasChanges);

		classContext.assertIsSatisfied();
		context.assertIsSatisfied();
	}

	@Test
	public void testPollChangesFirstTime() throws Exception {
		classContext.checking(new Expectations() {{
		    one(project).getLastBuild(); will(returnValue(null));
		}});
		
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		boolean hasChanges = scm.pollChanges(project, launcher, workspace, taskListener);
		assertTrue("The first time should always return true", hasChanges);

		classContext.assertIsSatisfied();
		context.assertIsSatisfied();
	}

	@Test
	public void testSupportsPolling() {		
		ClearCaseSCM scm = new ClearCaseSCM(clearTool, "branch", "configspec", "viewname", true);
		assertTrue("The Clear Case SCM supports polling but is reported not to", scm.supportsPolling());
	}

	@Test
	public void testClearToolLauncherImplWithNullStreams() throws Exception {
		final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

		context.checking(new Expectations() {{
			one(taskListener).getLogger(); will(returnValue(mockedStream));
		}});
		classContext.checking(new Expectations() {{
		    one(launcher).launch(with(any(String[].class)), with(any(String[].class)), 
		    		with(aNull(InputStream.class)), with(same(mockedStream)), with(any(FilePath.class)));
		}});

		ClearCaseSCM.ClearToolLauncherImpl launcherImpl = new ClearCaseSCM.ClearToolLauncherImpl(taskListener, workspace, launcher);
		launcherImpl.run(new String[]{"a"}, null, null, null);
		classContext.assertIsSatisfied();
		context.assertIsSatisfied();
	}
	
	@Test
	public void testClearToolLauncherImplWithOutput() throws Exception {
		final PrintStream mockedStream = new PrintStream(new ByteArrayOutputStream());

		context.checking(new Expectations() {{
			one(taskListener).getLogger(); will(returnValue(mockedStream));
		}});
		classContext.checking(new Expectations() {{
		    one(launcher).launch(with(any(String[].class)), with(any(String[].class)), 
		    		with(aNull(InputStream.class)), with(any(ForkOutputStream.class)), with(any(FilePath.class)));
		}});

		ClearCaseSCM.ClearToolLauncherImpl launcherImpl = new ClearCaseSCM.ClearToolLauncherImpl(taskListener, workspace, launcher);
		launcherImpl.run(new String[]{"a"}, null, new ByteArrayOutputStream(), null);
		classContext.assertIsSatisfied();
		context.assertIsSatisfied();
	}
}
