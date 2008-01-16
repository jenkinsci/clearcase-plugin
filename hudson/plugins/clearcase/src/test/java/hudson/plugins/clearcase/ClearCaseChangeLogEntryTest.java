package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import java.util.Calendar;
import org.junit.Test;

public class ClearCaseChangeLogEntryTest {

    @Test
    public void testSetFormattedDateStr() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setDateStr("28/08/2007 15:27:00");
        assertEquals("The date str is incorrect", "28/08/2007 15:27:00", entry.getDateStr());

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2007, 7, 28, 15, 27, 0);
        assertEquals("The date is incorrect", calendar.getTime(), entry.getDate());
    }

    @Test
    public void testSetNonFormattedDateStr() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setDateStr("Tue Aug 28 15:27:00 CEST 2007");
        assertEquals("Tue Aug 28 15:27:00 CEST 2007", entry.getDateStr());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSetFileAndThenVersion() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setFile("Filename");
        entry.setVersion("version1");
        assertEquals("Filename", entry.getElements().get(0).getFile());
        assertEquals("version1", entry.getElements().get(0).getVersion());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSetVersionAndThenFile() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setVersion("version1");
        entry.setFile("Filename");
        assertEquals("Filename", entry.getElements().get(0).getFile());
        assertEquals("version1", entry.getElements().get(0).getVersion());
    }

}
