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
                this.patternText = patternText;
                this.pattern =null;
                break;

            case EqualsIgnoreCase:
            case NotEqualsIgnoreCase:
            case ContainsIgnoreCase:
            case DoesNotContainIgnoreCase:
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
        Contains,
        ContainsIgnoreCase,
        DoesNotContain,
        DoesNotContainIgnoreCase,
        ContainsRegxp,
        DoesNotContainRegxp
    }

}
