package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;

/**
 * Check out action for dynamic views.
 * This will not update any files from the repository as it is a dynamic view.
 * It only makes sure the view is started as config specs don't exist in UCM
 */
public class UcmDynamicCheckoutAction implements CheckOutAction {

    private ClearTool cleartool;
    private String stream;
        
    public UcmDynamicCheckoutAction(ClearTool cleartool, String stream) {
        super();
        this.cleartool = cleartool;
        this.stream = stream;
    }

    public boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException {        
    	cleartool.startView(viewName);
    	cleartool.syncronizeViewWithStream(viewName, stream);
        
        return true;
    }

}
