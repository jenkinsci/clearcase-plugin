package hudson.plugins.clearcase.util;

import hudson.Launcher;

public abstract class WindowsUnixConversion {

	public static String convertConfigSpecsForUnixAndWindows(String configSpec,
			Launcher launcher) {

		String tempConfigSpec = configSpec;
		if (launcher.isUnix()) {
			tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
		}
		if (launcher.isUnix()) {
			tempConfigSpec = tempConfigSpec.replaceAll("\\\\", "/");
		} else {
			tempConfigSpec = tempConfigSpec.replaceAll("/", "\\");
		}
		return tempConfigSpec;
	}

}
