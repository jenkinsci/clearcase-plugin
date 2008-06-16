package hudson.plugins.clearcase.ucm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import hudson.plugins.clearcase.action.SaveChangeLogAction;
import hudson.scm.ChangeLogSet.Entry;

public class UcmSaveChangeLogAction implements SaveChangeLogAction {

    public void saveChangeLog(File changeLogFile, List<? extends Entry> entries) throws IOException, InterruptedException {
        UcmChangeLogSet.saveToChangeLog(new FileOutputStream(changeLogFile), (List<UcmActivity>) entries);
    }
}
