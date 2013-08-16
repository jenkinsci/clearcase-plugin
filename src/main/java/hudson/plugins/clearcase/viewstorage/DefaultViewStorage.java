package hudson.plugins.clearcase.viewstorage;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

public class DefaultViewStorage extends ViewStorage {

    @Extension
    public static class DescriptorImpl extends ViewStorageDescriptor<DefaultViewStorage> {

        @Override
        public String getDisplayName() {
            return "Default";
        }

    }

    @DataBoundConstructor
    public DefaultViewStorage() {
    }

    @Override
    public String[] getCommandArguments(boolean unix, String viewTag) {
        return new String[0];
    }

}
