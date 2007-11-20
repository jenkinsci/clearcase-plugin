package hudson.plugins.clearcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ClearCaseChangeLogSetTest {

    @Test
    public void testParse() throws IOException, SAXException {
        ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, getClass()
                .getResourceAsStream("changelog.xml"));
        List<ClearCaseChangeLogEntry> logs = logSet.getLogs();
        Assert.assertEquals("Number of logs are incorrect", 3, logs.size());
        Assert.assertEquals("The user is incorrect", "qhensam", logs.get(0).getUser());
        Assert.assertEquals("The date is incorrect", "Tue Aug 28 15:27:00 CEST 2007", logs.get(0).getDateStr());
    }

    @Test
    public void testGetParent() throws IOException, SAXException {
        ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, getClass()
                .getResourceAsStream("changelog.xml"));
        List<ClearCaseChangeLogEntry> logs = logSet.getLogs();

        Assert.assertNotNull("The parent of the first entry is null", logs.get(0).getParent());
    }

    @Test
    public void testUnicodeXml() throws IOException, SAXException {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setUser("Bülow");
        entry.setAction("action");
        entry.setComment("comment");
        entry.setDate(Calendar.getInstance().getTime());
        entry.setVersion("version");

        List<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();
        history.add(entry);

        File tempLogFile = File.createTempFile("clearcase", "xml");
        FileOutputStream fileOutputStream = new FileOutputStream(tempLogFile);

        ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, history);
        fileOutputStream.close();

        FileInputStream fileInputStream = new FileInputStream(tempLogFile);
        ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, fileInputStream);
        List<ClearCaseChangeLogEntry> logs = logSet.getLogs();

        Assert.assertEquals("The comment wasnt correct", "Bülow", logs.get(0).getUser());
    }
}
