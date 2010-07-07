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
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ConfigSpec;

import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;


/**
 * Check out action that will check out files into a snapshot view.
 */
public abstract class AbstractCheckoutAction implements CheckOutAction {
    
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

    protected final ClearTool cleartool;
    protected final String[] loadRules;
    protected final boolean useUpdate;
    
    public AbstractCheckoutAction(ClearTool cleartool, String[] loadRules, boolean useUpdate) {
        Validate.notNull(cleartool);
        this.cleartool = cleartool;
        this.loadRules = loadRules;
        this.useUpdate = useUpdate;
    }

    /**
     * Manages the re-creation of the view if needed. If something exists but not referenced correctly as a view, it will be renamed and the view will be created
     * @param workspace The job's workspace
     * @param viewTag The view identifier on server. Must be unique on server
     * @param viewPath The workspace relative path of the view
     * @param streamSelector The stream selector, using streamName[@pvob] format
     * @return true if a mkview has been done, false if a view existed and is reused
     * @throws IOException
     * @throws InterruptedException
     */
    protected boolean cleanAndCreateViewIfNeeded(FilePath workspace, String viewTag, String viewPath, String streamSelector) throws IOException, InterruptedException {
        Validate.notEmpty(viewPath);
        FilePath filePath = new FilePath(workspace, viewPath);
        boolean viewPathExists = filePath.exists();
        boolean doViewCreation = true;
        if (cleartool.doesViewExist(viewTag)) {
            if (viewPathExists) {
                if (viewTag.equals(cleartool.lscurrentview(viewPath))) {
                    if (useUpdate) {
                        doViewCreation = false;
                    } else {
                        cleartool.rmview(viewPath);
                    }
                } else {
                    filePath.renameTo(getUnusedFilePath(workspace, viewPath));
                    rmviewtag(viewTag);
                }
            } else {
                rmviewtag(viewTag);
            }
        } else {
            if (viewPathExists) {
                filePath.renameTo(getUnusedFilePath(workspace, viewPath));
            }
        }
        if (doViewCreation) {
            cleartool.mkview(viewPath, viewTag, streamSelector);
        }
        return doViewCreation;
    }
    
    private void rmviewtag(String viewTag) throws InterruptedException, IOException{
        try {
            cleartool.rmviewtag(viewTag);
        } catch(IOException e) {
            // ClearCase RT doesn't support rmview -tag
            cleartool.rmtag(viewTag);
        }
    }

    protected AbstractCheckoutAction.LoadRulesDelta getLoadRulesDelta(Set<String> configSpecLoadRules, Launcher launcher) {
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
        return new AbstractCheckoutAction.LoadRulesDelta(removedLoadRules, addedLoadRules);
    }

    private FilePath getUnusedFilePath(FilePath workspace, String viewName) throws IOException, InterruptedException {
        for (int i = 1; i < Integer.MAX_VALUE; i++) {
            FilePath result = new FilePath(workspace, viewName + ".keep." + i);
            if (!result.exists()) {
                return result;
            }
        }
        return null;
    }
}
