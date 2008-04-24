package hudson.plugins.clearcase;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface ClearTool {

    /**
     * Updates the elements in the view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     */
    void update(String viewName) throws IOException, InterruptedException;

    /**
     * Removes the view from a VOB
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     */
    void rmview(String viewName) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     */
    void mkview(String viewName) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     */
    void mkview(String viewName, String streamSelector) throws IOException, InterruptedException;

    /**
     * Sets the config spec of the view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @param configSpec the name of the file containing a config spec
     */
    void setcs(String viewName, String configSpec) throws IOException, InterruptedException;

    /**
     * Attaches version labels to versions of elements
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @param label the label name
     */
    void mklabel(String viewName, String label) throws IOException, InterruptedException;

    /**
     * Lists event records for VOB-database objects
     * 
     * @param launcher launcher for launching the command
     * @param lastBuildDate lists events recorded since (that is, at or after) the specified date-time
     * @param viewName the name of the view
     * @param branch the name of the branch to get history events for; if null then history events for all branches are
     *                listed
     * @return the event records
     */
    List<ClearCaseChangeLogEntry> lshistory(Date lastBuildDate, String viewName,
            String branch, String vobPaths) throws IOException, InterruptedException;

    /**
     * Lists view registry entries
     * 
     * @param launcher launcher for launching the command
     * @param onlyActiveDynamicViews true for only return active dynamic views; false all views are returned
     * @return list of view names
     */
    List<String> lsview(boolean onlyActiveDynamicViews) throws IOException,
            InterruptedException;

    /**
     * Lists VOB registry entries
     * 
     * @param launcher launcher for launching the command
     * @param onlyMOunted true for only return mounted vobs; false all vobs are returned
     * @return list of vob names
     */
    List<String> lsvob(boolean onlyMOunted) throws IOException, InterruptedException;

    /**
     * Retrives the config spec for the specified viewname
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @return a string containing the config spec
     */
    String catcs(String viewName) throws IOException, InterruptedException;
    
    /**
     * @param launcher launcher for launching the command
     * Creates a process that is set to a dynamic view
     * @param viewTag Any view tag specifying a dynamic view that is registered for the current network region.
     */
    void setView(String viewTag)  throws IOException, InterruptedException;
}
