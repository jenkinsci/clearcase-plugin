package hudson.plugins.clearcase.viewstorage;

import hudson.plugins.clearcase.util.PathUtil;

class SpecificViewStorage implements ViewStorage {

    private final String winStorageDir;

    private final String unixStorageDir;

    private final boolean unix;

    private final String viewTag;

    /**
     * @param unix
     * @param winStorageDir
     * @param unixStorageDir
     */
    SpecificViewStorage(String winStorageDir, String unixStorageDir, boolean unix, String viewTag) {
        this.winStorageDir = winStorageDir;
        this.unixStorageDir = unixStorageDir;
        this.unix = unix;
        this.viewTag = viewTag;
    }

    @Override
    public String[] getCommandArguments() {
        return new String[]{getStorageDir(unix) + sepFor(unix) + viewTag + ".vws"};
    }

    private String sepFor(boolean unix) {
        return PathUtil.fileSepForOS(unix);
    }

    private String getStorageDir(boolean unix) {
        if(unix) {
            return unixStorageDir;
        } else {
            return winStorageDir;
        }
    }

    @Override
    public String getType() {
        return "specific";
    }

}
