/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase;

import java.io.IOException;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public interface ClearTool {

    /**
     * Get the inner CLearToolLauncher.
     * 
     * @return The inner CLearToolLauncher.
     */
    public ClearToolLauncher getLauncher();

    /**
     * Updates the elements in the view
     * 
     * @param viewName the name of the view
     * @param loadRules optional load rules, null if not used.
     */
    void update(String viewName, String[] loadRules) throws IOException, InterruptedException;

    /**
     * Ends the view
     * 
     * @param viewName the name of the view
     */
    void endView(String viewName) throws IOException, InterruptedException;

    /**
     * Removes the view from a VOB
     * 
     * @param viewName the name of the view
     */
    void rmview(String viewName) throws IOException, InterruptedException;

    /**
     * Unregisters the view tag for a given UUID.
     * 
     * @param viewUuid the unique identifier for the view.
     */
    void unregisterView(String viewUuid) throws IOException, InterruptedException;

    /**
     * Removes the view (as identified by UUID) from all VOBs
     * 
     * @param viewUuid the unique identifier for the view
     */
    void rmviewUuid(String viewUuid) throws IOException, InterruptedException;

    /**
     * Removes the view tag from the ClearCase registry - used when the view storage in the workspace has already been
     * deleted.
     * 
     * @param viewName the name of the view
     */
    void rmviewtag(String viewName) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @param streamSelector optional stream selector, null if not used.
     */
    void mkview(String viewName, String streamSelector) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param launcher launcher for launching the command
     * @param viewName the name of the view
     * @param streamSelector optional stream selector, null if not used.
     */
    void mkview(String viewName, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException;

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
     * 
     * @param format format that should be used by the lshistory command
     * @param lastBuildDate lists events recorded since (that is, at or after) the specified date-time
     * @param viewName the name of the view
     * @param branch the name of the branch to get history events for; if null then history events for all branches are
     *            listed
     * @param pathsInView view paths that should be added to the lshistory command. The view paths must be relative.
     * @return Reader containing output from command
     */
    Reader lshistory(String format, Date lastBuildDate, String viewName, String branch, String[] pathsInView) throws IOException, InterruptedException;

    /**
     * Lists activities .......(?)
     * 
     * @throws InterruptedException
     * @throws IOException
     * @return reader containing command output
     */
    Reader lsactivity(String activity, String commandFormat, String viewname) throws IOException, InterruptedException;

    /**
     * Lists view registry entries. This command needs to be run inside a view.
     * 
     * @param onlyActiveDynamicViews true for only return active dynamic views; false all views are returned
     * @return list of view names
     */
    List<String> lsview(boolean onlyActiveDynamicViews) throws IOException, InterruptedException;
    
    /**
     * Given a relative path, return the associated view tag if it exists. Otherwise, it will return null
     * @return
     * @throws IOException
     * @throws InterruptedException 
     */
    String lscurrentview(String viewPath) throws IOException, InterruptedException;

    /**
     * Lists VOB registry entries
     * 
     * @param onlyMOunted true for only return mounted vobs; false all vobs are returned
     * @return list of vob names
     */
    List<String> lsvob(boolean onlyMOunted) throws IOException, InterruptedException;

    /**
     * Checks whether the given view tag already exists in the ClearCase region.
     * 
     * @param viewName the view tag to check
     * @return true if the view tag exists, false otherwise.
     */
    boolean doesViewExist(String viewName) throws IOException, InterruptedException;

    /**
     * Retrieves the canonical working directory for a given view.
     * 
     * @param viewName the view to use
     * @return the return from "cleartool pwv"
     */
    String pwv(String viewName) throws IOException, InterruptedException;

    /**
     * Retrieves the config spec for the specified view name
     * 
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

    /**
     * Mounts all VOBs.
     */
    void mountVobs() throws IOException, InterruptedException;

    /**
     * Synchronizes the Dynamic UCM view with the streams recommended baseline
     * 
     * @param viewName
     * @param stream
     * @throws IOException
     * @throws InterruptedException
     */
    void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException;
    
    /**
     * Call the cleartool describe with the provided format on the specified object selector
     * See http://www.ipnom.com/ClearCase-Commands/describe.html for valid options
     * @param format
     * @param objectSelector
     * @return A reader to the command output
     * @throws IOException If cleartool throws an error code
     * @throws InterruptedException If the process is interrupted
     * @since 1.3
     */
    Reader describe(String format, String objectSelector) throws IOException, InterruptedException;
    

    /**
     * Gets the view UUID, for thorough view deletion.
     * 
     * @param viewName
     * @throws IOException
     * @throws InterruptedException
     */
    Properties getViewData(String viewName) throws IOException, InterruptedException;

    void logRedundantCleartoolError(String[] cmd, Exception ex);

    Reader diffblVersions(String baseline1, String baseline2, String viewPath);

}
