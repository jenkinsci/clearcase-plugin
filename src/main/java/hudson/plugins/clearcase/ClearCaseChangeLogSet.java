package hudson.plugins.clearcase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester.Digester;
import org.xml.sax.SAXException;

import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogSet;

/**
 * Clear case change log set.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogSet extends ChangeLogSet<ClearCaseChangeLogEntry> {

	static final String[] TAGS = new String[] { "file", "user", "comment", "action", "date", "version" };

	private List<ClearCaseChangeLogEntry> history = null;

	public ClearCaseChangeLogSet(AbstractBuild<?, ?> build, List<ClearCaseChangeLogEntry> logs) {
		super(build);
		for (ClearCaseChangeLogEntry entry : logs) {
			entry.setParent(this);
		}
		this.history = Collections.unmodifiableList(logs);
	}

	@Override
	public boolean isEmptySet() {
		return history.size() == 0;
	}

	public Iterator<ClearCaseChangeLogEntry> iterator() {
		return history.iterator();
	}

	public List<ClearCaseChangeLogEntry> getLogs() {
		return history;
	}

	/**
	 * Parses the change log file and returns a clear case change log set.
	 * 
	 * @param build the build for the change log
	 * @param changeLogFile the change log file
	 * @return the change log set
	 */
	public static ClearCaseChangeLogSet parse(AbstractBuild build, File changeLogFile) throws IOException,
			SAXException {
		return parse(build, new FileInputStream(changeLogFile));
	}

	/**
	 * Parses the change log stream and returns a clear case change log set.
	 * 
	 * @param build the build for the change log
	 * @param changeLogStream input stream containing the change log
	 * @return the change log set
	 */
	public static ClearCaseChangeLogSet parse(AbstractBuild build, InputStream changeLogStream) throws IOException,
			SAXException {

		ArrayList<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();

		// Parse the change log file.
		Digester digester = new Digester();
		digester.setClassLoader(ClearCaseChangeLogSet.class.getClassLoader());
		digester.push(history);
		digester.addObjectCreate("*/entry", ClearCaseChangeLogEntry.class);

		digester.addBeanPropertySetter("*/entry/date", "dateStr");
		digester.addBeanPropertySetter("*/entry/comment");
		digester.addBeanPropertySetter("*/entry/user");
		digester.addBeanPropertySetter("*/entry/file");
		digester.addBeanPropertySetter("*/entry/action");
		digester.addBeanPropertySetter("*/entry/version");
		
		digester.addSetNext("*/entry", "add");
		digester.parse(changeLogStream);

		return new ClearCaseChangeLogSet(build, history);
	}

	/**
	 * Stores the history objects to the output stream as xml
	 * 
	 * @param outputStream the stream to write to
	 * @param history the history objects to store
	 * @throws IOException
	 */
	public static void saveToChangeLog(OutputStream outputStream, List<ClearCaseChangeLogEntry> history) throws IOException {
		PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

		int tagcount = ClearCaseChangeLogSet.TAGS.length;
		stream.println("<?xml version='1.0' encoding='UTF-8'?>");
		stream.println("<history>");
		for (ClearCaseChangeLogEntry entry : history) {
			stream.println("\t<entry>");
			String[] strings = getEntryAsStrings(entry);
			for (int tag = 0; tag < tagcount; tag++) {
				stream.print("\t\t<");
				stream.print(ClearCaseChangeLogSet.TAGS[tag]);
				stream.print('>');
				stream.print(escapeForXml(strings[tag]));
				stream.print("</");
				stream.print(ClearCaseChangeLogSet.TAGS[tag]);
				stream.println('>');
			}
			stream.println("\t</entry>");
		}
		stream.println("</history>");
		stream.close();
	}
	
	private static String[] getEntryAsStrings(ClearCaseChangeLogEntry entry) {
		String[] array = new String[TAGS.length];
		array[0] = entry.getFile();
		array[1] = entry.getUser();
		array[2] = entry.getComment();
		array[3] = entry.getAction();
		array[4] = entry.getDateStr();
		array[5] = entry.getVersion();
		return array;
	}

	private static String escapeForXml(String string) {
		if (string == null) {
			return null;
		}

		// Loop through and replace the special chars.
		int size = string.length();
		char ch = 0;
		StringBuffer escapedString = new StringBuffer(size);
		for (int index = 0; index < size; index++) {
			// Convert special chars.
			ch = string.charAt(index);
			switch (ch) {
			case '&':
				escapedString.append("&amp;");
				break;
			case '<':
				escapedString.append("&lt;");
				break;
			case '>':
				escapedString.append("&gt;");
				break;
			case '\'':
				escapedString.append("&apos;");
				break;
			case '\"':
				escapedString.append("&quot;");
				break;
			default:
				escapedString.append(ch);
			}
		}

		return escapedString.toString();
	}

}
