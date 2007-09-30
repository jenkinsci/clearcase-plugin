package hudson.plugins.clearcase;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ClearToolHistoryParserTest {

	@Test
	public void testParseNoComment() throws IOException, ParseException {

		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070827.084801\" \"inttest14\" \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());
		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("File is incorrect", "Source\\Definitions\\Definitions.csproj", entry[ClearToolHistoryParser.FILE_INDEX]);
		Assert.assertEquals("User is incorrect", "inttest14", entry[ClearToolHistoryParser.USER_INDEX]);
		Assert.assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 48, 1), entry[ClearToolHistoryParser.DATE_INDEX]);
		Assert.assertEquals("Action is incorrect", "create version", entry[ClearToolHistoryParser.ACTION_INDEX]);
		Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\1", entry[ClearToolHistoryParser.VERSION_INDEX]);
		Assert.assertEquals("Comment is incorrect", "", entry[ClearToolHistoryParser.COMMENT_INDEX]);
	}

	@Test
	public void testEmptyComment() throws IOException, ParseException {
	
		ClearToolHistoryParser parser = new ClearToolHistoryParser();		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		                
		parser.parse(new StringReader("\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"\n"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("Comment is incorrect", "", entry[ClearToolHistoryParser.COMMENT_INDEX]);
	}

	@Test
	public void testCommentWithEmptyLine() throws IOException, ParseException {
	
		ClearToolHistoryParser parser = new ClearToolHistoryParser();		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		                
		parser.parse(new StringReader("\"20070906.091701\"   \"egsperi\"    \"create directory version\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"\ntext\n\nend of comment"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("Comment is incorrect", "text\n\nend of comment", entry[ClearToolHistoryParser.COMMENT_INDEX]);
	}

	@Test
	public void testParseWithComment() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"\nBUG8949")
					, historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());
		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry[ClearToolHistoryParser.FILE_INDEX]);
		Assert.assertEquals("User is incorrect", "aname", entry[ClearToolHistoryParser.USER_INDEX]);
		//Assert.assertEquals("Date is incorrect", getDate(8,27,8,59), entry[ClearToolHistoryParser.DATE_INDEX]);
		Assert.assertEquals("Action is incorrect", "create version", entry[ClearToolHistoryParser.ACTION_INDEX]);
		Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry[ClearToolHistoryParser.VERSION_INDEX]);
		Assert.assertEquals("Comment is incorrect", "BUG8949", entry[ClearToolHistoryParser.COMMENT_INDEX]);
	}

	@Test
	public void testParseWithTwoLineComment() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070827.085901\"   \"aname\"    \"create version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"\nBUG8949\nThis fixed the problem")
					, historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());
		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry[ClearToolHistoryParser.FILE_INDEX]);
		Assert.assertEquals("User is incorrect", "aname", entry[ClearToolHistoryParser.USER_INDEX]);
		//Assert.assertEquals("Date is incorrect", getDate(8, 27, 8, 59), entry[ClearToolHistoryParser.DATE_INDEX]);
		Assert.assertEquals("Action is incorrect", "create version", entry[ClearToolHistoryParser.ACTION_INDEX]);
		Assert.assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry[ClearToolHistoryParser.VERSION_INDEX]);
		Assert.assertEquals("Comment is incorrect", "BUG8949\nThis fixed the problem", entry[ClearToolHistoryParser.COMMENT_INDEX]);
	}
	
	@Test
	public void testParseWithLongAction() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070827.085901\"   \"aname\"    \"create a version\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"\n")
					, historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 1, historyEntries.size());		
		Object[] entry = historyEntries.get(0);
		Assert.assertEquals("Action is incorrect", "create a version", entry[ClearToolHistoryParser.ACTION_INDEX]);
	}


	@Test
	public void testCreateBranchAction() throws IOException, ParseException {
	
		ClearToolHistoryParser parser = new ClearToolHistoryParser();		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070906.091701\"   \"egsperi\"    \"create branch\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"\n"), historyEntries );
		Assert.assertEquals("Number of history entries are incorrect", 0, historyEntries.size());		
	}

	@Test
	public void testFirstVersion() throws IOException, ParseException {	
		ClearToolHistoryParser parser = new ClearToolHistoryParser();		
		List<Object[]> historyEntries = new ArrayList<Object[]>();		                
		parser.parse(new StringReader("\"20070906.091701\"   \"egsperi\"    \"create version\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\0\"\n"), historyEntries );
		Assert.assertEquals("Number of history entries are incorrect", 0, historyEntries.size());		
	}

	@Test
	public void testSorted() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070827.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n" +
									  "\"20070825.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n" +
									  "\"20070830.084801\"   \"inttest1\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 3, historyEntries.size());

		Assert.assertEquals("First entry is incorrect", "inttest1", historyEntries.get(0)[ClearToolHistoryParser.USER_INDEX]);
		Assert.assertEquals("First entry is incorrect", "inttest2", historyEntries.get(1)[ClearToolHistoryParser.USER_INDEX]);
		Assert.assertEquals("First entry is incorrect", "inttest3", historyEntries.get(2)[ClearToolHistoryParser.USER_INDEX]);
	}

	@Test
	public void testMultiline() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070830.084801\"   \"inttest2\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n" +
									  "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 2, historyEntries.size());
	}

	@Test
	public void testErrorOutput() throws IOException, ParseException {
		
		ClearToolHistoryParser parser = new ClearToolHistoryParser();
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		parser.parse(new StringReader("\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n" + 
									  "cleartool: Error: Branch type not found: \"sit_r6a\".\n" +
									  "\"20070830.084801\"   \"inttest3\"  \"create version\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"\n\n"), historyEntries );
		
		Assert.assertEquals("Number of history entries are incorrect", 2, historyEntries.size());
		Assert.assertEquals("First entry is incorrect", "", historyEntries.get(0)[ClearToolHistoryParser.COMMENT_INDEX]);
		Assert.assertEquals("Scond entry is incorrect", "", historyEntries.get(1)[ClearToolHistoryParser.COMMENT_INDEX]);
	}
/*
	@Test
	public void testUserOutput() throws IOException, ParseException {	
		ClearToolHistoryParser parser = new ClearToolHistoryParser();		
		List<Object[]> historyEntries = new ArrayList<Object[]>();		                
		parser.parse(new InputStreamReader(getClass().getResourceAsStream("cleartool-output.txt")), historyEntries );
		Assert.assertEquals("Number of history entries are incorrect", 3, historyEntries.size());		
	}
*/
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
