package hudson.plugins.clearcase.util;

import hudson.Launcher;

public abstract class PathUtil {

	public static String convertPathsBetweenUnixAndWindows(String path,
			Launcher launcher) {

		String tempPath = path;
		if (launcher.isUnix()) {
			tempPath = path.replaceAll("\r\n", "\n");
		}
		if (launcher.isUnix()) {
			tempPath = tempPath.replaceAll("\\\\", "/");
		} else {
			tempPath = tempPath.replaceAll("/", "\\");
		}
		return tempPath;
	}

}
