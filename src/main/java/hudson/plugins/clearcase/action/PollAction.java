package hudson.plugins.clearcase.action;


import java.io.IOException;
import java.util.Date;

/**
 * Action for polling a ClearCase repository.
 */
public interface PollAction {
    /**
     * Returns if the repository has any changes since the specified time
     * @param time check for changes since this time
     * @param viewName the name of the view
     * @param branchNames the branch names
     * @param viewPaths optional vob paths
     * @return true, if the ClearCase repository has changes; false, otherwise.
     */
    boolean getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException;
}
