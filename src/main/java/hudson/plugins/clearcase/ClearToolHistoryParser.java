package hudson.plugins.clearcase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
	private SimpleDateFormat dateFormatter;

	public ClearToolHistoryParser() {
		pattern = Pattern.compile("^(\\S+)\\s+(\\w+)\\s+(.+)\\s+\"(.+)@@(.+)\"");
		dateFormatter = new SimpleDateFormat("dd-MMM.HH:mm");
	}

	public void parse(Reader inReader, List<Object[]> historyEntries) throws IOException {

		BufferedReader reader = new BufferedReader(inReader);

		String line = reader.readLine();
		while (line != null) {

			Object[] content = new Object[6];
			Matcher matcher = pattern.matcher(line);

			if (matcher.find() && matcher.groupCount() == 5) {
				try {
					content[DATE_INDEX] = dateFormatter.parse(matcher.group(1));
				} catch (ParseException e) {
				}
				content[USER_INDEX] = matcher.group(2);
				content[ACTION_INDEX] = matcher.group(3);
				content[VERSION_INDEX] = matcher.group(5);
				content[FILE_INDEX] = matcher.group(4);

				line = reader.readLine();

				StringBuilder commentBuilder = new StringBuilder();
				while ((line != null) && ((line.length() == 0) || (line.charAt(0) == ' '))) {
					if (commentBuilder.length() > 0) {
						commentBuilder.append("\n");
					}
					commentBuilder.append(line.trim());
					line = reader.readLine();
				}

				if (commentBuilder.length() > 0) {
					commentBuilder.deleteCharAt(0);
					commentBuilder.deleteCharAt(commentBuilder.length() - 1);
				}

				content[COMMENT_INDEX] = commentBuilder.toString();
				historyEntries.add(content);
			} else {
				line = reader.readLine();
			}
		}
	}
}
