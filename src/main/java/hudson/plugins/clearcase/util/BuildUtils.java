package hudson.plugins.clearcase.util;

import hudson.model.AbstractBuild;
import hudson.model.Node;
import hudson.util.StreamTaskListener;

public final class BuildUtils {
    private BuildUtils() {}

    public static boolean isRunningOnUnix(AbstractBuild<?, ?> build) {
        Node builtOn = build.getBuiltOn();
        if (builtOn == null) {
          return false;
        }
        return builtOn.createLauncher(StreamTaskListener.NULL).isUnix();
      }
}
