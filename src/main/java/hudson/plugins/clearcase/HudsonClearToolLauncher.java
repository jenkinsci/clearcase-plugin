package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.util.ForkOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class for executing the cleartool commands in the Hudson instance.
 */
public class HudsonClearToolLauncher implements ClearToolLauncher {

    private final TaskListener listener;
    private final FilePath workspace;
    private final Launcher launcher;
    
    private final String scmName;
    private final String executable;

    public HudsonClearToolLauncher(String executable, String scmName, TaskListener listener, FilePath workspace, Launcher launcher) {
        this.executable = executable;
        this.scmName = scmName;
        this.listener = listener;
        this.workspace = workspace;
        this.launcher = launcher;
    }

    public TaskListener getListener() {
        return listener;
    }

    public FilePath getWorkspace() {
        return workspace;
    }

    public boolean run(String[] cmd, InputStream inputStream, OutputStream outputStream, FilePath filePath) throws IOException,
            InterruptedException {
        OutputStream out = outputStream;
        FilePath path = filePath;
        String[] env = new String[0];

        if (path == null) {
            path = workspace;
        }

        if (out == null) {
            out = listener.getLogger();
        } else {
            out = new ForkOutputStream(out, listener.getLogger());
        }
        
        String[] cmdWithExec = new String[cmd.length + 1];
        cmdWithExec[0] = executable;
        for (int i = 0; i < cmd.length; i++) {
            cmdWithExec[i + 1] = cmd[i];
        }

        int r = launcher.launch(cmdWithExec, env, inputStream, out, path).join();
        if (r != 0) {
            StringBuilder builder = new StringBuilder();
            for (String cmdParam : cmd) {
                if (builder.length() > 0) {
                    builder.append(" ");
                }
                builder.append(cmdParam);
            }
            listener.fatalError(scmName + " failed. exit code=" + r);
            throw new IOException("cleartool did not return the expected exit code. Command line=\""
                    + builder.toString() + "\", actual exit code=" + r);
        }
        return true;
    }
}
