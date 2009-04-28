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


package hudson.plugins.clearcase.history;

import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author hlyh
 */
public class FieldFilterTest {

    private static final String PATTERN="filtertest";
    private static final String PATTERN_UC="FilterTest";
    private static final String PATTERN_REGXP="F[a-z]*";

    @Test
    public void testEqualsAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Equals,PATTERN);
        assertTrue(filter.accept(PATTERN));        
    }

    @Test
    public void testEqualsFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Equals,PATTERN);
        assertFalse(filter.accept(PATTERN+"_Not"));
        assertFalse(filter.accept(PATTERN_UC));
        assertFalse(filter.accept(PATTERN.substring(2)));
    }

    @Test
    public void testEqualsIgnoreCaseAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.EqualsIgnoreCase,PATTERN);
        assertTrue("Equals ",filter.accept(PATTERN));
        assertTrue("Equals UpperCase",filter.accept(PATTERN_UC));
    }

    @Test
    public void testEqualsIgnoreCaseFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.EqualsIgnoreCase,PATTERN);
        assertFalse(filter.accept(PATTERN+"_Not"));
        assertFalse(filter.accept(PATTERN.substring(2)));
    }

    @Test
    public void testNotEqualsFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEquals,PATTERN);
        assertFalse(filter.accept(PATTERN));
    }

    @Test
    public void testNotEqualsAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEquals,PATTERN);
        assertTrue(filter.accept(PATTERN+"_Not"));
        assertTrue(filter.accept(PATTERN_UC));
        assertTrue(filter.accept(PATTERN.substring(2)));
    }

    @Test
    public void testNotEqualsIgnoreCaseFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEqualsIgnoreCase,PATTERN);
        assertFalse(filter.accept(PATTERN));
        assertFalse(filter.accept(PATTERN_UC));

    }

    @Test
    public void testNotEqualsIgnoreCaseAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEqualsIgnoreCase,PATTERN);
        assertTrue(filter.accept(PATTERN+"_Not"));
        assertTrue(filter.accept(PATTERN.substring(2)));
    }

    @Test
    public void testContainssAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Contains,PATTERN);
        assertTrue("Equals",filter.accept(PATTERN));
        assertTrue("PostFix",filter.accept(PATTERN+"_Post"));
        assertTrue("Prefix",filter.accept("Pre_"+PATTERN));
    }

    @Test
    public void testContainsFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Contains,PATTERN);
        assertFalse(filter.accept(PATTERN_UC));
        assertFalse(filter.accept(PATTERN.substring(2)));
    }


    @Test
    public void testContainsIgnoreCaseFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Contains,PATTERN);
        assertFalse(filter.accept(PATTERN_UC));
        assertFalse(filter.accept(PATTERN.substring(2)));
    }

    @Test
    public void testNotContainsIgnoreCaseAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContain,PATTERN);
        assertTrue(filter.accept(PATTERN_UC));
        assertTrue(filter.accept(PATTERN.substring(2)));

    }

    @Test
    public void testNotContainssAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContain,PATTERN);
        assertTrue(filter.accept(PATTERN_UC));
        assertTrue(filter.accept(PATTERN.substring(2)));

    }
    @Test
    public void testNotContainsFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContain,PATTERN);
        assertFalse("Equals",filter.accept(PATTERN));
        assertFalse("PostFix",filter.accept(PATTERN+"_Post"));
        assertFalse("Prefix",filter.accept("Pre_"+PATTERN));

    }

    @Test
    public void testNotContainssIgnoreCaseAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContainIgnoreCase,PATTERN);
        assertFalse(filter.accept(PATTERN_UC));
        

    }
    
    @Test
    public void testNotContainsIgnoreCaseFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContainIgnoreCase,PATTERN);
        assertTrue("smaller",filter.accept(PATTERN.substring(2)));
        assertFalse("Equals",filter.accept(PATTERN));
        assertFalse("UC",filter.accept(PATTERN_UC));
        assertFalse("PostFix",filter.accept(PATTERN+"_Post"));
        assertFalse("Prefix",filter.accept("Pre_"+PATTERN));

    }

    @Test
    public void testRegxpAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.ContainsRegxp,PATTERN_REGXP);
        assertTrue(filter.accept(PATTERN_UC));
        assertTrue(filter.accept("Fhello"));
    }

    @Test
    public void testRegxpfiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.ContainsRegxp,PATTERN_REGXP);
        assertFalse("Pattern", filter.accept(PATTERN));
        assertFalse("fhello",filter.accept("fhello"));
        assertFalse("1hello",filter.accept("1hello"));
    }


    @Test
    public void testNotRegxpAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContainRegxp,PATTERN_REGXP);
        assertTrue("Pattern", filter.accept(PATTERN));
        assertTrue("fhello",filter.accept("fhello"));
        assertTrue("1hello",filter.accept("1hello"));


    }

    @Test
    public void testNotRegxpfiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContainRegxp,PATTERN_REGXP);
        assertFalse(filter.accept(PATTERN_UC));
        assertFalse(filter.accept("Fhello"));
    }


    private static class FieldFilterDummy extends FieldFilter {

        public FieldFilterDummy(Type type, String patternText) {
            super(type, patternText);
        }

        @Override
        public boolean accept(HistoryEntry element) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }
}
