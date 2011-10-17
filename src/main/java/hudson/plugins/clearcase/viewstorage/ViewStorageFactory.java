package hudson.plugins.clearcase.viewstorage;

import hudson.Util;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class ViewStorageFactory {

    private String server;

    private String winStorageDir;

    private String unixStorageDir;

    @DataBoundConstructor
    public ViewStorageFactory(String server, String winDynStorageDir, String unixDynStorageDir) {
        this.server = server;
        this.winStorageDir = winDynStorageDir;
        this.unixStorageDir = unixDynStorageDir;
    }

    public ViewStorage create(VariableResolver<String> variableResolver, boolean unix, String viewTag) {
        if (server == null) {
            return new SpecificViewStorage(Util.replaceMacro(winStorageDir, variableResolver), Util.replaceMacro(unixStorageDir, variableResolver), unix, viewTag);
        } else {
            if("auto".equals(server)) {
                return new ServerViewStorage();
            } else {
                return new ServerViewStorage(server);
            }
        }
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

    public static ViewStorage createDefault() {
        return new ServerViewStorage();
    }

    public static ViewStorageFactory getDefault() {
        return new ViewStorageFactory("auto", "", "");
    }
}
