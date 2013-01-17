package hudson.plugins.clearcase.viewstorage;

import hudson.Extension;
import hudson.Util;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class SpecificViewStorage extends ViewStorage {

    @Extension
    public static class DescriptorImpl extends ViewStorageDescriptor<SpecificViewStorage> {

        @Override
        public String getDisplayName() {
            return "Use explicit path";
        }

        public String getWinStorageDir() {
            ViewStorage defaultViewStorage = getDefaultViewStorage();
            if (defaultViewStorage instanceof SpecificViewStorage) {
                return ((SpecificViewStorage) defaultViewStorage).getWinStorageDir();
            }
            return getClearcaseDescriptor().getDefaultWinDynStorageDir();
        }

        public String getUnixStorageDir() {
            ViewStorage defaultViewStorage = getDefaultViewStorage();
            if (defaultViewStorage instanceof SpecificViewStorage) {
                return ((SpecificViewStorage) defaultViewStorage).getUnixStorageDir();
            }
            return getClearcaseDescriptor().getDefaultUnixDynStorageDir();
        }

    }

    private final String unixStorageDir;

    private final String winStorageDir;

    /**
     * @param unix
     * @param winStorageDir
     * @param unixStorageDir
     */
    @DataBoundConstructor
    public SpecificViewStorage(String winStorageDir, String unixStorageDir) {
        this.winStorageDir = winStorageDir;
        this.unixStorageDir = unixStorageDir;
    }

    @Override
    public SpecificViewStorage decorate(VariableResolver<String> resolver) {
        return new SpecificViewStorage(Util.replaceMacro(winStorageDir, resolver), Util.replaceMacro(unixStorageDir, resolver));
    }

    @Override
    public String[] getCommandArguments(boolean unix, String viewTag) {
        return new String[] { "-vws", getStorageDir(unix) + sepFor(unix) + viewTag + ".vws" };
    }

    public String getUnixStorageDir() {
        return unixStorageDir;
    }

    public String getWinStorageDir() {
        return winStorageDir;
    }

    private String getStorageDir(boolean unix) {
        if (unix) {
            return unixStorageDir;
        }
        return winStorageDir;
    }

    private String sepFor(boolean unix) {
        return PathUtil.fileSepForOS(unix);
    }

}
