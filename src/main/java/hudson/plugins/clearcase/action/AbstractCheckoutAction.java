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
package hudson.plugins.clearcase.action;


import hudson.FilePath;
import hudson.Launcher;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractCheckoutAction implements CheckOutAction {
    public AbstractCheckoutAction() {
        
    }
    
    public abstract boolean checkout(Launcher launcher, FilePath workspace, String viewName) throws IOException, InterruptedException;

    protected Set<String> extractLoadRules(String configSpec) {
        Set<String> rules = new HashSet<String>();
        for (String row : configSpec.split("[\\r\\n]+")) {
            String trimmedRow = row.toLowerCase().trim();
            if (trimmedRow.startsWith("load")) {
                String rule = row.trim().substring("load".length()).trim();
                rules.add(rule);
                if ((!rule.startsWith("/")) && (!rule.startsWith("\\"))) {
                    rules.add(rule);
                } else {
                    rules.add(rule.substring(1));
                }
            }
        }
        return rules;
    }
    
    
    protected String getLoadRuleFreeConfigSpec(String configSpec) {
        String lrFreeCS = "";

        for (String row : configSpec.split("[\\r\\n]+")) {
            if (!row.startsWith("load")) {
                lrFreeCS += row + "\n";
            }
        }

        return lrFreeCS.trim();
    }

}