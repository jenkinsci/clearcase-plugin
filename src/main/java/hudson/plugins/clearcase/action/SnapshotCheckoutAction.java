package hudson.plugins.clearcase.action;

import java.io.IOException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;

/**
 * Check out action that will check out files into a snapshot view.
 */
public class SnapshotCheckoutAction implements CheckOutAction {

    private final String configSpec;
    private final boolean useUpdate;
    private final ClearTool cleartool;

    public SnapshotCheckoutAction(ClearTool clearTool, String configSpec, boolean useUpdate) {
        this.cleartool = clearTool;
        this.configSpec = configSpec;
        this.useUpdate = useUpdate;        
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {

        boolean updateView = useUpdate;        
        boolean localViewPathExists = new FilePath(workspace, viewName).exists();
            
        if (localViewPathExists) {
            if (updateView) {
                String currentConfigSpec = cleartool.catcs(viewName).trim();
                if (!configSpec.trim().replaceAll("\r\n", "\n").equals(currentConfigSpec)) {
                    updateView = false;
                }
            }
            if (!updateView) {
                cleartool.rmview(viewName);
                localViewPathExists = false;
            }                
        }

        if (!localViewPathExists) {
            cleartool.mkview(viewName, null);
            String tempConfigSpec = configSpec;
            if (launcher.isUnix()) {
                tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
            }
            cleartool.setcs(viewName, tempConfigSpec);
            updateView = false;
        }

        if (updateView) {
            cleartool.update(viewName, null);
        }
        return true;
    }

}
