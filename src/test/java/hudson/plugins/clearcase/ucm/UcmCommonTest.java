/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    public void testGenerateLoadRulesFromBaselinesMultiBaseline() throws Exception {
        when(cleartool.describe(eq("%[root_dir]p\\n"), eq(new String[] { "component:comp1@\\pvob", "component:comp2@\\otherpvob" }))).thenReturn(
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
        verify(cleartool).describe(eq("%[root_dir]p\\n"), eq(new String[] { "component:comp1@\\pvob", "component:comp2@\\otherpvob" }));
    }

    @Test
    public void testGenerateLoadRulesFromBaselinesOneBaseline() throws Exception {
        when(cleartool.describe(eq("%[root_dir]p\\n"), eq(new String[] { "component:comp1@\\pvob" }))).thenReturn(new StringReader("/vob/comp1"));
        when(cleartool.getLauncher()).thenReturn(launcher);
        when(launcher.getListener()).thenReturn(listener);
        when(listener.getLogger()).thenReturn(System.out);
        assertTrue(UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", null) == null);
        List<Baseline> baselines = new ArrayList<Baseline>();
        baselines.add(new Baseline("bl1@\\pvob", "comp1@\\pvob"));
        String[] loadRules = UcmCommon.generateLoadRulesFromBaselines(cleartool, "mystream", baselines);
        assertTrue(loadRules.length == 1);
        assertEquals("vob/comp1", loadRules[0]);
        verify(cleartool).describe(eq("%[root_dir]p\\n"), eq(new String[] { "component:comp1@\\pvob" }));
    }
}
