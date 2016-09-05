package hudson.plugins.clearcase.util;

import hudson.model.AbstractBuild;
import hudson.model.Node;

public final class BuildUtils {
    private BuildUtils() {}

    public static Boolean isRunningOnUnix(AbstractBuild<?, ?> build) {
        Node builtOn = build.getBuiltOn();
        if (builtOn == null || builtOn.toComputer() == null) {
          return false;
        }

        return builtOn.toComputer().isUnix();
      }
}
