package hudson.plugins.clearcase.action;

/**
 * Action that tags a ClearCase repository.
 * @todo implement this
 */
public interface TaggingAction {

    void tag(String label, String comment);
}
