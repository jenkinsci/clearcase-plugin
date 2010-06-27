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
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;

/**
 * Abstraction to cleartool commands
 */
public interface ClearTool {

    public static enum DiffBlOptions {
        ACTIVITIES, VERSIONS, BASELINES, FIRST_ONLY, NRECURSE
    }
    
    public static enum DefaultPromotionLevel {
        REJECTED, INITIAL, BUILT, TESTED, RELEASED 
    }

    /**
     * Retrieves the config spec for the specified view name
     * 
     * @param viewTag The view tag the client want the config spec for.
     * @return a string containing the config spec
     */
    String catcs(String viewTag) throws IOException, InterruptedException;

    /**
     * Call the cleartool describe with the provided format on the specified object selector See
     * http://www.ipnom.com/ClearCase-Commands/describe.html for valid options
     * 
     * @param format
     * @param objectSelector
     * @return A reader to the command output
     * @throws IOException If cleartool throws an error code
     * @throws InterruptedException If the process is interrupted
     * @since 1.3
     */
    Reader describe(String format, String objectSelector) throws IOException, InterruptedException;

    /**
     * Call diffbl using the two provided baselines (can be stream or baseline)
     * 
     * @param options see http://www.ipnom.com/ClearCase-Commands/diffbl.html
     * @param baseline1
     * @param baseline2
     * @param viewPath A view path name needed to retrieve versions from
     * @return
     */
    Reader diffbl(EnumSet<DiffBlOptions> options, String baseline1, String baseline2, String viewPath);

    /**
     * Checks whether the given view tag already exists in the ClearCase region.
     * 
     * @param viewTag the view tag to check
     * @return true if the view tag exists, false otherwise.
     */
    boolean doesViewExist(String viewTag) throws IOException, InterruptedException;

    /**
     * Ends the view
     * 
     * @param viewTag the view tag
     */
    void endView(String viewTag) throws IOException, InterruptedException;

    /**
     * Get the inner CLearToolLauncher.
     * 
     * @return The inner CLearToolLauncher.
     */
    public ClearToolLauncher getLauncher();

    /**
     * Gets the view UUID, for thorough view deletion.
     * 
     * @param viewTag
     * @throws IOException
     * @throws InterruptedException
     */
    Properties getViewData(String viewTag) throws IOException, InterruptedException;

    void logRedundantCleartoolError(String[] cmd, Exception ex);
    
    /**
     * Lock an object. See http://www.ipnom.com/ClearCase-Commands/lock.html
     * @param comment Can be null
     * @param objectSelector Object select. Cannot be null
     * @return true if the lock succeeded.
     * @throws IOException
     * @throws InterruptedException
     */
    boolean lock(String comment, String objectSelector) throws IOException, InterruptedException;

    /**
     * Call lsactivity (see on <a href="http://www.ipnom.com/ClearCase-Commands/lsactivity.html">Rational ClearCase
     * Commands Reference</a> for details)
     * 
     * @param activity Specifies one or more activities to list.<br>
     *            You can specify an activity as a simple name or as an object selector of the form
     *            [activity]:name@vob-selector, where vob-selector specifies a project VOB (see the cleartool reference
     *            page).<br>
     *            If you specify a simple name and the current directory is not a project VOB, this command assumes that
     *            the activity resides in the project VOB associated with the stream attached to the current view.<br>
     *            If the current directory is a project VOB, that project VOB is the context for identifying the
     *            activity.
     * @param commandFormat The output format to be used (-fmt &lt;commandFormat&gt;)
     * @param viewPath view path name to use in order to list activity
     * @return A reader to the lsactivity command output
     * @throws IOException
     * @throws InterruptedException
     */
    Reader lsactivity(String activity, String commandFormat, String viewPath) throws IOException, InterruptedException;
    
    /**
     * List attributes of a baseline
     * @param baselineName
     * @param format
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public String lsbl(String baselineName, String format) throws IOException, InterruptedException;

    /**
     * Given a relative path, return the associated view tag if it exists. Otherwise, it will return null
     * 
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    String lscurrentview(String viewPath) throws IOException, InterruptedException;

    /**
     * Returns Reader containing output from lshistory.
     * 
     * @param format format that should be used by the lshistory command
     * @param lastBuildDate lists events recorded since (that is, at or after) the specified date-time
     * @param viewPath the name of the view
     * @param branch the name of the branch to get history events for; if null then history events for all branches are
     *            listed
     * @param pathsInView view paths that should be added to the lshistory command. The view paths must be relative.
     * @return Reader containing output from command
     */
    Reader lshistory(String format, Date lastBuildDate, String viewPath, String branch, String[] pathsInView) throws IOException, InterruptedException;

    /**
     * List attributes of a project
     * @param viewTag View tag of a view attached to a stream of the project
     * @param format
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    String lsproject(String viewTag, String format) throws InterruptedException, IOException;
    
    /**
     * List attributes of a stream
     * @param stream TODO
     * @param viewTag The view tag of a view on the wanted stream
     * @param format
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    String lsstream(String stream, String viewTag, String format) throws IOException, InterruptedException;
    
    /**
     * Lists view registry entries. This command needs to be run inside a view.
     * 
     * @param onlyActiveDynamicViews true for only return active dynamic views; false all views are returned
     * @return list of view names
     */
    List<String> lsview(boolean onlyActiveDynamicViews) throws IOException, InterruptedException;

    /**
     * Lists VOB registry entries
     * 
     * @param onlyMounted true for only return mounted vobs; false all vobs are returned
     * @return list of vob names
     */
    List<String> lsvob(boolean onlyMounted) throws IOException, InterruptedException;

    /**
     * Attaches version labels to versions of elements
     * 
     * @param viewPath The view path name (relative to the workspace)
     * @param label the label name
     */
    void mklabel(String viewPath, String label) throws IOException, InterruptedException;
    
    /**
     * Creates a new baseline
     * @param name The base name for the baseline
     * @param viewTag The view from which to create baseline. Baselines are created in the stream linked to this view
     * @param comment
     * @param fullBaseline
     * @param identical
     * @param components
     * @param dDependOn TODO
     * @param aDependOn TODO
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    List<String> mkbl(String name, String viewTag, String comment, boolean fullBaseline, boolean identical, List<String> components, String dDependOn, String aDependOn) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param viewPath The view path name (relative to the workspace)
     * @param viewTag the name of the view
     * @param streamSelector optional stream selector, null if not used.
     * @param launcher launcher for launching the command
     */
    void mkview(String viewPath, String viewTag, String streamSelector) throws IOException, InterruptedException;

    /**
     * Creates and registers a view
     * 
     * @param viewPath The view path name (relative to the workspace)
     * @param viewTag The view tag (unique server identifier for the view)
     * @param streamSelector optional stream selector, null if not used.
     * @param launcher launcher for launching the command
     */
    void mkview(String viewPath, String viewTag, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException;

    /**
     * Mounts all VOBs.
     */
    void mountVobs() throws IOException, InterruptedException;

    /**
     * Retrieves the canonical working directory for a given view.
     * 
     * @param viewPath The view path to use to execute pwv
     * @return the return from "cleartool pwv"
     */
    String pwv(String viewPath) throws IOException, InterruptedException;
    
    /**
     * Rebase a dynamic view
     * @param viewTag the view to rebase. It must be a dynamic view
     * @param baseline The new foundation baseline to use
     * @throws IOException
     * @throws InterruptedException
     */
    void rebaseDynamic(String viewTag, String baseline) throws IOException, InterruptedException;
    
    /**
     * Recommend the latest baselines on the stream that matches the minimum promotion level of the stream
     * @param streamSelector
     * @throws IOException
     * @throws InterruptedException
     */
    void recommendBaseline(String streamSelector) throws IOException, InterruptedException;

    /**
     * Removes the view from a VOB
     * 
     * @param viewPath The path used for the view
     */
    void rmview(String viewPath) throws IOException, InterruptedException;

    /**
     * Removes the view tag from the ClearCase registry - used when the view storage in the workspace has already been
     * deleted.
     * 
     * @param viewTag The view tag (server identifier of the view)
     */
    void rmviewtag(String viewTag) throws IOException, InterruptedException;
    
    /**
     * Removes a view tag or a VOB tag from the networkwide storage registry
     * @param tag
     * @throws IOException
     * @throws InterruptedException
     */
    void rmtag(String tag) throws IOException, InterruptedException;

    /**
     * Removes the view (as identified by UUID) from all VOBs
     * 
     * @param viewUuid the unique identifier for the view
     */
    void rmviewUuid(String viewUuid) throws IOException, InterruptedException;
    
    /**
     * Set the baseline promotion level to the given level. The predefined promotion levels are defined in ClearTool.DefaultPromotionLevel.
     * @param baselineName
     * @param promotionLevel
     * @throws IOException
     * @throws InterruptedException
     */
    void setBaselinePromotionLevel(String baselineName, String promotionLevel) throws IOException, InterruptedException;
    
    void setBaselinePromotionLevel(String baselineName, DefaultPromotionLevel promotionLevel) throws IOException, InterruptedException;

    /**
     * Sets the config spec of the view
     * 
     * @param viewPath The view path name (relative to the workspace)
     * @param option The type of setcs that needs to be performed
     * @param configSpec the name of the file containing a config spec
     */
    void setcs(String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException;
    
    /**
     * Synchronizes the Dynamic UCM view with the streams recommended baseline
     * 
     * @param viewTag
     * @param option The option to use
     * @param configSpec The config spec to apply. If omitted, the view tag
     * @throws IOException
     * @throws InterruptedException
     */
    void setcsTag(String viewTag, SetcsOption option, String configSpec) throws IOException, InterruptedException;
    
    public static enum SetcsOption {
        STREAM, CURRENT, CONFIGSPEC
    }

    /**
     * Starts or connects to a dynamic view's view_server process
     * 
     * @param viewTags One or more currently registered view tags (that is, view tags visible to lsview).
     */
    void startView(String viewTags) throws IOException, InterruptedException;
    
    /**
     * Unlock an object
     * @param comment
     * @param objectSelector
     * @throws IOException
     * @throws InterruptedException
     */
    void unlock(String comment, String objectSelector) throws IOException, InterruptedException;

    /**
     * Unregisters the view tag for a given UUID.
     * 
     * @param viewUuid the unique identifier for the view.
     */
    void unregisterView(String viewUuid) throws IOException, InterruptedException;

    /**
     * Updates the elements in the view
     * 
     * @param viewPath the name of the view
     * @param loadRules optional load rules, null if not used.
     */
    void update(String viewPath, String[] loadRules) throws IOException, InterruptedException;

}
