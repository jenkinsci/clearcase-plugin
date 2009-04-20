package hudson.plugins.clearcase.util;

import hudson.Launcher;

public abstract class PathUtil {
    
    public static String convertPathsBetweenUnixAndWindows(String path, Launcher launcher) {
        return convertPathsBetweenUnixAndWindows(path, launcher.isUnix());
    }

    public static String convertPathsBetweenUnixAndWindows(String path,
                                                           boolean isUnix) {
		String tempPath = path;
		if (isUnix) {
			tempPath = tempPath.replaceAll("\r\n", "\n");
		} else {
			tempPath = tempPath.replaceAll("\n", "\r\n");
			tempPath = tempPath.replaceAll("\r\r\n", "\r\n");
		}
		if (isUnix) {
			tempPath = tempPath.replaceAll("\\\\", "/");
		} else {
			tempPath = tempPath.replaceAll("/", "\\\\");
		}
		return tempPath;
	}

}
