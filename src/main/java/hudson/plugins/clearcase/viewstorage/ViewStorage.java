package hudson.plugins.clearcase.viewstorage;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.util.VariableResolver;


public abstract class ViewStorage extends AbstractDescribableImpl<ViewStorage> implements ExtensionPoint {
    public abstract String[] getCommandArguments(boolean unix, String viewTag);
    
    public static ViewStorage createDefault() {
        return new ServerViewStorage("-auto");
    }
    
    public ViewStorage decorate(VariableResolver<String> resolver) {
        return this;
    }
    
}
