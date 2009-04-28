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

import java.util.regex.Pattern;

/**
 * Class that helps deteremine if an event record should be used in a change log.
 * The change log could be created in a checkout or polling action.
 */
public class EventRecordFilter {

    private static final Pattern DESTROYED_SUB_BRANCH_PATTERN = Pattern.compile("destroy sub-branch \".+\" of branch");

    private boolean filterOutDestroySubBranchEvent = false;

    /**
     * Tests if a specified event record should be included in a change log.
     * @param event string containing the event
     * @param version string containing the version info
     * @return true if the event record should be included in a change log.
     */
    public boolean accept(String event, String version) {
        return !  (version.endsWith("/0") 
                || version.endsWith("\\0") 
                || event.equalsIgnoreCase("create branch")
                || (filterOutDestroySubBranchEvent && DESTROYED_SUB_BRANCH_PATTERN.matcher(event).matches()));
    }
    
    /**
     * Set whetever the poll action should filter out "Destroy sub-branch [BRANCH] of branch" events. 
     * @param filterOutEvent true, then it should ignore the event; false (default) should not ignore it.
     */
    public void setFilterOutDestroySubBranchEvent(boolean filterOutEvent) {
        this.filterOutDestroySubBranchEvent  = filterOutEvent;
    }
    
    public boolean isFilteringOutDestroySubBranchEvent() {
        return filterOutDestroySubBranchEvent;
    }
}
