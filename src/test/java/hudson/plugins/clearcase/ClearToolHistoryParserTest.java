package hudson.plugins.clearcase;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClearToolHistoryParserTest {

    @Test
    public void testParseNoComment() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070827.084801\" \"inttest14\" \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("File is incorrect", "Source\\Definitions\\Definitions.csproj", entry.getElements().get(0).getFile());
        Assert.assertEquals("User is incorrect", "inttest14", entry.getUser());
        Assert.assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 48, 1), entry.getDate());
        Assert.assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\1", entry.getElements().get(0).getVersion());
        Assert.assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testEmptyComment() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testCommentWithEmptyLine() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\ntext\n\nend of comment"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("Comment is incorrect", "text\n\nend of comment", entry.getComment());
    }

    @Test
    public void testParseWithComment() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\nBUG8949"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        Assert.assertEquals("User is incorrect", "aname", entry.getUser());
        Assert.assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        Assert.assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        Assert.assertEquals("Comment is incorrect", "BUG8949", entry.getComment());
    }

    @Test
    public void testParseWithTwoLineComment() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\nBUG8949\nThis fixed the problem"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        Assert.assertEquals("User is incorrect", "aname", entry.getUser());
        Assert.assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        Assert.assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        Assert.assertEquals("Comment is incorrect", "BUG8949\nThis fixed the problem", entry.getComment());
    }

    @Test
    public void testParseWithLongAction() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070827.085901\"   \"aname\"    \"create a version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"  \"mkelem\"\n"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        Assert.assertEquals("Action is incorrect", "create a version", entry.getElements().get(0).getAction());
    }

    @Test
    public void testCreateBranchAction() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create branch\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n"
                      + "\"20070906.091701\"   \"egsperi\"    \"create branch\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n"));
        Assert.assertEquals("Number of history entries are incorrect", 0, entries.size());
    }

    @Test
    public void testFirstVersion() throws IOException, ParseException {
        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\0\"  \"mkelem\"\n"
                      + "\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\0\"  \"mkelem\"\n"));
        Assert.assertEquals("Number of history entries are incorrect", 0, entries.size());
    }

    @Test
    public void testFirstVersionOnLinux() throws IOException, ParseException {
        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"    \"create version\" \"/ApplicationConfiguration\" \"/main/sit_r6a/0\"  \"mkelem\"\n"
                      + "\"20070906.091701\"   \"egsperi\"    \"create version\" \"/ApplicationConfiguration\" \"/main/sit_r6a/0\"  \"mkelem\"\n"));
        Assert.assertEquals("Number of history entries are incorrect", 0, entries.size());
    }

    @Test
    public void testSorted() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070827.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                      + "\"20070825.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                      + "\"20070830.084801\"   \"inttest1\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"));

        Assert.assertEquals("Number of history entries are incorrect", 3, entries.size());

        Assert.assertEquals("First entry is incorrect", "inttest1", entries.get(0).getUser());
        Assert.assertEquals("First entry is incorrect", "inttest2", entries.get(1).getUser());
        Assert.assertEquals("First entry is incorrect", "inttest3", entries.get(2).getUser());
    }

    @Test
    public void testMultiline() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070830.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n"
                      + "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"));

        Assert.assertEquals("Number of history entries are incorrect", 2, entries.size());
    }

    @Test
    public void testErrorOutput() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"
                      + "cleartool: Error: Branch type not found: \"sit_r6a\".\n"
                      + "\"20070829.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"mkelem\"\n\n"));

        Assert.assertEquals("Number of history entries are incorrect", 2, entries.size());
        Assert.assertEquals("First entry is incorrect", "", entries.get(0).getComment());
        Assert.assertEquals("Scond entry is incorrect", "", entries.get(1).getComment());
    }

    @Test
    public void testUserOutput() throws IOException, ParseException {
        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> list = parser.parse(new InputStreamReader(ClearToolHistoryParser.class.getResourceAsStream(
                "ct-lshistory-1.log")));
        Assert.assertEquals("Number of history entries are incorrect", 3, list.size());
    }

    @Test
    public void testOperation() throws IOException, ParseException {

        ClearToolHistoryParser parser = new ClearToolHistoryParser();
        List<ClearCaseChangeLogEntry> entries = parser
                .parse(new StringReader(
                        "\"20070906.091701\"   \"egsperi\"  \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"mkelem\"\n"));

        Assert.assertEquals("Number of history entries are incorrect", 1, entries.size());
        FileElement element = entries.get(0).getElements().get(0);
        Assert.assertEquals("Status is incorrect", "mkelem", element.getOperation());
    }
    
    private Date getDate(int year, int month, int day, int hour, int min, int sec) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        return calendar.getTime();
    }
}
