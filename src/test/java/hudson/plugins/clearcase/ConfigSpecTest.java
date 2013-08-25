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
package hudson.plugins.clearcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Set;

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
        String rawCs = org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, true);
        Set<String> loadRules = cs.getLoadRules();
        assertEquals(1, loadRules.size());
        assertEquals("/a/b", loadRules.iterator().next());
    }

    @Test
    public void testExtractLoadRulesWindows() throws IOException {
        String rawCs = org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, false);
        Set<String> loadRules = cs.getLoadRules();
        assertEquals(1, loadRules.size());
        assertEquals("\\a\\b", loadRules.iterator().next());
    }

    @Test
    public void testLineEndings() throws IOException {
        String rawCs1 = org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-LF.log"));
        String rawCs2 = org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs1 = new ConfigSpec(rawCs1, true);
        ConfigSpec cs2 = new ConfigSpec(rawCs2, true);
        assertEquals(cs1, cs2);
    }

    @Test
    public void testStripLoadRulesWindows() throws IOException {
        String rawCs = org.apache.commons.io.IOUtils.toString(getClass().getResourceAsStream("ct-catcs-2-CRLF.log"));
        ConfigSpec cs = new ConfigSpec(rawCs, false);
        cs = cs.stripLoadRules();
        assertTrue(cs.getLoadRules().isEmpty());
    }

}
