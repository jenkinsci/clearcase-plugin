package hudson.plugins.clearcase.util;

import java.util.regex.Pattern;

/**
 * Class that helps deteremine if an event record should be used in a change log.
 * The change log could be created in a checkout or polling action.
 */
public class EventRecordFilter {

    private static final Pattern DESTROYED_SUB_BRANCH_PATTERN = Pattern.compile("destroy sub-branch \".+\" of branch");

    private boolean filterOutDestroySubBranchEvent = false;

    /**
     * Tests if a specified event record should be included in a change log.
     * @param event string containing the event
     * @param version string containing the version info
     * @return true if the event record should be included in a change log.
     */
    public boolean accept(String event, String version) {
        return !  (version.endsWith("/0") 
                || version.endsWith("\\0") 
                || event.equalsIgnoreCase("create branch")
                || (filterOutDestroySubBranchEvent && DESTROYED_SUB_BRANCH_PATTERN.matcher(event).matches()));
    }
    
    /**
     * Set whetever the poll action should filter out "Destroy sub-branch [BRANCH] of branch" events. 
     * @param filterOutEvent true, then it should ignore the event; false (default) should not ignore it.
     */
    public void setFilterOutDestroySubBranchEvent(boolean filterOutEvent) {
        this.filterOutDestroySubBranchEvent  = filterOutEvent;
    }
    
    public boolean isFilteringOutDestroySubBranchEvent() {
        return filterOutDestroySubBranchEvent;
    }
}
