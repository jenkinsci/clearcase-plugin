/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import hudson.plugins.clearcase.util.PathUtil;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public class ConfigSpec {
    private final String raw;
    private final boolean isUnix;
    
    public ConfigSpec(String raw, boolean isUnix) {
        Validate.notNull(raw);
        this.raw = raw;
        this.isUnix = isUnix;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ConfigSpec other = (ConfigSpec) obj;
        if (isUnix != other.isUnix)
            return false;
        if (raw == null) {
            if (other.raw != null)
                return false;
        } else if (!raw.equals(other.raw))
            return false;
        return true;
    }
    
    public Set<String> getLoadRules() {
        Set<String> rules = new HashSet<String>();
        for (String row : raw.split("[\\r\\n]+")) {
            String trimmedRow = row.trim();
            if (trimmedRow.startsWith("load")) {
                String rule = row.trim().substring("load".length()).trim();
                rules.add(rule);
            }
        }
        return rules;
    }

    public String getRaw() {
        return raw;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (isUnix ? 1231 : 1237);
        result = prime * result + ((raw == null) ? 0 : raw.hashCode());
        return result;
    }
    
    public ConfigSpec setLoadRules(String[] loadRules) {
        StringBuilder sb = stripLoadRulesOnRaw();
        if (!ArrayUtils.isEmpty(loadRules)) {
            for (String loadRule : loadRules) {
                // Make sure the load rule starts with \ or /, as appropriate
                sb.append("load ");
                sb.append(cleanLoadRule(loadRule, isUnix).trim()).append(PathUtil.newLineForOS(isUnix));
            }
        }
        return new ConfigSpec(sb.toString(), isUnix);
    }

    public static String cleanLoadRule(String loadRule, boolean isUnix) {
        if (StringUtils.isBlank(loadRule)) {
            return loadRule;
        }
        String lr = loadRule;
        // Remove quotes if needed
        if (lr.charAt(0) == '"' && lr.charAt(lr.length()-1) == '"') {
            lr = lr.substring(1, lr.length()-1);
        }
        // Prepend OS separator
        char firstChar = lr.charAt(0);
        if (!(firstChar == '\\') && !(firstChar == '/')) {
            lr = PathUtil.fileSepForOS(isUnix) + lr;
        }
        // Add quotes if path contains spaces
        if (lr.contains(" ")) {
            lr = '"' + lr + '"';
        }
        return PathUtil.convertPathForOS(lr, isUnix);
    }

    public ConfigSpec stripLoadRules() {
        return new ConfigSpec(stripLoadRulesOnRaw().toString().trim(), isUnix);
    }

    private StringBuilder stripLoadRulesOnRaw() {
        StringBuilder sb = new StringBuilder();
        for (String row : raw.split("[\\r\\n]+")) {
            if (!row.startsWith("load")) {
                sb.append(row).append(PathUtil.newLineForOS(isUnix));
            }
        }
        return sb;
    }
}
