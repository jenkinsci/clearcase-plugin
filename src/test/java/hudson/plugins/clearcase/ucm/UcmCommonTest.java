package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.AbstractWorkspaceTest;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mock;

public class UcmCommonTest extends AbstractWorkspaceTest {

    @Mock
    private ClearTool         cleartool;

    @Mock
    private ClearToolLauncher launcher;

    @Mock
    private TaskListener      listener;

    @Test
    public void testGenerateLoadRulesFromBaselinesOneBaseline() throws Exception {
        when(cleartool.describe("%[root_dir]p\\n", "component:comp1@\\pvob")).thenReturn(new StringReader("/vob/comp1"));
        when(cleartool.getLauncher()).thenReturn(launcher);
        when(launcher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 1);
        assertEquals("vob/comp1", loadRules[0]);
        verify(cleartool).describe("%[root_dir]p\\n", "component:comp1@\\pvob");
    }

    @Test
    public void testGenerateLoadRulesFromBaselinesMultiBaseline() throws Exception {
        when(cleartool.describe("%[root_dir]p\\n", "component:comp1@\\pvob component:comp2@\\otherpvob")).thenReturn(
                new StringReader("/vob/comp1\n/othervob/comp2"));
        when(cleartool.getLauncher()).thenReturn(launcher);
        when(launcher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);

        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        baselines.add(new Baseline("bl2@\\otherpvob", "comp2@\\otherpvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 2);
        assertEquals("vob/comp1", loadRules[0]);
        assertEquals("othervob/comp2", loadRules[1]);
        verify(cleartool).describe("%[root_dir]p\\n", "component:comp1@\\pvob component:comp2@\\otherpvob");
    }
}
