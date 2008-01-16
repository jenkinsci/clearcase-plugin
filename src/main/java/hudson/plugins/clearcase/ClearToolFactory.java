package hudson.plugins.clearcase;

import hudson.model.TaskListener;
import hudson.plugins.clearcase.util.ChangeLogEntryMerger;

/**
 * Interface so the ClearCase SCM can create cleartools on the go.
 * This helps in unit testing as the ClearCaseScmDescriptor can not be used
 * in unit tests.
 */
public interface ClearToolFactory {
    ClearTool create(ClearCaseSCM scm, TaskListener listener);
    ChangeLogEntryMerger createChangeLogEntryMerger(ClearCaseSCM scm);
}
