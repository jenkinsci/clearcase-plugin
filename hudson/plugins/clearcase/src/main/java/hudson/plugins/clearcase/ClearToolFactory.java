package hudson.plugins.clearcase;

import hudson.model.TaskListener;

/**
 * Interface so the Clear Case SCM can create clear tools on the go.
 * This helps in unit testing as the ClearCaseScmDescriptor can not be used
 * in unit tests.
 */
public interface ClearToolFactory {
    ClearTool create(ClearCaseSCM scm, TaskListener listener);
}
