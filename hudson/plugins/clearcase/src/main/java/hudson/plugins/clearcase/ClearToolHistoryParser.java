package hudson.plugins.clearcase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for the clear tool history output.
 * 
 * @author Erik Ramfelt
 */
public class ClearToolHistoryParser {

	public static final int FILE_INDEX = 0;
	public static final int USER_INDEX = 1;
	public static final int COMMENT_INDEX = 2;
	public static final int ACTION_INDEX = 3;
	public static final int DATE_INDEX = 4;
	public static final int VERSION_INDEX = 5;

	private final transient Pattern pattern;
	private final transient SimpleDateFormat dateFormatter;

	public ClearToolHistoryParser() {
		//pattern = Pattern.compile("^(\\S+)\\s+(\\w+)\\s+(.+)\\s+\"(.+)@@(.+)\"");
		pattern = Pattern.compile("\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"");
		dateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
	}
	
	/**
	 * Returns the log format that the parser supports
	 * @return the format for the 'cleartool lshistory' command 
	 */
	public static String getLogFormat() {
		return "\\\"%Nd\\\" \\\"%u\\\" \\\"%e\\\" \\\"%En\\\" \\\"%Vn\\\"\\n%c\\n";
	}

	public void parse(Reader inReader, List<Object[]> historyEntries) throws IOException, ParseException {

		BufferedReader reader = new BufferedReader(inReader);

		Object[] content = null;
		StringBuilder commentBuilder = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			
			if (!line.startsWith("cleartool: Error:")) {
				Matcher matcher = pattern.matcher(line);		
				if (matcher.find() && matcher.groupCount() == 5) {					
					if (content != null) {
						content[COMMENT_INDEX] = commentBuilder.toString();
						if (! (		((String)content[ACTION_INDEX]).equalsIgnoreCase("create branch")
								||  ((String) content[VERSION_INDEX]).endsWith("\\0") ) ) {
							historyEntries.add(content);
						}	
					}
					commentBuilder = new StringBuilder();
					content = new Object[6];
					Date date = dateFormatter.parse(matcher.group(1));
					content[DATE_INDEX] = date;
					content[USER_INDEX] = matcher.group(2);
					content[ACTION_INDEX] = matcher.group(3);
					content[VERSION_INDEX] = matcher.group(5);
					content[FILE_INDEX] = matcher.group(4);
				} else {
					if (commentBuilder.length() > 0) {
						commentBuilder.append("\n");
					}
					commentBuilder.append(line);	
				}				
			}
			line = reader.readLine();			
		}
		if (content != null) {
			content[COMMENT_INDEX] = commentBuilder.toString();
			if (! (		((String)content[ACTION_INDEX]).equalsIgnoreCase("create branch")
					||  ((String) content[VERSION_INDEX]).endsWith("\\0") ) ) {
				historyEntries.add(content);
			}	
		}

		Collections.sort(historyEntries, new Comparator<Object[]>() {
			public int compare(Object[] arg0, Object[] arg1) {
				return ((Date) arg1[ClearToolHistoryParser.DATE_INDEX]).compareTo(
						((Date) arg0[ClearToolHistoryParser.DATE_INDEX]));
			}
		});
	}
}
