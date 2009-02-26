package hudson.plugins.clearcase.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import junit.framework.Assert;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.remoting.VirtualChannel;

import org.jmock.Mockery;
import org.junit.Test;

public class PathUtilTest {
	@Test
	public void testWindows() throws Exception {
		Mockery ctx = new Mockery();
		Launcher launcher = new MyLauncher(false);
		String converted = PathUtil.convertPathsBetweenUnixAndWindows("C/abc",
				launcher);
		Assert.assertEquals("C\\abc", converted);
		String converted2 = PathUtil.convertPathsBetweenUnixAndWindows(
				"\nPeter\n", launcher);
		Assert.assertEquals("\r\nPeter\r\n", converted2);
		String converted3 = PathUtil.convertPathsBetweenUnixAndWindows(
				"C\\abc", launcher);
		Assert.assertEquals("C\\abc", converted);
		String converted4 = PathUtil.convertPathsBetweenUnixAndWindows(
				"\r\nPeter\r\n", launcher);
		Assert.assertEquals("\r\nPeter\r\n", converted2);

	}

	@Test
	public void testUnix() throws Exception {
		Mockery ctx = new Mockery();
		Launcher launcher = new MyLauncher(true);
		String converted = PathUtil.convertPathsBetweenUnixAndWindows("C\\abc",
				launcher);
		Assert.assertEquals("C/abc", converted);
		String converted2 = PathUtil.convertPathsBetweenUnixAndWindows(
				"\r\nPeter\r\n", launcher);
		Assert.assertEquals("\nPeter\n", converted2);
		String converted3 = PathUtil.convertPathsBetweenUnixAndWindows("C/abc",
				launcher);
		Assert.assertEquals("C/abc", converted);
		String converted4 = PathUtil.convertPathsBetweenUnixAndWindows(
				"\nPeter\n", launcher);
		Assert.assertEquals("\nPeter\n", converted2);
	}

	private static class MyLauncher extends Launcher {

		public MyLauncher(boolean unix) {
			super(null, null);
			this.unix = unix;
		}

		private boolean unix;

		@Override
		public void kill(Map<String, String> arg0) throws IOException,
				InterruptedException {
			// TODO Auto-generated method stub

		}

		@Override
		public Proc launch(String[] arg0, String[] arg1, InputStream arg2,
				OutputStream arg3, FilePath arg4) throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Proc launch(String[] arg0, boolean[] arg1, String[] arg2,
				InputStream arg3, OutputStream arg4, FilePath arg5)
				throws IOException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Channel launchChannel(String[] arg0, OutputStream arg1,
				FilePath arg2, Map<String, String> arg3) throws IOException,
				InterruptedException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isUnix() {
			return this.unix;
		}
	}
}
