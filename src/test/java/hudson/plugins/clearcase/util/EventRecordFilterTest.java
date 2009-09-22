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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;


public class EventRecordFilterTest {

    private EventRecordFilter filter;

    @Before
    public void setUp() {
        filter = new EventRecordFilter();
    }
    
    @Test
    public void assertZeroVersionIsNotAccepted() {
        assertFalse("Version with 0 should be ignored", filter.accept("event", "\\main\\sit_r6a\\0"));
    }
    
    @Test
    public void assertZeroVersionOnUnixIsNotAccepted() {
        assertFalse("Version with 0 should be ignored", filter.accept("event", "/main/sit_r6a/0"));
    }
    
    @Test
    public void assertNonZeroVersionIsAccepted() {
        assertTrue("Version with 11 should not be ignored", filter.accept("event", "\\main\\sit_r6a\\11"));
    }
    
    @Test
    public void assertNonZeroVersionOnUnixIsAccepted() {
        assertTrue("Version with 11 should not be ignored", filter.accept("event", "/main/sit_r6a/11"));
    }
    
    @Test
    public void assertCreateBranchEventIsNotAccepted() {
        assertFalse("'create branch' event be ignored", filter.accept("create branch", "/main/sit_r6a/11"));
    }
    
    @Test
    public void assertDestroySubBranchEventIsNotAccepted() {
        filter.setFilterOutDestroySubBranchEvent(true);
        assertFalse("'destroy sub-branch' was configured to be ignored", filter.accept("destroy sub-branch \"esmalling_branch\" of branch", "/main/sit_r6a/11"));
    }
    
    @Test
    public void assertDestroySubBranchEventIsAccepted() {
        filter.setFilterOutDestroySubBranchEvent(false);
        assertTrue("'destroy sub-branch' was configured to be ignored", filter.accept("destroy sub-branch \"esmalling_branch\" of branch", "/main/sit_r6a/11"));
    }
    
    @Test
    public void assertDestroySubBranchEventIsAcceptedByDefault() {
        assertTrue("'destroy sub-branch' was configured to be ignored", filter.accept("destroy sub-branch \"esmalling_branch\" of branch", "/main/sit_r6a/11"));
    }
}
