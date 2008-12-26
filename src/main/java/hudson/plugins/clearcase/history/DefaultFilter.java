package hudson.plugins.clearcase.history;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class DefaultFilter implements Filter {

    @Override
    public boolean accept(HistoryEntry entry) {
        if (entry.getVersionId().endsWith("/0"))
            return false;

        if (entry.getVersionId().endsWith("\\0"))
            return false;
       if  (entry.getEvent().equalsIgnoreCase("create branch"))
           return false;

        return true;
    }

}
