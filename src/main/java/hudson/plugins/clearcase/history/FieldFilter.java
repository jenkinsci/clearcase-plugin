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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hudson.plugins.clearcase.history;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hlyh
 */
public abstract class FieldFilter implements Filter{
    private Type type;
    private String patternText;
    private Pattern pattern;


    public FieldFilter(FieldFilter.Type type,String patternText) {
        this.type = type;
        switch (this.type) {
        case Equals:
        case NotEquals:
        case Contains:
        case DoesNotContain:
        case StartsWith:
        case EndsWith:
            this.patternText = patternText;
            this.pattern =null;
            break;
            
        case EqualsIgnoreCase:
        case NotEqualsIgnoreCase:
        case ContainsIgnoreCase:
        case DoesNotContainIgnoreCase:
        case StartsWithIgnoreCase:
        case EndsWithIgnoreCase:
            this.patternText = patternText.toLowerCase();
            this.pattern =null;
            break;

        case ContainsRegxp:
        case DoesNotContainRegxp:
            this.patternText = patternText;
            this.pattern = Pattern.compile(patternText);
            break;
        }
    }
    
    public boolean accept(String value) {
        
        switch (type) {
        case Equals:
            return value.equals(patternText);
        case EqualsIgnoreCase:
            return value.toLowerCase().equals(patternText);
        case NotEquals:
            return !(value.equals(patternText));
        case NotEqualsIgnoreCase:
            return !(value.toLowerCase().equals(patternText));
        case StartsWith:
            return value.startsWith(patternText);
        case StartsWithIgnoreCase:
            return value.toLowerCase().startsWith(patternText);
        case EndsWith:
            return value.endsWith(patternText);
        case EndsWithIgnoreCase:
            return value.toLowerCase().endsWith(patternText);
        case Contains:
            return value.contains(patternText);
        case ContainsIgnoreCase:
            return value.toLowerCase().contains(patternText);
        case DoesNotContain:
            return !(value.contains(patternText));
        case DoesNotContainIgnoreCase:
            System.out.println(value.toLowerCase()+" <>" +patternText);
            return !(value.toLowerCase().contains(patternText));
        case ContainsRegxp:
            Matcher m = pattern.matcher(value);
            return m.find();
        case DoesNotContainRegxp:
            Matcher m2 = pattern.matcher(value);
            return !m2.find();
        }
        return true;
    }


    public enum Type {
        Equals,
        EqualsIgnoreCase,
        NotEquals,
        NotEqualsIgnoreCase,
        StartsWith,
        StartsWithIgnoreCase,
        EndsWith,
        EndsWithIgnoreCase,
        Contains,
        ContainsIgnoreCase,
        DoesNotContain,
        DoesNotContainIgnoreCase,
        ContainsRegxp,
        DoesNotContainRegxp
    }

}
