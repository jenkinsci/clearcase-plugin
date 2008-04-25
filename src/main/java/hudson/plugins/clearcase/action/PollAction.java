package hudson.plugins.clearcase.action;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Action for polling a ClearCase repository.
 */
public interface PollAction {
    /**
     * Returns a list of change log entries for the repository since the specified time
     * @param time get all logs since this time
     * @param viewName the name of the view
     * @param branchName the branch/stream name
     * @param vobPaths optional vob paths
     * @return a list of change log entries
     */
    List<ClearCaseChangeLogEntry> getChanges(Date time, String viewName, String branchName, String vobPaths) throws IOException, InterruptedException;
}
