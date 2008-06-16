package hudson.plugins.clearcase;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.List;

public interface ClearTool {

    /**
     * Updates the elements in the view
     * 
     * @param viewName the name of the view
     * @param loadRules optional load rules, null if not used.
     */
    void update(String viewName, String loadRules) throws IOException, InterruptedException;
    
    /**
     * Removes the view from a VOB
     * 
     * @param viewName the name of the view
     */
    void rmview(String viewName) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @param streamSelector optional stream selector, null if not used.
     */
    void mkview(String viewName, String streamSelector) throws IOException, InterruptedException;

    /**
     * Sets the config spec of the view
     * 
     * @param viewName the name of the view
     * @param configSpec the name of the file containing a config spec
     */
    void setcs(String viewName, String configSpec) throws IOException, InterruptedException;

    /**
     * Attaches version labels to versions of elements
     * 
     * @param viewName the name of the view
     * @param label the label name
     */
    void mklabel(String viewName, String label) throws IOException, InterruptedException;

    /**
     * Returns Reader containing output from lshistory.
     * @param format format that should be used by the lshistory command
     * @param lastBuildDate lists events recorded since (that is, at or after) the specified date-time
     * @param viewName the name of the view
     * @param branch the name of the branch to get history events for; if null then history events for all branches are
     *                listed
     * @param pathsInView view paths that should be added to the lshistory command. The view paths
     *                  must be relative. 
     * 
     * @return Reader containing output from command
     */
    Reader lshistory(String format, Date lastBuildDate, String viewName, String branch, String[] pathsInView) throws IOException, InterruptedException;

    /**
     * Lists activities .......(?)
     * @throws InterruptedException 
     * @throws IOException
     * @return reader containing command output 
     */
    Reader lsactivity(String activity, String commandFormat,String viewname) throws IOException, InterruptedException;
    
    /**
     * Lists view registry entries.
     * This command needs to be run inside a view.
     * 
     * @param onlyActiveDynamicViews true for only return active dynamic views; false all views are returned
     * @return list of view names
     */
    List<String> lsview(boolean onlyActiveDynamicViews) throws IOException,
            InterruptedException;

    /**
     * Lists VOB registry entries
     * 
     * @param onlyMOunted true for only return mounted vobs; false all vobs are returned
     * @return list of vob names
     */
    List<String> lsvob(boolean onlyMOunted) throws IOException, InterruptedException;

    /**
     * Retrives the config spec for the specified viewname
     * @param viewName the name of the view
     * @return a string containing the config spec
     */
    String catcs(String viewName) throws IOException, InterruptedException;
    
    /**
     * Starts or connects to a dynamic view's view_server process
     * 
     * @param viewTags One or more currently registered view tags (that is, view tags visible to lsview).
     */
    void startView(String viewTags) throws IOException, InterruptedException;
}
