/*
 * The MIT License
 *
 * Copyright (c) 2009-2010, Manufacture Francaise des Pneumatiques Michelin, Romain Seguy
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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.TaskListener;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.util.FormValidation;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jvnet.localizer.ResourceBundleHolder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Corresponds to an IBM ClearCase installation.
 *
 * <p>This {@link Extension} aims at allowing to set a specific ClearCase home
 * folder for each node of an Hudson instance (which is useful if ClearCase is
 * not in the {@code PATH} of all the nodes and if the ClearCase installation
 * folder is not the same on each node).</p>
 * <p>This {@link ToolInstallation} is NOT {@link EnvironmentSpecific}, it is
 * only {@link NodeSpecific}.</p>
 *
 * @author Romain Seguy (http://davadoc.deviantart.com)
 */
public class ClearCaseInstallation extends ToolInstallation implements NodeSpecific<ClearCaseInstallation> {

    public final static String NAME = "ClearCase";
    public final static String CLEARTOOL_EXE = "bin/cleartool";
    public final static String CLEARTOOL_EXE_FALLBACK = "cleartool";

    @DataBoundConstructor
    public ClearCaseInstallation(String home) {
        super(NAME, home, Collections.EMPTY_LIST);
    }

    public ClearCaseInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new ClearCaseInstallation(translateFor(node, log));
    }

    public String getCleartoolExe(Node node, TaskListener listener) throws IOException, InterruptedException {
        ClearCaseInstallation installation = this;
        installation = installation.forNode(node, listener);
        if (StringUtils.isNotBlank(installation.getHome())) {
            // If an installation is specified, use it
            return PathUtil.convertPathForOS(installation.getHome() + "/" + CLEARTOOL_EXE, node.createLauncher(listener).decorateFor(node).isUnix());
        } else {
            // Otherwise, fallback to a default case where cleartool is in PATH
            return CLEARTOOL_EXE_FALLBACK;
        }
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<ClearCaseInstallation> {

        public DescriptorImpl() {
            // let's avoid a NullPointerException in getInstallations()
            setInstallations(new ClearCaseInstallation[0]);
            load();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // logmergetimewindow has not been moved there, it is still tight
            // to ClearCaseSCM for backward-compatibility, but its config is now
            // done in the ClearCaseInstallation/global.jelly rather than the
            // former ClearCaseSCM/global.jelly (which has been dropped)
            // ==> we need to delegate the config to the "former" descriptor
            ClearCaseSCM.ClearCaseScmDescriptor desc = Hudson.getInstance().getDescriptorByType(ClearCaseSCM.ClearCaseScmDescriptor.class);
            if(desc == null) {
                desc = new ClearCaseSCM.ClearCaseScmDescriptor();
            }
            desc.configure(req, formData);
            desc.save();

            setInstallations(
                    req.bindJSONToList(
                            ClearCaseInstallation.class,
                            formData.get("clearcaseinstall")).toArray(new ClearCaseInstallation[0]));
            save();
            
            return true;
        }

        public FormValidation doCheckHome(@QueryParameter String value) {
            if(!Hudson.getInstance().hasPermission(Hudson.ADMINISTER)) {
                return FormValidation.ok();
            }

            File clearCaseHome = new File(Util.replaceMacro(value, EnvVars.masterEnvVars));

            if(clearCaseHome.getPath().equals("")) {
                return FormValidation.ok(Messages.ClearCaseInstallation_CleartoolWillBeCalledFromPath());
            }
            
            if(!clearCaseHome.isDirectory()) {
                return FormValidation.error(Messages.ClearCaseInstallation_NotAFolder(value));
            }

            String cleartool = CLEARTOOL_EXE;
            if(Functions.isWindows()) {
                cleartool += ".exe";
            }
            
            if(!new File(clearCaseHome, cleartool).exists()) {
                return FormValidation.error(Messages.ClearCaseInstallation_NotAClearCaseInstallationFolder(value));
            }
            
            return FormValidation.ok();
        }

        @Override
        public String getDisplayName() {
            return Messages.ClearCaseInstallation_DisplayName();
        }

        public ClearCaseInstallation getInstallation() {
            ClearCaseInstallation[] installations = getInstallations();
            if(installations.length > 0) {
                return installations[0];
            }
            return null;
        }
        
        public String getDefaultViewName() {
            return getCCDescriptor().getDefaultViewName();
        }
        
        public String getDefaultViewPath() {
            return getCCDescriptor().getDefaultViewPath();
        }
        
        public String getDefaultWinDynStorageDir() {
            return getCCDescriptor().getDefaultWinDynStorageDir();
        }
        
        public String getDefaultUnixDynStorageDir() {
            return getCCDescriptor().getDefaultUnixDynStorageDir();
        }

        public int getLogMergeTimeWindow() {
            return getCCDescriptor().getLogMergeTimeWindow();
        }
        
        // Keep a ref to descriptor to avoid init each time
        private transient ClearCaseScmDescriptor desc;

        private ClearCaseScmDescriptor getCCDescriptor() {
            // cf. the big comment in the configure() method to know why we have
            // this stuff here
            if(desc == null) {
                desc = Hudson.getInstance().getDescriptorByType(ClearCaseSCM.ClearCaseScmDescriptor.class);
                if (desc == null) {
                    desc = new ClearCaseSCM.ClearCaseScmDescriptor();
                }
                // we apparently have to force the loading
                desc.load();
            }
            return desc;
        }

    }

}
