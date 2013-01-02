package hudson.plugins.clearcase.viewstorage;

import hudson.Util;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class ViewStorageFactory {

    /**
     * Clearcase UCM with automatic storage location selection.
     */
    public static final String SERVER_AUTO = "auto";
    /**
     * Base Clearcase, which doesn't normally use Storage Locations, shared view locations, etc..
     */
    public static final String SERVER_BASE = "-base-";

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
        if (this.server == null) {
            return new SpecificViewStorage(Util.replaceMacro(this.winStorageDir, variableResolver), 
                    Util.replaceMacro(this.unixStorageDir, variableResolver), unix, viewTag);
        } // else
        if (SERVER_AUTO.equals(this.server)) {
            return new ServerViewStorage();
        } // else
        if (SERVER_BASE.equals(this.server)) {
            return new BaseViewStorage();
        } // else
        return new ServerViewStorage(this.server);
    }

    public String getServer() {
        return this.server;
    }

    public String getUnixStorageDir() {
        return this.unixStorageDir;
    }

    public String getWinStorageDir() {
        return this.winStorageDir;
    }

    public static ViewStorage createDefault() {
        return new ServerViewStorage();
    }

    /**
     * Get a {@link ViewStorageFactory} for Base Clearcase instances.
     * @return
     */
    public static ViewStorageFactory getBaseDefault() {
        return new ViewStorageFactory(SERVER_BASE, "", "");
    }
    
    public static ViewStorageFactory getDefault() {
        return new ViewStorageFactory(SERVER_AUTO, "", "");
    }
}
