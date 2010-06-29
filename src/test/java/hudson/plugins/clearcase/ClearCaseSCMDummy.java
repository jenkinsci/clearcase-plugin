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

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.plugins.clearcase.history.HistoryAction;
import hudson.util.VariableResolver;

public class ClearCaseSCMDummy extends ClearCaseSCM {
    private ClearTool cleartool;
    private ClearCaseScmDescriptor clearCaseScmDescriptor;
    private Computer overrideComputer;
    
    public ClearCaseSCMDummy(String branch, String configspec, String viewname,
                             boolean useupdate, String loadRules, boolean usedynamicview,
                             String viewdrive, String mkviewoptionalparam,
                             boolean filterOutDestroySubBranchEvent,
                             boolean doNotUpdateConfigSpec, boolean rmviewonrename,
                             String excludedRegions, String multiSitePollBuffer,
                             boolean useTimeRule, boolean createDynView,
                             ClearTool cleartool,
                             ClearCaseScmDescriptor clearCaseScmDescriptor) {
        this(branch, configspec, viewname, useupdate, loadRules, usedynamicview,
             viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, doNotUpdateConfigSpec,
             rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule,
             createDynView, cleartool,
             clearCaseScmDescriptor, null, viewname);
    }
    
    public ClearCaseSCMDummy(String branch, String configspec, String viewname,
                             boolean useupdate, String loadRules, boolean usedynamicview,
                             String viewdrive, String mkviewoptionalparam,
                             boolean filterOutDestroySubBranchEvent,
                             boolean doNotUpdateConfigSpec, boolean rmviewonrename,
                             String excludedRegions, String multiSitePollBuffer,
                             boolean useTimeRule, boolean createDynView,
                             ClearTool cleartool,
                             ClearCaseScmDescriptor clearCaseScmDescriptor,
                             Computer overrideComputer, String viewPath) {
        super(branch, configspec, viewname, useupdate, loadRules, usedynamicview,
              viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, doNotUpdateConfigSpec,
              rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule, createDynView, "", "", viewPath);
        this.cleartool = cleartool;
        this.clearCaseScmDescriptor = clearCaseScmDescriptor;
        this.overrideComputer = overrideComputer;
    }
    
    @Override
    protected ClearTool createClearTool(VariableResolver variableResolver,
                                        ClearToolLauncher launcher) {
        return cleartool;
    }

    @Override
    public HistoryAction createHistoryAction(
                                                VariableResolver variableResolver, ClearToolLauncher launcher, AbstractBuild build) throws IOException, InterruptedException {   

        return super.createHistoryAction(variableResolver, launcher, null);
    }
    
    @Override
    public ClearCaseScmDescriptor getDescriptor() {
        return clearCaseScmDescriptor;
    }

    @Override
    public Computer getCurrentComputer() {
        return overrideComputer;
    }
}

