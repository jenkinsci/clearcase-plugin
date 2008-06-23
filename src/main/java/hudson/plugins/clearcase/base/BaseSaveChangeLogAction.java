package hudson.plugins.clearcase.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearCaseChangeLogSet;
import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.scm.ChangeLogSet.Entry;

/**
 * Save change log action for Base ClearCase
 */
public class BaseSaveChangeLogAction implements SaveChangeLogAction {

    public void saveChangeLog(File changeLogFile, List<? extends Entry> entries) throws IOException, InterruptedException {
        FileOutputStream fileOutputStream = new FileOutputStream(changeLogFile);
        ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, (List<ClearCaseChangeLogEntry>) entries);
        fileOutputStream.close();
    }
}
