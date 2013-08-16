package hudson.plugins.clearcase.viewstorage;

import hudson.model.Descriptor;
import hudson.model.Hudson;
import hudson.plugins.clearcase.ClearCaseSCM;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;

public abstract class ViewStorageDescriptor<T extends ViewStorage> extends Descriptor<ViewStorage> {

    private ClearCaseScmDescriptor clearcaseScmDescriptor;

    public ClearCaseScmDescriptor getClearcaseDescriptor() {
        if (clearcaseScmDescriptor == null) {
            clearcaseScmDescriptor = (ClearCaseScmDescriptor) Hudson.getInstance().getDescriptorOrDie(ClearCaseSCM.class);
        }
        return clearcaseScmDescriptor;
    }

    public ViewStorage getDefaultViewStorage() {
        return getClearcaseDescriptor().getDefaultViewStorage();
    }
}
