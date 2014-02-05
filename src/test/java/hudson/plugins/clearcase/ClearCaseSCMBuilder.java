package hudson.plugins.clearcase;

import hudson.model.Computer;
import hudson.plugins.clearcase.AbstractClearCaseScm.ChangeSetLevel;
import hudson.plugins.clearcase.ClearCaseSCM.ClearCaseScmDescriptor;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

public class ClearCaseSCMBuilder {

    public String         branch;
    public String         label;
    public String         configspec;
    public String         viewTag;
    public boolean        useupdate;
    public String         loadRules;
    public boolean        usedynamicview;
    public String         viewdrive;
    public String         mkviewoptionalparam;
    public boolean        filterOutDestroySubBranchEvent;
    public boolean        doNotUpdateConfigSpec;
    public boolean        rmviewonrename;
    public boolean        extractConfigSpec;
    public String         configSpecFileName;
    public boolean        refreshConfigSpec;
    public String         refreshConfigSpecCommand;
    public boolean        extractLoadRules;
    public boolean        useOtherLoadRulesForPolling;
    public String         loadRulesForPolling;
    public String         excludedRegions;
    public String         multiSitePollBuffer;
    public boolean        useTimeRule;
    public boolean        createDynView;
    public String         viewPath;
    public ChangeSetLevel changeset;
    public ViewStorage    viewStorage;
    public ClearTool cleartool;
    public ClearCaseScmDescriptor clearCaseScmDescriptor;
    public Computer overrideComputer;

    public ClearCaseSCM build() {
        return new ClearCaseSCMDummy(branch, label, extractConfigSpec, configSpecFileName, refreshConfigSpec, refreshConfigSpecCommand, configspec, viewTag,
                useupdate, extractLoadRules, loadRules, useOtherLoadRulesForPolling, loadRulesForPolling, usedynamicview, viewdrive, mkviewoptionalparam,
                filterOutDestroySubBranchEvent, doNotUpdateConfigSpec, rmviewonrename, excludedRegions, multiSitePollBuffer, useTimeRule, createDynView,
                viewPath, changeset, viewStorage, cleartool, clearCaseScmDescriptor, overrideComputer);
    }
}
