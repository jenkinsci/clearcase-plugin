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
import hudson.util.VariableResolver;
import hudson.plugins.clearcase.ucm.UcmHistoryAction;

import hudson.plugins.clearcase.history.HistoryAction;

public class ClearCaseUcmSCMDummy extends ClearCaseUcmSCM {

    private ClearTool cleartool;
    private ClearCaseUcmScmDescriptor clearCaseUcmScmDescriptor;
    private Computer overrideComputer;
    
    public ClearCaseUcmSCMDummy(String stream, String loadrules, String viewname,
                                boolean usedynamicview, String viewdrive,
                                String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent,
                                boolean useUpdate, boolean rmviewonrename,
                                String excludedRegions, String multiSitePollBuffer,
                                String overrideBranchName, boolean createDynView,
                                ClearTool cleartool,
                                ClearCaseUcmScmDescriptor clearCaseUcmScmDescriptor) {
        this(stream, loadrules, viewname, usedynamicview,
             viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate,
             rmviewonrename, excludedRegions, multiSitePollBuffer, overrideBranchName, createDynView,
             cleartool, clearCaseUcmScmDescriptor, null);
    }
    
    public ClearCaseUcmSCMDummy(String stream, String loadrules, String viewname,
                                boolean usedynamicview, String viewdrive,
                                String mkviewoptionalparam, boolean filterOutDestroySubBranchEvent,
                                boolean useUpdate, boolean rmviewonrename,
                                String excludedRegions, String multiSitePollBuffer,
                                String overrideBranchName, boolean createDynView,
                                ClearTool cleartool,
                                ClearCaseUcmScmDescriptor clearCaseUcmScmDescriptor,
                                Computer overrideComputer) {
        super(stream, loadrules, viewname, usedynamicview,
              viewdrive, mkviewoptionalparam, filterOutDestroySubBranchEvent, useUpdate,
              rmviewonrename, excludedRegions, multiSitePollBuffer, overrideBranchName, createDynView, overrideBranchName, overrideBranchName, true, true, false);
        this.cleartool = cleartool;
        this.clearCaseUcmScmDescriptor = clearCaseUcmScmDescriptor;
        this.overrideComputer = overrideComputer;
    }
    
    @Override
    protected ClearTool createClearTool(VariableResolver variableResolver,
                                        ClearToolLauncher launcher) {
        return cleartool;
    }
    
    @Override
    public Computer getCurrentComputer() {
        return overrideComputer;
    }
    
    @Override
    public HistoryAction createHistoryAction(
                                             VariableResolver variableResolver, ClearToolLauncher launcher, AbstractBuild build) {
        return super.createHistoryAction(variableResolver, launcher, null);
    }
    
    @Override
    public ClearCaseUcmScmDescriptor getDescriptor() {
        return clearCaseUcmScmDescriptor;
    }
}

