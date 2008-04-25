package hudson.plugins.clearcase.action;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearTool;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Default action for polling for changes in a repository.
 */
public class DefaultPollAction implements PollAction {

    private ClearTool cleartool;

    public DefaultPollAction(ClearTool cleartool) {
        this.cleartool = cleartool;
    }

    public List<ClearCaseChangeLogEntry> getChanges(Date time, String viewName, String branchName, String vobPaths) throws IOException, InterruptedException {
        return cleartool.lshistory(time, viewName, branchName, vobPaths);
    }
}
