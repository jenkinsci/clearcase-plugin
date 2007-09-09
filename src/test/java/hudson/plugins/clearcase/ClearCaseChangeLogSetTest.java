package hudson.plugins.clearcase;

import java.io.IOException;
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
}
