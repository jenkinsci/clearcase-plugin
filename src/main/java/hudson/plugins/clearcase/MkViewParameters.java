package hudson.plugins.clearcase;

import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.plugins.clearcase.viewstorage.ViewStorageFactory;

public class MkViewParameters {
    
    private ViewType type = ViewType.Snapshot;
    private String streamSelector;
    private String viewTag;
    private String viewPath;
    private ViewStorage viewStorage;
    private String additionalParameters;

    public String getAdditionalParameters() {
        return additionalParameters;
    }

    public String getStreamSelector() {
        return streamSelector;
    }
    public ViewType getType() {
        return type;
    }

    public String getViewPath() {
        return viewPath;
    }
    public ViewStorage getViewStorage() {
        if (viewStorage == null) {
            viewStorage = ViewStorageFactory.createDefault();
        }
        return viewStorage;
    }
    public String getViewTag() {
        return viewTag;
    }

    public void setAdditionalParameters(String additionalParameters) {
        this.additionalParameters = additionalParameters;
    }
    public void setStreamSelector(String streamSelector) {
        this.streamSelector = streamSelector;
    }
    public void setType(ViewType type) {
        this.type = type;
    }
    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }
    public void setViewStorage(ViewStorage viewStorage) {
        this.viewStorage = viewStorage;
    }
    public void setViewTag(String viewTag) {
        this.viewTag = viewTag;
    }


}
