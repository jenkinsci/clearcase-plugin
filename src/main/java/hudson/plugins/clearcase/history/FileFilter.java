package hudson.plugins.clearcase.history;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class FileFilter implements Filter {

    private boolean include;
    private String patternText;
    private Pattern pattern;

    public FileFilter(boolean include,String patternText) {
        this.include = include;
        this.patternText = patternText;
        this.pattern = Pattern.compile(patternText);
    }


    @Override
    public boolean accept(HistoryEntry entry) {
        Matcher m = pattern.matcher(entry.getElement());
        return include == m.find();
    }

}
