package hudson.plugins.clearcase.history;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public interface Filter {

    public boolean accept(HistoryEntry element);
    

}
