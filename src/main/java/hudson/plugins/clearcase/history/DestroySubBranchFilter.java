package hudson.plugins.clearcase.history;

import java.util.regex.Pattern;

/**
 *
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class DestroySubBranchFilter implements Filter {

    private static final Pattern DESTROYED_SUB_BRANCH_PATTERN = Pattern.compile("destroy sub-branch \".+\" of branch");
    
    @Override
    public boolean accept(HistoryEntry entry) {
        if (DESTROYED_SUB_BRANCH_PATTERN.matcher(entry.getEvent()).matches())
            return false;
        
        return true;
    }
}
