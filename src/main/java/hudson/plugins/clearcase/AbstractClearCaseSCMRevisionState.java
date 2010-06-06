package hudson.plugins.clearcase;

import hudson.scm.SCMRevisionState;

import java.util.Date;

public abstract class AbstractClearCaseSCMRevisionState extends SCMRevisionState {

    protected final Date buildTime;

    public AbstractClearCaseSCMRevisionState(Date buildTime) {
        super();
        this.buildTime = buildTime;
    }

    public Date getBuildTime() {
        return buildTime;
    }

}