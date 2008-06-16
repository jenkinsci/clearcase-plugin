package hudson.plugins.clearcase.action;

import hudson.scm.ChangeLogSet.Entry;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Action that stores a change log into a file.
 */
public interface SaveChangeLogAction {
    /**
     * Store the change log into the specified file.
     * @param changeLogFile file to write the change log to (as XML)
     * @param entries the entries in the change log
     */
    void saveChangeLog(File changeLogFile, List<? extends Entry> entries) throws IOException, InterruptedException;        
}
