/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase;

import org.apache.commons.lang.ObjectUtils;
import org.hamcrest.Description;
import org.mockito.ArgumentMatcher;

public class ArrayThatStartsWith<T> extends ArgumentMatcher<T[]> {

    private T[] startsWith;

    public ArrayThatStartsWith(T[] startsWith) {
        this.startsWith = startsWith;
    }

    @Override
    public void describeTo(Description description) {
        description.appendValueList("[", ", ", ", ", startsWith);
        description.appendText("...]");
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
        }
        return false;
    }

}
