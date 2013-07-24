package hudson.plugins.clearcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

import hudson.util.IOUtils;

import org.junit.Test;

public class ConfigSpecTest {

    @Test
    public void testConfigSpecDifferentOS() {
        ConfigSpec cs1 = new ConfigSpec("element /MyVOB/... .../MyBranch/LATEST", true);
        ConfigSpec cs2 = new ConfigSpec("element \\MyVOB\\... ...\\MyBranch\\LATEST", true);
        assertEquals(cs1, cs2);
    }
    
    @Test
    public void testExtractLoadRulesUnix() throws IOException {
        String rawCs = IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, true);
        Set<String> loadRules = cs.getLoadRules();
        assertEquals(1, loadRules.size());
        assertEquals("/a/b", loadRules.iterator().next());
    }
    
    @Test
    public void testLineEndings() throws IOException {
        String rawCs1 = IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-LF.log"));
        String rawCs2 = IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs1 = new ConfigSpec(rawCs1, true);
        ConfigSpec cs2 = new ConfigSpec(rawCs2, true);
        assertEquals(cs1, cs2);
    }
    
    @Test
    public void testExtractLoadRulesWindows() throws IOException {
        String rawCs = IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, false);
        Set<String> loadRules = cs.getLoadRules();
        assertEquals(1, loadRules.size());
        assertEquals("\\a\\b", loadRules.iterator().next());
    }
    
    @Test
    public void testStripLoadRulesWindows() throws IOException {
        String rawCs = IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, false);
        cs = cs.stripLoadRules();
        assertTrue(cs.getLoadRules().isEmpty());
    }

}
