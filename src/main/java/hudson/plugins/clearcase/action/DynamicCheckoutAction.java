package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;

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

    public DynamicCheckoutAction(ClearTool cleartool, String configSpec) {
        this.cleartool = cleartool;
        this.configSpec = configSpec;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException { 
        cleartool.startView(viewName);
        String currentConfigSpec = cleartool.catcs(viewName).trim();
        if (!configSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
            String tempConfigSpec = configSpec;
            if (launcher.isUnix()) {
                tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
            }
            cleartool.setcs(viewName, tempConfigSpec);
        }
        return true;
    }
}
