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
package hudson.plugins.clearcase.ucm.model;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public abstract class UcmSelector {
    private String name;
    private String pvob;

    protected UcmSelector() {
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        UcmSelector other = (UcmSelector) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (pvob == null) {
            if (other.pvob != null)
                return false;
        } else if (!pvob.equals(other.pvob))
            return false;
        return true;
    }

    public String getName() {
        return name;
    }

    public String getPvob() {
        return pvob;
    }

    public final String getSelector() {
        return getPrefix() + name + '@' + pvob;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((pvob == null) ? 0 : pvob.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + name + "@" + pvob + "]";
    }

    protected abstract String getPrefix();

    protected void init(String selector) {
        Validate.notNull(selector);
        String simpleSelector = StringUtils.removeStart(selector, getPrefix());
        if (simpleSelector.contains(":"))
            throw new IllegalArgumentException("Selector " + simpleSelector + " doesn't match the current class " + getClass().getName());
        String[] split = simpleSelector.split("@");
        name = split[0];
        if (split.length > 1) {
            pvob = split[1];
        }
    }

    public static <T extends UcmSelector> T parse(String selector, Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            instance.init(selector);
            return instance;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
