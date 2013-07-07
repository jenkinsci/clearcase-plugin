/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
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
package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.MkViewParameters;
import hudson.plugins.clearcase.ViewType;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;

/**
 * Check out action that will check out files into a snapshot view.
 */
public abstract class SnapshotCheckoutAction extends CheckoutAction {

    public static class LoadRulesDelta {
        private final Set<String> removed;
        private final Set<String> added;

        public LoadRulesDelta(Set<String> removed, Set<String> added) {
            super();
            this.removed = removed;
            this.added = added;
        }

        public String[] getAdded() {
            return added.toArray(new String[added.size()]);
        }

        public String[] getRemoved() {
            return removed.toArray(new String[removed.size()]);
        }

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty();
        }
    }

    protected final String[] loadRules;
    protected final boolean  useUpdate;
    protected final String   viewPath;

    public SnapshotCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate, String viewPath, ViewStorage viewStorage) {
        super(cleartool, viewStorage);
        this.loadRules = loadRules;
        this.useUpdate = useUpdate;
        this.viewPath = viewPath;
    }

    /**
     * @deprecated Use {@link #isViewValid(FilePath,String)} instead
     */
    @Override
    @Deprecated
    public boolean isViewValid(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException {
        return isViewValid(workspace, viewTag);
    }

    @Override
    public boolean isViewValid(FilePath workspace, String viewTag) throws IOException, InterruptedException {
        Validate.notEmpty(viewPath);
        FilePath filePath = new FilePath(workspace, viewPath);
        boolean viewPathExists = filePath.exists();
        try {
            String currentViewTag = getCleartool().lscurrentview(viewPath);
            return getCleartool().doesViewExist(viewTag) && viewPathExists && viewTag.equals(currentViewTag);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Manages the re-creation of the view if needed. If something exists but not referenced correctly as a view, it will be renamed and the view will be created
     * @param workspace The job's workspace
     * @param jobViewTag The view identifier on server. Must be unique on server
     * @param viewPath The workspace relative path of the view
     * @param streamSelector The stream selector, using streamName[@pvob] format
     * @return true if a mkview has been done, false if a view existed and is reused
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean cleanAndCreateViewIfNeeded(FilePath workspace, String jobViewTag, String viewPath, String streamSelector) throws IOException, InterruptedException {
        Validate.notEmpty(viewPath);
        ClearTool ct = getCleartool();
        TaskListener listener = ct.getLauncher().getListener();
        FilePath filePath = new FilePath(workspace, viewPath);
        boolean viewPathExists = filePath.exists();
        boolean doViewCreation = true;
        PrintStream logger = listener.getLogger();
        if (ct.doesViewExist(jobViewTag)) {
            if (viewPathExists) {
                String currentViewTag = ct.lscurrentview(viewPath);
                if (jobViewTag.equals(currentViewTag)) {
                    if (useUpdate) {
                        doViewCreation = false;
                    } else {
                        logger.println("Removing view because 'Use Update' isn't checked.");
                        ct.rmview(viewPath);
                    }
                } else if (currentViewTag != null) {
                    logger.println("Removing view because the view tag of the job " + jobViewTag + " doesn't match the current view tag " + currentViewTag);
                    ct.rmview(viewPath);
                    logger.println("Removing the job view tag because we detected that it already exists.");
                    rmviewtag(jobViewTag);
                } else {
                    logger.println("The view directory is not linked to any view tag. Removing it using OS delete.");
                    filePath.deleteRecursive();
                }
            } else {
                logger.println("Removing view tag because it exists, but the view path doesn't.");
                rmviewtag(jobViewTag);
            }
        } else {
            if (viewPathExists) {
                String currentViewTag = ct.lscurrentview(viewPath);
                if (currentViewTag != null) {
                    logger.println("Removing view because it doesn't match with our view tag.");
                    ct.rmview(viewPath);
                } else {
                    logger.println("The view directory is not linked to any view tag. Removing it using OS delete.");
                    filePath.deleteRecursive();
                }
            }
        }
        if (doViewCreation) {
            MkViewParameters params = new MkViewParameters();
            params.setType(ViewType.Snapshot);
            params.setViewPath(viewPath);
            params.setViewTag(jobViewTag);
            params.setStreamSelector(streamSelector);
            params.setViewStorage(getViewStorage());
            ct.mkview(params);
        }
        return doViewCreation;
    }

    private void rmviewtag(String viewTag) throws InterruptedException, IOException {
        try {
            getCleartool().rmviewtag(viewTag);
        } catch (IOException e) {
            // ClearCase RT doesn't support rmview -tag
            getCleartool().rmtag(viewTag);
        }
    }

    protected SnapshotCheckoutAction.LoadRulesDelta getLoadRulesDelta(Set<String> configSpecLoadRules, Launcher launcher) {
        Set<String> removedLoadRules = new LinkedHashSet<String>(configSpecLoadRules);
        Set<String> addedLoadRules = new LinkedHashSet<String>();
        if (!ArrayUtils.isEmpty(loadRules)) {
            for (String loadRule : loadRules) {
                addedLoadRules.add(ConfigSpec.cleanLoadRule(loadRule, launcher.isUnix()));
            }
            removedLoadRules.removeAll(addedLoadRules);
            addedLoadRules.removeAll(configSpecLoadRules);
            PrintStream logger = launcher.getListener().getLogger();
            for (String removedLoadRule : removedLoadRules) {
                logger.println("Removed load rule : " + removedLoadRule);
            }
            for (String addedLoadRule : addedLoadRules) {
                logger.println("Added load rule : " + addedLoadRule);
            }
        }
        return new SnapshotCheckoutAction.LoadRulesDelta(removedLoadRules, addedLoadRules);
    }
}
