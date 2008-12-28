package hudson.plugins.clearcase.history;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class FileFilter implements Filter {

    private String patternText;
    private Pattern pattern;
    private FileFilter.Type type;

    public FileFilter(FileFilter.Type type,String patternText) {
        this.type = type;
        this.patternText = patternText;
        if (this.type.equals(Type.ContainsRegxp) || this.type.equals(Type.DoesNotContainRegxp)) {
            pattern = Pattern.compile(patternText);
        }
    }


    @Override
    public boolean accept(HistoryEntry entry) {
        
        switch (type) {
            case Equals:
                return entry.getElement().equals(patternText);
            case NotEquals:
                return !(entry.getElement().equals(patternText));
            case Contains:
                return entry.getElement().contains(patternText);
            case DoesNotContain:
                return !(entry.getElement().contains(patternText));
            case ContainsRegxp:
                Matcher m = pattern.matcher(entry.getElement());
                return m.find();
            case DoesNotContainRegxp:
                Matcher m2 = pattern.matcher(entry.getElement());
                return !m2.find();
        }
        return true;
    }

    public enum Type {
        Equals,
        NotEquals,
        Contains,
        DoesNotContain,
        ContainsRegxp,
        DoesNotContainRegxp
    }

}
