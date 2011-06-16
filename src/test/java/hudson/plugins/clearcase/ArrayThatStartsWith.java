package hudson.plugins.clearcase;

import org.apache.commons.lang.ObjectUtils;
import org.mockito.ArgumentMatcher;

public class ArrayThatStartsWith<T> extends ArgumentMatcher<T[]> {
    private T[] startsWith;
    
    public ArrayThatStartsWith(T[] startsWith) {
        this.startsWith = startsWith;
    }

    @Override
    public boolean matches(Object item) {
        if (item instanceof Object[]) {
            Object[] arr = (Object[]) item;
            if (arr.length < startsWith.length) {
                return false;
            }
            for (int i = 0; i < startsWith.length; i++) {
                if (!ObjectUtils.equals(startsWith[i], arr[i])) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

}
