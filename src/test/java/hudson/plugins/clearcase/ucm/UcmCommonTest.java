package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;

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
    
    private ClearToolLauncher launcher;
    
    private TaskListener listener;

    @Before
    public void setUp() {
        context = new JUnit4Mockery();
        cleartool = context.mock(ClearTool.class);
        launcher = context.mock(ClearToolLauncher.class);
        listener = context.mock(TaskListener.class);
    }

    @Test
    public void testGenerateLoadRulesFromBaselinesOneBaseline() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).describe("%[root_dir]p\\n", null, "component:comp1@\\pvob"); will(returnValue(new StringReader("/vob/comp1")));
                allowing(cleartool).getLauncher(); will(returnValue(launcher));
                allowing(launcher).getListener(); will(returnValue(listener));
                allowing(listener).getLogger(); will(returnValue(System.out));
            }
        });
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 1);
        assertEquals("vob/comp1", loadRules[0]);
    }
    
    @Test
    public void testGenerateLoadRulesFromBaselinesMultiBaseline() throws Exception {
        context.checking(new Expectations() {
            {
                one(cleartool).describe("%[root_dir]p\\n", null, "component:comp1@\\pvob component:comp2@\\otherpvob"); will(returnValue(new StringReader("/vob/comp1\n/othervob/comp2")));
                allowing(cleartool).getLauncher(); will(returnValue(launcher));
                allowing(launcher).getListener(); will(returnValue(listener));
                allowing(listener).getLogger(); will(returnValue(System.out));
            }
        });
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        baselines.add(new Baseline("bl2@\\otherpvob", "comp2@\\otherpvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 2);
        assertEquals("vob/comp1", loadRules[0]);
        assertEquals("othervob/comp2", loadRules[1]);
    }
}
