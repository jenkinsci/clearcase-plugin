

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
    public void testNotContainssAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContain,PATTERN);
        assertTrue(filter.accept(PATTERN_UC));
        assertTrue(filter.accept(PATTERN.substring(2)));

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
    public void testNotContainsFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.DoesNotContain,PATTERN);
        assertFalse("Equals",filter.accept(PATTERN));
        assertFalse("PostFix",filter.accept(PATTERN+"_Post"));
        assertFalse("Prefix",filter.accept("Pre_"+PATTERN));

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
