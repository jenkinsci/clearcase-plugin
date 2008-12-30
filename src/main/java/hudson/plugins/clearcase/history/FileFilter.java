package hudson.plugins.clearcase.history;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class FileFilter extends FieldFilter {

    public FileFilter(Type type, String patternText) {
        super(type, patternText);
    }

    
    @Override
    public boolean accept(HistoryEntry entry) {
        return accept(entry.getElement());
    }

}
