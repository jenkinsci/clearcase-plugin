package hudson.plugins.clearcase;

import hudson.scm.SCMRevisionState;

import java.util.Date;

public abstract class AbstractClearCaseSCMRevisionState extends SCMRevisionState {

    protected final Date buildTime;
    private String[] loadRules;

    public AbstractClearCaseSCMRevisionState(Date buildTime) {
        super();
        this.buildTime = buildTime;
    }

    public Date getBuildTime() {
        return buildTime;
    }

    public String[] getLoadRules() {
        return loadRules;
    }

    public void setLoadRules(String[] loadRules) {
        this.loadRules = loadRules;
    }

}