package hudson.plugins.clearcase.action;

import hudson.plugins.clearcase.util.EventRecordFilter;
import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Action that gathers change log for a ClearCase repository.
 * 
 * @author Henrik L. Hansen
 */
public interface ChangeLogAction {
    
    List<? extends ChangeLogSet.Entry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException,InterruptedException;

    /**
     * Sets the event record filter that should be used when determining if an event is real or not.
     * @param filter the filter to use.
     */
    void setEventRecordFilter(EventRecordFilter filter);
}
