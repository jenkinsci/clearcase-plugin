package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.BuildListener;

import java.io.IOException;

/**
 * Action for performing check outs from ClearCase.
 */
public interface CheckOutAction {
    
    boolean checkout(Launcher launcher, FilePath workspace, BuildListener listener) throws IOException, InterruptedException;
}
