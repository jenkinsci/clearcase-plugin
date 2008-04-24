package hudson.plugins.clearcase.action;

import java.io.IOException;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.ClearTool;

public class SnapshotCheckoutAction implements CheckOutAction {

    private final String viewName;
    private final String configSpec;
    private final boolean useUpdate;
    private final ClearTool cleartool;

    public SnapshotCheckoutAction(ClearTool clearTool, String viewName, String configSpec, boolean useUpdate) {
        this.cleartool = clearTool;
        this.viewName = viewName;
        this.configSpec = configSpec;
        this.useUpdate = useUpdate;        
    }

    public boolean checkout(Launcher launcher, FilePath workspace, BuildListener listener) throws IOException, InterruptedException {

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
            cleartool.mkview(viewName);
            String tempConfigSpec = configSpec;
            if (launcher.isUnix()) {
                tempConfigSpec = configSpec.replaceAll("\r\n", "\n");
            }
            cleartool.setcs(viewName, tempConfigSpec);
            updateView = false;
        }

        if (updateView) {
            cleartool.update(viewName);
        }
        return true;
    }

}
