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
        this.patternText = patternText;
        if (this.type.equals(Type.ContainsRegxp) || this.type.equals(Type.DoesNotContainRegxp)) {
            pattern = Pattern.compile(patternText);
        }
        }
    
    public boolean accept(String value) {

        switch (type) {
            case Equals:
                return value.equals(patternText);
            case NotEquals:
                return !(value.equals(patternText));
            case Contains:
                return value.contains(patternText);
            case DoesNotContain:
                return !(value.contains(patternText));
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
        Contains,
        ContainsIgnoreCase,
        DoesNotContain,
        DoesNotContainIgnoreCase,
        ContainsRegxp,
        DoesNotContainRegxp
    }

}
