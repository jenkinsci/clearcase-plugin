package hudson.plugins.clearcase.viewstorage;

import hudson.Extension;

import org.kohsuke.stapler.DataBoundConstructor;

public class ColocatedViewStorage extends ViewStorage {
    
    @Extension
    public static class DescriptorImpl extends ViewStorageDescriptor<SpecificViewStorage> {

        @Override
        public String getDisplayName() {
            return "Colocated with the view";
        }
        
    }
    
    @DataBoundConstructor
    public ColocatedViewStorage(){}

    @Override
    public String[] getCommandArguments(boolean unix, String viewTag) {
        return new String[0];
    }

}
