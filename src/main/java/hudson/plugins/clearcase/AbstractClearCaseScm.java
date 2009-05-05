/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.ItemListener;
import hudson.plugins.clearcase.action.CheckOutAction;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCM;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Abstract class for ClearCase SCM. The class contains the logic around
 * checkout and polling, the deriving classes only have to implement the
 * specific checkout and polling logic.
 */
public abstract class AbstractClearCaseScm extends SCM {
    
    public static final String CLEARCASE_VIEWNAME_ENVSTR = "CLEARCASE_VIEWNAME";
    public static final String CLEARCASE_VIEWPATH_ENVSTR = "CLEARCASE_VIEWPATH";
    
    private final String viewName;
    private final String mkviewOptionalParam;
    private final boolean filteringOutDestroySubBranchEvent;
    private transient String normalizedViewName;
    private final boolean useUpdate;
    private final boolean removeViewOnRename;
    private String excludedRegions;
    
    protected void setNormalizedViewName(String normalizedViewName) {
        this.normalizedViewName = normalizedViewName;
    }
    
    protected String getNormalizedViewName() {
        return normalizedViewName;
    }
    
    public AbstractClearCaseScm(final String viewName,
                                final String mkviewOptionalParam,
                                final boolean filterOutDestroySubBranchEvent,
                                final boolean useUpdate, 
                                final boolean removeViewOnRename,
                                final String excludedRegions) {
        this.viewName = viewName;
        this.mkviewOptionalParam = mkviewOptionalParam;
        this.filteringOutDestroySubBranchEvent = filterOutDestroySubBranchEvent;
        this.useUpdate = useUpdate;
        this.removeViewOnRename = removeViewOnRename;
        this.excludedRegions = excludedRegions;
        createAndRegisterListener();
    }
    
    /**
     * Create a CheckOutAction that will be used by the checkout method.
	 * 
	 * @param launcher
	 *            the command line launcher
	 * @return an action that can check out code from a ClearCase repository.
	 */
	protected abstract CheckOutAction createCheckOutAction(
			VariableResolver variableResolver, ClearToolLauncher launcher);

	// /**
	// * Create a PollAction that will be used by the pollChanges() method.
	// *
	// * @param launcher
	// * the command line launcher
	// * @return an action that can poll if there are any changes a ClearCase
	// * repository.
	// */
	// protected abstract PollAction createPollAction(VariableResolver
	// variableResolver, ClearToolLauncher launcher,List<Filter> filters);

	/**
	 * Create a HistoryAction that will be used by the pollChanges() and
	 * checkout() method.
	 * 
	 * @param launcher
	 *            the command line launcher
	 * @return an action that can poll if there are any changes a ClearCase
	 *         repository.
	 */
	protected abstract HistoryAction createHistoryAction(
			VariableResolver variableResolver, ClearToolLauncher launcher);

	/**
	 * Create a SaveChangeLog action that is used to save a change log
	 * 
	 * @param launcher
	 *            the command line launcher
	 * @return an action that can save a change log to the Hudson changlog file
	 */
	protected abstract SaveChangeLogAction createSaveChangeLogAction(
			ClearToolLauncher launcher);

	// /**
	// * Create a ChangeLogAction that will be used to get the change logs for a
	// * CC repository
	// *
	// * @param launcher
	// * the command line launcher
	// * @param build
	// * the current build
	// * @return an action that returns the change logs for a CC repository
	// */
	// protected abstract ChangeLogAction createChangeLogAction(
	// ClearToolLauncher launcher, AbstractBuild<?, ?> build,
	// Launcher baseLauncher,List<Filter> filters);

	/**
	 * Return string array containing the branch names that should be used when
	 * polling for changes.
	 * 
	 * @return a string array, can not be empty
	 */
	public abstract String[] getBranchNames();

	/**
	 * Return string array containing the paths in the view that should be used
	 * when polling for changes.
	 * 
	 * @param viewPath
	 *            the file path for the view
	 * @return string array that will be used by the lshistory command
	 */
	public abstract String[] getViewPaths(FilePath viewPath)
			throws IOException, InterruptedException;

	@Override
	public boolean supportsPolling() {
		return true;
	}

	@Override
	public boolean requiresWorkspaceForPolling() {
		return true;
	}

	@Override
	public FilePath getModuleRoot(FilePath workspace) {
		if (getNormalizedViewName() == null) {
			return super.getModuleRoot(workspace);
		} else {
			return workspace.child(getNormalizedViewName());
		}
	}

	public String getViewName() {
		if (viewName == null) {
			return "${USER_NAME}_${JOB_NAME}_${NODE_NAME}_view";
		} else {
			return viewName;
		}
	}

	/**
	 * Returns a normalized view name that will be used in cleartool commands.
	 * It will replace ${JOB_NAME} with the name of the job, * ${USER_NAME} with
	 * the name of the user. This way it will be easier to add new jobs without
	 * trying to find an unique view name. It will also replace invalid chars
	 * from a view name.
	 * 
	 * @param project
	 *            the project to get the name from
	 * @return a string containing no invalid chars.
	 */
	public String generateNormalizedViewName(AbstractBuild<?, ?> build,
			Launcher launcher) {
		String generatedNormalizedViewName = viewName;

		generatedNormalizedViewName = Util.replaceMacro(viewName,
				new BuildVariableResolver(build, launcher));

		generatedNormalizedViewName = generatedNormalizedViewName.replaceAll(
				"[\\s\\\\\\/:\\?\\*\\|]+", "_");
		this.normalizedViewName = generatedNormalizedViewName;
		return generatedNormalizedViewName;
	}

	/**
	 * Returns the user configured optional params that will be used in when
	 * creating a new view.
	 * 
	 * @return string containing optional mkview parameters.
	 */
	public String getMkviewOptionalParam() {
		return mkviewOptionalParam;
	}

	/**
	 * Returns if the "Destroyed branch" event should be filtered out or not.
	 * For more information about the boolean, see the full discussion at
	 * http://www.nabble.com/ClearCase-build-triggering-td17507838i20.html
	 * "Usually, CC admins have a CC trigger, fires on an uncheckout event, that
	 * destroys empty branches."
	 * 
	 * @return true if the "Destroyed branch" event should be filtered out or
	 *         not; false otherwise
	 */
	public boolean isFilteringOutDestroySubBranchEvent() {
		return filteringOutDestroySubBranchEvent;
	}

	/**
	 * Adds the env variable for the ClearCase SCMs. CLEARCASE_VIEWNAME - The
	 * name of the clearcase view. CLEARCASE_VIEWPATH - The absolute path to the
	 * clearcase view.
	 */
	@Override
	public void buildEnvVars(AbstractBuild build, Map<String, String> env) {
		if (getNormalizedViewName() != null) {

			env.put(CLEARCASE_VIEWNAME_ENVSTR, getNormalizedViewName());

			String workspace = env.get("WORKSPACE");
			if (workspace != null) {
				env.put(CLEARCASE_VIEWPATH_ENVSTR, workspace + File.separator
						+ getNormalizedViewName());
			}
		}
	}

	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher,
			FilePath workspace, BuildListener listener, File changelogFile)
			throws IOException, InterruptedException {
		ClearToolLauncher clearToolLauncher = createClearToolLauncher(listener,
				workspace, launcher);

		// Create actions
		VariableResolver variableResolver = new BuildVariableResolver(build,
				launcher);
		CheckOutAction checkoutAction = createCheckOutAction(variableResolver,
				clearToolLauncher);
		HistoryAction historyAction = createHistoryAction(variableResolver,
				clearToolLauncher);
		SaveChangeLogAction saveChangeLogAction = createSaveChangeLogAction(clearToolLauncher);

		// Checkout code
		String normalizedViewName = generateNormalizedViewName(build, launcher);
		checkoutAction.checkout(launcher, workspace, normalizedViewName);

		// Gather change log
		List<? extends ChangeLogSet.Entry> changelogEntries = null;
		if (build.getPreviousBuild() != null) {
			Date lastBuildTime = build.getPreviousBuild().getTimestamp()
					.getTime();
			changelogEntries = historyAction.getChanges(lastBuildTime,
					normalizedViewName, getBranchNames(),
					getViewPaths(workspace.child(normalizedViewName)));
		}

		// Save change log
		if ((changelogEntries == null) || (changelogEntries.isEmpty())) {
			// no changes
			return createEmptyChangeLog(changelogFile, listener, "changelog");
		} else {
			saveChangeLogAction.saveChangeLog(changelogFile, changelogEntries);
		}

		return true;
	}

	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher,
			FilePath workspace, TaskListener listener) throws IOException,
			InterruptedException {

		Run lastBuild = project.getLastBuild();
		if (lastBuild == null) {
			return true;
		}

		Date buildTime = lastBuild.getTimestamp().getTime();

		VariableResolver variableResolver = new BuildVariableResolver(
				(AbstractBuild<?, ?>) lastBuild, launcher);

		HistoryAction historyAction = createHistoryAction(variableResolver,
				createClearToolLauncher(listener, workspace, launcher));

		String normalizedViewName = generateNormalizedViewName(
				(AbstractBuild) lastBuild, launcher);

		return historyAction.hasChanges(buildTime, normalizedViewName,
				getBranchNames(), getViewPaths(workspace
						.child(normalizedViewName)));
	}

	/**
	 * Creates a Hudson clear tool launcher.
	 * 
	 * @param listener
	 *            listener to write command output to
	 * @param workspace
	 *            the workspace for the job
	 * @param launcher
	 *            actual launcher to launch commands with
	 * @return a clear tool launcher that uses Hudson for launching commands
	 */
	protected ClearToolLauncher createClearToolLauncher(TaskListener listener,
			FilePath workspace, Launcher launcher) {
		return new HudsonClearToolLauncher(PluginImpl.BASE_DESCRIPTOR
				.getCleartoolExe(), getDescriptor().getDisplayName(), listener,
				workspace, launcher);
	}

	protected ClearTool createClearTool(VariableResolver variableResolver,
			ClearToolLauncher launcher) {
		return new ClearToolSnapshot(variableResolver, launcher,
				mkviewOptionalParam);
	}

	/**
	 * Register listeners for Hudson events. At the moment we listen to
	 * onDeleted and try to remove the ClearCase view that was created for this
	 * job.
	 * 
	 * @param viewName
	 *            the name of the view
	 */
	protected void createAndRegisterListener() {
		Hudson hudson = Hudson.getInstance();
		if (hudson == null) {
			// Probably a JUnit test run?
			Logger.getLogger(AbstractClearCaseScm.class.getName()).log(
					Level.INFO, "Failed to get Hudson instance");

			return;
		}
		hudson.getJobListeners().add(new ItemListener() {

			@Override
			public void onRenamed(Item item, String oldName, String newName) {
				if (removeViewOnRename) {
					onDeleted(item);
				}
			}

			@Override
			public void onDeleted(Item item) {
				if (item instanceof AbstractProject) {
					AbstractProject<?, ?> project = (AbstractProject<?, ?>) item;
					if (project.getScm() instanceof AbstractClearCaseScm) {
						StreamTaskListener listener = new StreamTaskListener(
								System.out);
						Launcher launcher = Hudson.getInstance()
								.createLauncher(listener);
						ClearTool ct = createClearTool(null,
								createClearToolLauncher(listener,
										project.getWorkspace().getParent()
												.getParent(), launcher));
						try {
							ct
									.rmview(generateNormalizedViewName(null,
											launcher));
						} catch (Exception e) {
							Logger.getLogger(
									AbstractClearCaseScm.class.getName()).log(
									Level.WARNING,
									"Failed to remove ClearCase view", e);
						}
					}
				}
			}
		});
	}

	public boolean isUseUpdate() {
		return useUpdate;
	}


	 public String getExcludedRegions() {
		  return excludedRegions;
	 }

	 public String[] getExcludedRegionsNormalized() {
             return excludedRegions == null ? null : excludedRegions.split("[\\r\\n]+");
	 }

    
    protected List<Filter> configureFilters() {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());
        
        String[] excludedStrings = getExcludedRegionsNormalized();
        
        if (excludedStrings != null && excludedStrings.length > 0) {
            for (String s : excludedStrings) {
                if (!s.equals("")) {
                    filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, s));
                }
            }
        }
                                           
	if (isFilteringOutDestroySubBranchEvent()) {
            filters.add(new DestroySubBranchFilter());
        }
        return filters;
    }
}
