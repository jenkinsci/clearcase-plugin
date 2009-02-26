package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.util.PathUtil;

import java.io.IOException;

/**
 * Check out action for dynamic views.
 * This will not update any files from the repository as it is a dynamic view.
 * The class will make sure that the configured config spec is the same as the one
 * for the dynamic view.
 */
public class DynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String configSpec;
    private boolean doNotUpdateConfigSpec;

    public DynamicCheckoutAction(ClearTool cleartool, String configSpec, boolean doNotUpdateConfigSpec) {
        this.cleartool = cleartool;
        this.configSpec = configSpec;
        this.doNotUpdateConfigSpec = doNotUpdateConfigSpec;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException { 
        cleartool.startView(viewName);
        String currentConfigSpec = cleartool.catcs(viewName).trim();
        if (!doNotUpdateConfigSpec && !configSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
            String tempConfigSpec = PathUtil.convertPathsBetweenUnixAndWindows(configSpec, launcher);
            cleartool.setcs(viewName, tempConfigSpec);
        }
        return true;
    }


}
