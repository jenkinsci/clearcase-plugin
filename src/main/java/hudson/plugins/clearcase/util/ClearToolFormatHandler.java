package hudson.plugins.clearcase.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hlyh
 */
public class ClearToolFormatHandler {
    
    private String format;
    private String patternStr;
    private int groupCount;
    private Pattern pattern;
    
   public ClearToolFormatHandler(String... elements) {
        setPattern(elements);
    }    
   
   public void setPattern(String... elements) {
        StringBuilder formatBuilder = new StringBuilder();
        StringBuilder patternBuilder = new StringBuilder();
        for (String element : elements) {
            formatBuilder.append(OutputFormat.START_DELIMITER);
            formatBuilder.append(element);
            formatBuilder.append(OutputFormat.END_DELIMITER);
            patternBuilder.append(OutputFormat.REGEX_GROUP);
        }
        formatBuilder.append(OutputFormat.LINEEND);
        groupCount =elements.length;
        format = formatBuilder.toString();
        patternStr = patternBuilder.toString();
        pattern = Pattern.compile(patternStr);
   }

    public String getFormat() {
        return format;
    }

    public String getPattern() {
        return patternStr;
    }
   
   public Matcher checkLine(String line) {
       Matcher matcher = pattern.matcher(line);
       
       if (matcher.find() && matcher.groupCount() == groupCount) {
           return matcher;
       } 
       return null;       
   }       
}
