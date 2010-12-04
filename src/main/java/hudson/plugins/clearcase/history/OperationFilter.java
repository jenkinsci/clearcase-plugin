/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer,
 *                          Krzysztof Malinowski
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
package hudson.plugins.clearcase.history;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.ArrayUtils;

/**
 * @author Krzysztof Malinowski (raspy@dev.java.net)
 */
public abstract class OperationFilter implements Filter {

    private static final Pattern PATTERN_OBJECT_NAME
            = Pattern.compile("^[^\"]*\"(.*)\"[^\"]*$");

    protected ArrayList<Pattern> namePatterns;

    protected abstract String[] getApplicableOperations();
    protected abstract boolean getAllowOtherOperations();

    public OperationFilter(String... namePatterns) {
        if (namePatterns != null) {
            this.namePatterns = new ArrayList<Pattern>(namePatterns.length);
            for (String name : namePatterns) {
                this.namePatterns.add(Pattern.compile(name));
            }
        }
    }

    @Override
    public boolean accept(HistoryEntry entry) {

        if (!ArrayUtils.contains(
                getApplicableOperations(), entry.getOperation())) {
            // Operation not applicable.
            return getAllowOtherOperations();
        }

        if (namePatterns == null) {
            // No name filtering requested, accept operation.
            return true;
        }

        String objectName = getObjectName(entry);
        for (Pattern pattern : namePatterns) {
            if (pattern.matcher(objectName).matches()) {
                return true;
            }
        }

        return false;
    }

    protected String getObjectName(HistoryEntry entry) {
        Matcher matcher = PATTERN_OBJECT_NAME.matcher(entry.getEvent());
        return matcher.matches() ? matcher.group(1) : "";
    }

}
