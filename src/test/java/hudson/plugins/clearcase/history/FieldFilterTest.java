

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
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Equals,PATTERN);
        assertTrue(filter.accept(PATTERN));
        assertTrue(filter.accept(PATTERN_UC));
    }

    @Test
    public void testEqualsIgnoreCaseFiltered() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.Equals,PATTERN);
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
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEquals,PATTERN);
        assertFalse(filter.accept(PATTERN));
        assertFalse(filter.accept(PATTERN_UC));

    }

    @Test
    public void testNotEqualsIgnoreCaseAccepted() {
        FieldFilter filter = new FieldFilterDummy(FieldFilter.Type.NotEquals,PATTERN);
        assertTrue(filter.accept(PATTERN+"_Not"));
        assertTrue(filter.accept(PATTERN.substring(2)));
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
