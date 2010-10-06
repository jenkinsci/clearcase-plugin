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
package hudson.plugins.clearcase;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Hudson;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.listeners.ItemListener;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.scm.SCM;
import hudson.util.StreamTaskListener;
import hudson.util.VariableResolver;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;

@Extension
public class ItemListenerImpl extends ItemListener {

    private static class JobNameOverrideBuildVariableResolver extends BuildVariableResolver {

        private String jobName;

        public JobNameOverrideBuildVariableResolver(String jobName, AbstractBuild<?, ?> build, Computer computer) {
            super(build);
            this.jobName = jobName;
        }

        @Override
        public String resolve(String key) {
            if ("JOB_NAME".equals(key)) {
                return jobName;
            } else {
                return super.resolve(key);
            }
        }
    }

    /**
     * Delete the view when the job is renamed
     */
    @Override
    public void onRenamed(Item item, String oldName, String newName) {
        Hudson hudson = getHudsonFromItem(item);
        if (item instanceof AbstractProject<?, ?>) {
            @SuppressWarnings("unchecked") AbstractProject project = (AbstractProject) item;
            SCM scm = project.getScm();
            if (scm instanceof AbstractClearCaseScm) {
                try {
                    AbstractClearCaseScm ccScm = (AbstractClearCaseScm) scm;
                    if (!ccScm.isRemoveViewOnRename()) {
                        return;
                    }
                    StreamTaskListener listener = StreamTaskListener.fromStdout();
                    Launcher launcher = hudson.createLauncher(listener);

                    AbstractBuild<?, ?> build = (AbstractBuild<?, ?>) project.getSomeBuildWithWorkspace();

                    if (build != null) {
                        VariableResolver<String> variableResolver = new JobNameOverrideBuildVariableResolver(oldName, build, ccScm.getBuildComputer(build));
                        String normalizedViewName = ccScm.generateNormalizedViewName(variableResolver);
                        FilePath workspace;
                        if (isFreeStyleProjectAndHasCustomWorkspace(project)) {
                            workspace = new FilePath(launcher.getChannel(), ((FreeStyleProject) project).getCustomWorkspace());
                        } else {
                            if (build.getBuiltOn() == hudson) {
                                workspace = build.getWorkspace().getParent().getParent().child(newName).child("workspace");
                            } else {
                                workspace = build.getWorkspace();
                            }
                        }
                        ClearTool ct = ccScm.createClearTool(null, ccScm.createClearToolLauncher(listener, workspace, launcher));

                        if (ct.doesViewExist(normalizedViewName)) {
                            String viewPath = ccScm.getViewPath(new VariableResolver.ByMap<String>(build.getEnvironment(listener)));
                            if (workspace.child(viewPath).exists()) {
                                ct.rmview(viewPath);
                            } else {
                                ct.rmviewtag(normalizedViewName);
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.WARNING, "Failed to remove ClearCase view", e);
                }
            }
        }
    }

    private boolean isFreeStyleProjectAndHasCustomWorkspace(@SuppressWarnings("unchecked") AbstractProject project) {
        if (project instanceof FreeStyleProject) {
            FreeStyleProject fsProject = (FreeStyleProject) project;
            return StringUtils.isNotEmpty(fsProject.getCustomWorkspace());
        } else {
            return false;
        }
    }

    private Hudson getHudsonFromItem(Item item) {
        ItemGroup<? extends Item> itemGroup = item.getParent();
        Hudson hudson = null;
        // Go up to Hudson instance
        while (hudson == null) {
            if (itemGroup instanceof Hudson) {
                hudson = (Hudson) itemGroup;
            } else if (itemGroup instanceof TopLevelItem) {
                hudson = ((TopLevelItem) itemGroup).getParent();
            } else {
                itemGroup = ((Item) itemGroup).getParent();
            }
        }
        return hudson;
    }

    /**
     * Delete the view when the job is deleted
     */
    @Override
    public void onDeleted(Item item) {
        Hudson hudson = getHudsonFromItem(item);
        if (item instanceof AbstractProject<?, ?>) {
            AbstractProject<?, ?> project = (AbstractProject<?, ?>) item;
            SCM scm = project.getScm();
            if (scm instanceof AbstractClearCaseScm) {
                try {
                    AbstractClearCaseScm ccScm = (AbstractClearCaseScm) scm;
                    if (ccScm.isUseDynamicView() && !ccScm.isCreateDynView()) {
                        return;
                    }
                    StreamTaskListener listener = StreamTaskListener.fromStdout();
                    Launcher launcher = hudson.createLauncher(listener);
                    ClearTool ct = ccScm.createClearTool(null, ccScm.createClearToolLauncher(listener, project.getSomeWorkspace().getParent().getParent(),
                            launcher));

                    // Adding checks to avoid NPE in HUDSON-4869
                    if (project.getLastBuild() != null) {
                        // Create a variable resolver using the last build's computer - HUDSON-5364
                        VariableResolver<String> variableResolver = new BuildVariableResolver(project.getLastBuild());

                        // Workspace has already been removed, so the view needs to be unregistered
                        String normalizedViewName = ccScm.generateNormalizedViewName(variableResolver);
                        ct.rmviewtag(normalizedViewName);
                    }
                } catch (Exception e) {
                    Logger.getLogger(AbstractClearCaseScm.class.getName()).log(Level.WARNING, "Failed to remove ClearCase view", e);
                }
            }
        }
    }
}