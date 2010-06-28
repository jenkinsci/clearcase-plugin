package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearTool;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;

public class UcmCommonTest {

    private Mockery context;

    private ClearTool cleartool;

    @Before
    public void setUp() {
        context = new JUnit4Mockery();
        cleartool = context.mock(ClearTool.class);
    }

    @Test
    public void testGenerateLoadRulesFromBaselinesOneBaseline() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).describe("%[root_dir]p", "component:comp1@\\pvob"); will(returnValue(new StringReader("/vob/comp1")));
            }
        });
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 1);
        assertEquals("/vob/comp1", loadRules[0]);
    }
    
    @Test
    public void testGenerateLoadRulesFromBaselinesMultiBaseline() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).describe("%[root_dir]p", "component:comp1@\\pvob"); will(returnValue(new StringReader("/vob/comp1")));
                one(cleartool).describe("%[root_dir]p", "component:comp2@\\otherpvob"); will(returnValue(new StringReader("/othervob/comp2")));
            }
        });
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        baselines.add(new Baseline("bl2@\\otherpvob", "comp2@\\otherpvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 2);
        assertEquals("/vob/comp1", loadRules[0]);
        assertEquals("/othervob/comp2", loadRules[1]);
    }
}
