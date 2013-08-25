package hudson.plugins.clearcase.command;

import java.io.IOException;

import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearToolLauncher;

public interface CleartoolCommand {
    
    /**
     * Executes the given cleartool command
     * @param launcher
     * @param listener
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    CleartoolOutput execute(ClearToolLauncher launcher, TaskListener listener) throws IOException, InterruptedException;
}
