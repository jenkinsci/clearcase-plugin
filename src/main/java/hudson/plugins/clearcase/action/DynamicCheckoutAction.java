package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;

public class DynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String viewName;
    private String configSpec;

    public DynamicCheckoutAction(ClearTool cleartool, String viewName, String configSpec) {
        super();
        this.cleartool = cleartool;
        this.viewName = viewName;
        this.configSpec = configSpec;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, BuildListener listener) throws IOException, InterruptedException {
        cleartool.setView(viewName);
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
