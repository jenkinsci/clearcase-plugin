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
package hudson.plugins.clearcase.ucm.service;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.ucm.model.UcmSelector;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ComponentServiceTest {
    @Mock
    ClearTool        ct;
    ComponentService instance;

    @Before
    public void setUp() {
        instance = new ComponentService(ct);
    }

    @Test
    public void testGetRootDirMultiple() throws IOException, InterruptedException {
        Component[] components = new Component[2];
        components[0] = UcmSelector.parse("component:name1@\\pvob", Component.class);
        components[1] = UcmSelector.parse("component:name2@\\pvob", Component.class);
        when(ct.describe(eq("%[root_dir]Xp"), anyString(), eq("component:name1@\\pvob"))).thenReturn(new StringReader("\\vob1\\cmp1"));
        when(ct.describe(eq("%[root_dir]Xp"), anyString(), eq("component:name2@\\pvob"))).thenReturn(new StringReader("\\vob1\\cmp2"));
        String[] rootDirs = instance.getRootDir(components);
        assertArrayEquals(new String[] { "\\vob1\\cmp1", "\\vob1\\cmp2" }, rootDirs);
    }

    @Test
    public void testGetRootDirMultipleWithComposite() throws IOException, InterruptedException {
        Component[] components = new Component[2];
        components[0] = UcmSelector.parse("component:name1@\\pvob", Component.class);
        components[1] = UcmSelector.parse("component:composite@\\pvob", Component.class);
        when(ct.describe(eq("%[root_dir]Xp"), anyString(), eq("component:name1@\\pvob"))).thenReturn(new StringReader("\\vob1\\cmp1"));
        when(ct.describe(eq("%[root_dir]Xp"), anyString(), eq("component:composite@\\pvob"))).thenReturn(new StringReader(""));
        String[] rootDirs = instance.getRootDir(components);
        assertArrayEquals(new String[] { "\\vob1\\cmp1" }, rootDirs);
    }

    @Test
    public void testGetRootDirSingle() throws IOException, InterruptedException {
        Component component = UcmSelector.parse("component:name@\\pvob", Component.class);
        when(ct.describe(eq("%[root_dir]Xp"), anyString(), eq("component:name@\\pvob"))).thenReturn(new StringReader("\\vob1\\cmp1"));
        String rootDir = instance.getRootDir(component);
        assertEquals("\\vob1\\cmp1", rootDir);
    }

}
