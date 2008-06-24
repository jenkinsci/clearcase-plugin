package hudson.plugins.clearcase;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;
import hudson.plugins.clearcase.util.EventRecordFilter;

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
 * Parser for the cleartool history output.
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
    private EventRecordFilter filter;
    
    public ClearToolHistoryParser() {
        this(new EventRecordFilter());
    }
    
    public ClearToolHistoryParser(EventRecordFilter filter) {
        this.filter = filter;
        // pattern = Pattern.compile("^(\\S+)\\s+(\\w+)\\s+(.+)\\s+\"(.+)@@(.+)\"");
        pattern = Pattern.compile("\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"\\s+\"(.+)\"");
        dateFormatter = new SimpleDateFormat("yyyyMMdd.HHmmss");
    }

    /**
     * Returns the log format that the parser supports
     * 
     * @return the format for the 'cleartool lshistory' command
     */
    public static String getLogFormat() {
        return "\\\"%Nd\\\" \\\"%u\\\" \\\"%e\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%o\\\"\\n%c\\n";
    }

    public List<ClearCaseChangeLogEntry> parse(Reader inReader) throws IOException, ParseException {

        List<ClearCaseChangeLogEntry> entries = new ArrayList<ClearCaseChangeLogEntry>();
        BufferedReader reader = new BufferedReader(inReader);

        ClearCaseChangeLogEntry newLogEntry = null;
        StringBuilder commentBuilder = new StringBuilder();
        String line = reader.readLine();
        while (line != null) {

            if (!line.startsWith("cleartool: Error:")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 6) {
                    if (newLogEntry != null) {
                        newLogEntry.setComment(commentBuilder.toString().trim());
                        if (isFileElementModification(newLogEntry.getElements().get(0))) {
                            entries.add(newLogEntry);
                        }
                    }
                    commentBuilder = new StringBuilder();
                    newLogEntry = new ClearCaseChangeLogEntry();
                    Date date = dateFormatter.parse(matcher.group(1));
                    newLogEntry.setDate(date);
                    newLogEntry.setUser(matcher.group(2));
                    ClearCaseChangeLogEntry.FileElement element = new ClearCaseChangeLogEntry.FileElement(
                            matcher.group(4).trim(), matcher.group(5).trim(), matcher.group(3).trim(), matcher.group(6).trim());
                    newLogEntry.addElement(element);
                } else {
                    if (commentBuilder.length() > 0) {
                        commentBuilder.append("\n");
                    }
                    commentBuilder.append(line);
                }
            }
            line = reader.readLine();
        }
        if (newLogEntry != null) {
            newLogEntry.setComment(commentBuilder.toString().trim());
            if (isFileElementModification(newLogEntry.getElements().get(0))) {
                entries.add(newLogEntry);
            }
        }
        
        Collections.sort(entries, new Comparator<ClearCaseChangeLogEntry>() {
            public int compare(ClearCaseChangeLogEntry arg0, ClearCaseChangeLogEntry arg1) {
                return (arg1.getDateStr().compareTo(arg0.getDateStr()));
            }
        });
        
        return entries;
    }
    
    /**
     * Returns true if the file element is a real modification that could trigger a build.
     * Mainly it will check the action string if it is "create branch" and if the version
     * for an element is 0.
     * @param element the file element to check
     * @return true, if it is a modification that should trigger a build; false otherwise.
     */
    private boolean isFileElementModification(FileElement element) {
        return filter.accept(element.getAction(), element.getVersion()); 
    }
}
