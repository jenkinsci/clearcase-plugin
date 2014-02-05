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

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.plugins.clearcase.viewstorage.ViewStorage;
import hudson.scm.SCMRevisionState;
import hudson.util.VariableResolver;

import java.io.IOException;

public class ClearCaseSCMDummy extends ClearCaseSCM {
    private ClearCaseScmDescriptor clearCaseScmDescriptor;
    private ClearTool              cleartool;
    private Computer               overrideComputer;

    public ClearCaseSCMDummy(String branch, String label, String configspec, String viewname, boolean useupdate, String loadRules, boolean usedynamicview,
            String viewdrive, String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename,
            String excludedRegions, String multiSitePollBuffer, boolean useTimeRule, boolean createDynView, ClearTool cleartool,
            ClearCaseScmDescriptor clearCaseScmDescriptor) {
        this(branch, label, configspec, viewname, useupdate, loadRules, usedynamicview, viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent,
                doNotUpdateConfigSpec, rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule, createDynView, cleartool, clearCaseScmDescriptor,
                null, viewname, null);
    }

    public ClearCaseSCMDummy(String branch, String label, String configspec, String viewname, boolean useupdate, String loadRules, boolean usedynamicview,
            String viewdrive, String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename,
            String excludedRegions, String multiSitePollBuffer, boolean useTimeRule, boolean createDynView, ClearTool cleartool,
            ClearCaseScmDescriptor clearCaseScmDescriptor, Computer overrideComputer, String viewPath, ViewStorage viewStorage) {
        super(branch, label, false, null, false, null, configspec, viewname, useupdate, false, loadRules, false, null, usedynamicview, viewdrive,
                mkviewoptionalparam, filterOutDestroySubBranchEvent, doNotUpdateConfigSpec, rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule,
                createDynView, viewPath, null, viewStorage);
        this.cleartool = cleartool;
        this.clearCaseScmDescriptor = clearCaseScmDescriptor;
        this.overrideComputer = overrideComputer;
    }

    public ClearCaseSCMDummy(String branch, String label, boolean extractConfigSpec, String configSpecFileName, boolean refreshConfigSpec,
            String refreshConfigSpecCommand, String configSpec, String viewTag, boolean useupdate, boolean extractLoadRules, String loadRules,
            boolean useOtherLoadRulesForPolling, String loadRulesForPolling, boolean usedynamicview, String viewdrive, String mkviewoptionalparam,
            boolean filterOutDestroySubBranchEvent, boolean doNotUpdateConfigSpec, boolean rmviewonrename, String excludedRegions, String multiSitePollBuffer,
            boolean useTimeRule, boolean createDynView, String viewPath, ChangeSetLevel changeset, ViewStorage viewStorage, ClearTool cleartool,
            ClearCaseScmDescriptor clearCaseScmDescriptor, Computer overrideComputer) {
        super(branch, label, extractConfigSpec, configSpecFileName, refreshConfigSpec, refreshConfigSpecCommand, configSpec, viewTag,
                useupdate, extractLoadRules, loadRules, useOtherLoadRulesForPolling, loadRulesForPolling, usedynamicview, viewdrive, mkviewoptionalparam,
                filterOutDestroySubBranchEvent, doNotUpdateConfigSpec, rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule, createDynView,
                viewPath, changeset, viewStorage);
        this.cleartool = cleartool;
        this.clearCaseScmDescriptor = clearCaseScmDescriptor;
        this.overrideComputer = overrideComputer;
    }

    @Override
    public HistoryAction createHistoryAction(VariableResolver variableResolver, ClearToolLauncher launcher, AbstractBuild build, SCMRevisionState baseline,
            boolean useRecurse) throws IOException, InterruptedException {
        return super.createHistoryAction(variableResolver, launcher, null, baseline, useRecurse);
    }

    @Override
    public Computer getCurrentComputer() {
        return overrideComputer;
    }

    @Override
    public ClearCaseScmDescriptor getDescriptor() {
        return clearCaseScmDescriptor;
    }

    @Override
    protected ClearTool createClearTool(VariableResolver variableResolver, ClearToolLauncher launcher) {
        return cleartool;
    }
}
