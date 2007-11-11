package hudson.plugins.clearcase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

	public List<ClearCaseChangeLogEntry> parse(Reader inReader) throws IOException, ParseException {
		
		List<ClearCaseChangeLogEntry> entries = new ArrayList<ClearCaseChangeLogEntry>();
		BufferedReader reader = new BufferedReader(inReader);

		ClearCaseChangeLogEntry content = null;
		StringBuilder commentBuilder = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
			
			if (!line.startsWith("cleartool: Error:")) {
				Matcher matcher = pattern.matcher(line);		
				if (matcher.find() && matcher.groupCount() == 5) {					
					if (content != null) {
						content.setComment(commentBuilder.toString());
						if (! (		(content.getAction()).equalsIgnoreCase("create branch")
								||  (content.getVersion()).endsWith("\\0") ) ) {
							entries.add(content);
						}	
					}
					commentBuilder = new StringBuilder();
					content = new ClearCaseChangeLogEntry();
					Date date = dateFormatter.parse(matcher.group(1));
					content.setDate(date);
					content.setUser(matcher.group(2));
					content.setAction(matcher.group(3));
					content.setVersion(matcher.group(5));
					content.setFile(matcher.group(4));
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
			content.setComment(commentBuilder.toString());
			if (! (		(content.getAction().equalsIgnoreCase("create branch"))
					||  (content.getVersion().endsWith("\\0")) ) ) {
				entries.add(content);
			}	
		}

		Collections.sort(entries, new Comparator<ClearCaseChangeLogEntry>() {
			public int compare(ClearCaseChangeLogEntry arg0, ClearCaseChangeLogEntry arg1) {
				return (arg1.getDateStr().compareTo(arg0.getDateStr()));
			}
		});
		return entries;
	}
}
