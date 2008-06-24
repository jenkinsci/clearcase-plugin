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
