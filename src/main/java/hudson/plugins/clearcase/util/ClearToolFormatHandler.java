/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hlyh
 */
public class ClearToolFormatHandler {
    
    private String format;
    private String patternStr;
    private int groupCount;
    private Pattern pattern;
    
   public ClearToolFormatHandler(String... elements) {
        setPattern(elements);
    }    
   
   public void setPattern(String... elements) {
        StringBuilder formatBuilder = new StringBuilder();
        StringBuilder patternBuilder = new StringBuilder();
        for (String element : elements) {
            formatBuilder.append(OutputFormat.START_DELIMITER);
            formatBuilder.append(element);
            formatBuilder.append(OutputFormat.END_DELIMITER);
            patternBuilder.append(OutputFormat.REGEX_GROUP);
        }
        formatBuilder.append(OutputFormat.LINEEND);
        groupCount =elements.length;
        format = formatBuilder.toString();
        patternStr = patternBuilder.toString();
        pattern = Pattern.compile(patternStr);
   }

    public String getFormat() {
        return format;
    }

    public String getPattern() {
        return patternStr;
    }
   
   public Matcher checkLine(String line) {
       Matcher matcher = pattern.matcher(line);
       
       if (matcher.find() && matcher.groupCount() == groupCount) {
           return matcher;
       } 
       return null;       
   }       
}
