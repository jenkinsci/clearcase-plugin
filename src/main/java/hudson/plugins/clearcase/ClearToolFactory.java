package hudson.plugins.clearcase;

import hudson.model.TaskListener;

public interface ClearToolFactory {
    ClearTool create(ClearCaseSCM scm, TaskListener listener);
}
