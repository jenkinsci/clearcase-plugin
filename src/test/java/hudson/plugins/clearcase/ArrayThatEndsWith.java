package hudson.plugins.clearcase;

import org.apache.commons.lang.ObjectUtils;
import org.mockito.ArgumentMatcher;

public class ArrayThatEndsWith<T> extends ArgumentMatcher<T[]> {
    private T[] endsWith;
    
    public ArrayThatEndsWith(T[] endsWith) {
        this.endsWith = endsWith;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Object[]) {
            Object[] arr = (Object[]) item;
            if (arr.length < endsWith.length) {
                return false;
            }
            int startIndex = arr.length - endsWith.length;
            for (int i = 0; i < endsWith.length; i++) {
                if (!ObjectUtils.equals(endsWith[i], arr[startIndex + i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

}
