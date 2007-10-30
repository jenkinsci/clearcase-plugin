package hudson.plugins.clearcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import hudson.FilePath;
import hudson.model.TaskListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.List;

import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearToolExecTest {

	private static final File PARENT_FILE = new File(System.getProperty("java.io.tmpdir"), "cc-files");

	private Mockery context;
	
	private ClearToolExec clearToolExec;
	private ClearToolLauncher launcher;
	private FilePath workspace;

	private TaskListener listener;

	@Before
	public void setUp() throws Exception {
		workspace = new FilePath(PARENT_FILE);
		workspace.mkdirs();
		
		context = new Mockery();
	    
		clearToolExec = new ClearToolExec("commandname");
		launcher = context.mock(ClearToolLauncher.class);
		listener = context.mock(TaskListener.class);
	}

	@After
	public void tearDown() throws Exception {
		workspace.deleteRecursive();
	}

	@Test
	public void testCreateView() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "mkview", "-snapshot", "-tag", "viewName", "viewName"})), 
		    		with(aNull(InputStream.class)), 
		    		with(aNull(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(returnValue(Boolean.TRUE));
		}});

		clearToolExec.mkview(launcher, "viewName");		
		context.assertIsSatisfied();
	}

	@Test
	public void testEditConfigSpec() throws Exception {
		context.checking(new Expectations() {{
			one(launcher).getWorkspace(); will(returnValue(workspace));
		    one(launcher).run(with(Matchers.hasItemInArray("commandname")), 
		    		with(aNull(InputStream.class)), 
		    		with(aNull(OutputStream.class)),
		    		with(equal("viewName"))); 
		    		will(returnValue(Boolean.TRUE));
		}});
		
		clearToolExec.setcs(launcher, "viewName", "configspec");		
		context.assertIsSatisfied();
	}

	@Test
	public void testRemoveView() throws Exception {
		context.checking(new Expectations() {{
			one(launcher).getWorkspace(); will(returnValue(workspace));
		    one(launcher).run(with(equal(new String[]{"commandname", "rmview", "-force", "viewName"})), 
		    		with(aNull(InputStream.class)), 
		    		with(aNull(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(returnValue(Boolean.TRUE));
		}});
		
		clearToolExec.rmview(launcher, "viewName");		
		context.assertIsSatisfied();
	}

	@Test
	public void testForcedRemoveView() throws Exception {
		workspace.child("viewName").mkdirs();
		
		context.checking(new Expectations() {{
			one(launcher).getWorkspace(); will(returnValue(workspace));
		    one(launcher).run(with(equal(new String[]{"commandname", "rmview", "-force", "viewName"})), 
		    		with(aNull(InputStream.class)), 
		    		with(aNull(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(returnValue(Boolean.TRUE));
		    one(launcher).getListener(); will(returnValue(listener));
		    one(listener).getLogger(); will(returnValue(new PrintStream(new ByteArrayOutputStream())));		    
		}});
		
		clearToolExec.rmview(launcher, "viewName");
		assertFalse("View folder still exists", workspace.child("viewName").exists());
		context.assertIsSatisfied();
	}

	@Test
	public void testUpdate() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "update", "-force", "-log", "NUL", "viewName"})), 
		    		with(aNull(InputStream.class)), 
		    		with(aNull(OutputStream.class)),
		    		with(aNull(String.class)));
		    will(returnValue(Boolean.TRUE));
		}});
		
		clearToolExec.update(launcher, "viewName");
		context.assertIsSatisfied();
	}

	@Test
	public void testListHistory() throws Exception {
		workspace.child("viewName").mkdirs();
		workspace.child("viewName").child("vob1").mkdirs();
		workspace.child("viewName").child("vob2").mkdirs();
		workspace.child("viewName").createTextTempFile("view", ".dat", "text");

		final Calendar mockedCalendar = Calendar.getInstance();
		mockedCalendar.set(2007, 10, 18, 15, 05, 25);
		
		context.checking(new Expectations() {{
			one(launcher).getWorkspace(); will(returnValue(workspace));
		    one(launcher).run(with(equal(new String[]{"commandname", "lshistory", "-r", "-since", "18-nov.15:05:25", "-fmt", ClearToolHistoryParser.getLogFormat(), "-branch", "branch", "-nco", "vob1", "vob2"})), 
		    		(InputStream) with(anything()), 
		    		(OutputStream) with(an(OutputStream.class)),
		    		with(equal("viewName")));
		    		will(doAll(new Streamer(2, ClearToolExecTest.class.getResourceAsStream("ct-lshistory-1.log")), returnValue(Boolean.TRUE))); 
		}});
		
		List<ClearCaseChangeLogEntry> lshistory = clearToolExec.lshistory(launcher, mockedCalendar.getTime(), "viewName", "branch");
		assertEquals("The history should contain 2 items", 2, lshistory.size());

		context.assertIsSatisfied();
	}

	@Test
	public void testListViews() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "lsview"})), 
		    		(InputStream) with(anything()), 
		    		(OutputStream) with(an(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(doAll(new Streamer(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log")), returnValue(Boolean.TRUE))); 
		}});
		
		List<String> views = clearToolExec.lsview(launcher, false);
		assertEquals("The view list should contain 4 items", 4, views.size());
		assertEquals("The first view name is incorrect", "qaaaabbb_R3A_view", views.get(0));
		assertEquals("The second view name is incorrect", "qccccddd_view", views.get(1));
		assertEquals("The third view name is incorrect", "qeeefff_view", views.get(2));
		assertEquals("The fourth view name is incorrect", "qeeefff_HUDSON_SHORT_CS_TEST", views.get(3));

		context.assertIsSatisfied();
	}

	@Test
	public void testListActiveDynamicViews() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "lsview"})), 
		    		(InputStream) with(anything()), 
		    		(OutputStream) with(an(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(doAll(new Streamer(2, ClearToolExecTest.class.getResourceAsStream("ct-lsview-1.log")), returnValue(Boolean.TRUE))); 
		}});
		
		List<String> views = clearToolExec.lsview(launcher, true);
		assertEquals("The view list should contain 1 item", 1, views.size());
		assertEquals("The third view name is incorrect", "qeeefff_view", views.get(0));

		context.assertIsSatisfied();
	}
	
	@Test
	public void testListVobs() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "lsvob"})), 
		    		(InputStream) with(anything()), 
		    		(OutputStream) with(an(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(doAll(new Streamer(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log")), returnValue(Boolean.TRUE))); 
		}});
		
		List<String> vobs = clearToolExec.lsvob(launcher, false);
		assertEquals("The vob list should contain 6 items", 6, vobs.size());
		assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
		assertEquals("The second vob name is incorrect", "pvoba", vobs.get(1));
		assertEquals("The third vob name is incorrect", "doc", vobs.get(2));
		assertEquals("The fourth vob name is incorrect", "demoa", vobs.get(3));
		assertEquals("The fifth vob name is incorrect", "pvob", vobs.get(4));
		assertEquals("The sixth vob name is incorrect", "bugvob", vobs.get(5));

		context.assertIsSatisfied();
	}

	@Test
	public void testListVobsMounted() throws Exception {
		context.checking(new Expectations() {{
		    one(launcher).run(with(equal(new String[]{"commandname", "lsvob"})), 
		    		(InputStream) with(anything()), 
		    		(OutputStream) with(an(OutputStream.class)),
		    		with(aNull(String.class)));
		    		will(doAll(new Streamer(2, ClearToolExecTest.class.getResourceAsStream("ct-lsvob-1.log")), returnValue(Boolean.TRUE))); 
		}});
		
		List<String> vobs = clearToolExec.lsvob(launcher, true);
		assertEquals("The vob list should contain 3 items", 3, vobs.size());
		assertEquals("The first vob name is incorrect", "demo", vobs.get(0));
		assertEquals("The second vob name is incorrect", "demoa", vobs.get(1));
		assertEquals("The third vob name is incorrect", "pvob", vobs.get(2));

		context.assertIsSatisfied();
	}
	
	
	private class Streamer implements Action {
		private InputStream inputStream;
		private int parameterIndex;

		/**
		 * @param inputStream
		 */
		public Streamer(int parameterIndex, InputStream inputStream) {
			this.inputStream = inputStream;
			this.parameterIndex = parameterIndex;
		}

		public void describeTo(Description description) {
		}
		
		public Object invoke(Invocation invocation) throws Throwable {
			int read = inputStream.read();
			while (read != -1) {
				((OutputStream)invocation.getParameter(parameterIndex)).write(read);
				read = inputStream.read();
			}
			inputStream.close();
			return null;
		}		
	}
}
