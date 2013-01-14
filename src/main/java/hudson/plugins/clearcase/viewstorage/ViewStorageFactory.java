package hudson.plugins.clearcase.viewstorage;

import org.kohsuke.stapler.DataBoundConstructor;

@Deprecated
public class ViewStorageFactory {

    private String server;

    private String unixStorageDir;

    private String winStorageDir;

    @DataBoundConstructor
    public ViewStorageFactory(String server, String winDynStorageDir, String unixDynStorageDir) {
        this.server = server;
        this.winStorageDir = winDynStorageDir;
        this.unixStorageDir = unixDynStorageDir;
    }

    public String getServer() {
        return server;
    }

    public String getUnixStorageDir() {
        return unixStorageDir;
    }

    public String getWinStorageDir() {
        return winStorageDir;
    }

}
