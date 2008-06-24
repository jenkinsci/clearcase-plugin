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
    
    List<? extends ChangeLogSet.Entry> getChanges(EventRecordFilter eventFilter, Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException,InterruptedException;
}
