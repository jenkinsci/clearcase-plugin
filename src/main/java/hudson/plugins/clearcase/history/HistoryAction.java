/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.clearcase.history;

import hudson.scm.ChangeLogSet;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 *
 * @author hlyh
 */
public interface HistoryAction {

    /**
     * Returns if the repository has any changes since the specified time
     * @param eventFilter TODO
     * @param time check for changes since this time
     * @param viewName the name of the view
     * @param branchNames the branch names
     * @param viewPaths optional vob paths
     * @return true, if the ClearCase repository has changes; false, otherwise.
     */
    public boolean hasChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException, InterruptedException;

    /**
     * Returns if the repository has any changes since the specified time
     * @param eventFilter TODO
     * @param time check for changes since this time
     * @param viewName the name of the view
     * @param branchNames the branch names
     * @param viewPaths optional vob paths
     * @return List of changes
     */
    public List<? extends ChangeLogSet.Entry> getChanges(Date time, String viewName, String[] branchNames, String[] viewPaths) throws IOException,InterruptedException;

}
