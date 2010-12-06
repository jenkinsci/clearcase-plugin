package hudson.plugins.clearcase.util;

import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_CONTRIBUTING;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_HEADLINE;
import static hudson.plugins.clearcase.util.OutputFormat.UCM_ACTIVITY_STREAM;
import static hudson.plugins.clearcase.util.OutputFormat.USER_ID;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ClearToolFormatHandlerTest {
    
    private ClearToolFormatHandler instance;
    
    private static final String[] INTEGRATION_ACTIVITY_FORMAT = {
        UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID,
        UCM_ACTIVITY_CONTRIBUTING };

    @Before
    public void setUp() throws Exception {
        instance = new ClearToolFormatHandler(INTEGRATION_ACTIVITY_FORMAT);
    }

    @Test
    public void testCheckLine() {
        assertNull(instance.checkLine(null));
    }

}
