package hudson.plugins.clearcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.SAXException;

public class ClearCaseChangeLogSetTest {

	@Test
	public void testParse() throws IOException, SAXException {
		ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, getClass().getResourceAsStream("changelog.xml"));
		List<ClearCaseChangeLogEntry> logs = logSet.getLogs();
		
		Assert.assertEquals("Number of logs are incorrect", 3, logs.size());
	}
	
	@Test
	public void testGetParent() throws IOException, SAXException {
		ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, getClass().getResourceAsStream("changelog.xml"));
		List<ClearCaseChangeLogEntry> logs = logSet.getLogs();
		
		Assert.assertNotNull("The parent of the first entry is null", logs.get(0).getParent());
	}
	
	@Test
	public void testUnicodeXml() throws IOException, SAXException {
		Object[] logEntry = new Object[6];
		logEntry[2] = "Bülow";
		
		List<Object[]> history = new ArrayList<Object[]>();
		history.add(logEntry);
		
		File tempLogFile = File.createTempFile("clearcase", "xml");
		FileOutputStream fileOutputStream = new FileOutputStream(tempLogFile);
		
		ClearCaseChangeLogSet.saveToChangeLog(fileOutputStream, history);
		fileOutputStream.close();
		
		FileInputStream fileInputStream = new FileInputStream(tempLogFile);
		ClearCaseChangeLogSet logSet = ClearCaseChangeLogSet.parse(null, fileInputStream);
		List<ClearCaseChangeLogEntry> logs = logSet.getLogs();
		
		Assert.assertEquals("The comment wasnt correct", "Bülow", logs.get(0).getComment());
	}
}
