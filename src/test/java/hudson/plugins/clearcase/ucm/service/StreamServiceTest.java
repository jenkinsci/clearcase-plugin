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

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.model.Versions;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.model.UcmSelector;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StreamServiceTest {
    @Mock
    ClearTool     ct;
    StreamService instance;

    @Before
    public void setUp() {
        instance = new StreamService(ct);
    }

    @Test
    public void testGetFoundationBaselines() throws IOException, InterruptedException {
        Stream stream = UcmSelector.parse("stream:name@\\pvob", Stream.class);
        when(ct.describe(eq("%[found_bls]Xp"), anyString(), eq("stream:name@\\pvob"))).thenReturn(
                new StringReader("baseline:bl1@\\pvob baseline:bl2@\\pvob baseline:bl3@\\pvob"));
        Baseline[] baselines = instance.getFoundationBaselines(stream);
        assertEquals(3, baselines.length);
        assertEquals("bl1", baselines[0].getName());
        assertEquals("bl2", baselines[1].getName());
        assertEquals("bl3", baselines[2].getName());
    }

    @Test
    public void testGetLatestBaselines() throws IOException, InterruptedException {
        Stream stream = UcmSelector.parse("stream:name@\\pvob", Stream.class);
        when(ct.describe(eq("%[latest_bls]Xp"), anyString(), eq("stream:name@\\pvob"))).thenReturn(
                new StringReader("baseline:bl1@\\pvob baseline:bl2@\\pvob baseline:bl3@\\pvob"));
        Baseline[] baselines = instance.getLatestBaselines(stream);
        assertEquals(3, baselines.length);
        assertEquals("bl1", baselines[0].getName());
        assertEquals("bl2", baselines[1].getName());
        assertEquals("bl3", baselines[2].getName());
    }
    
    @Test
    public void testGetVersionsEmpty() throws IOException, InterruptedException{
        Stream stream = UcmSelector.parse("stream:name@\\pvob", Stream.class);
        when(ct.lsactivityIn(anyString(), anyString(), anyString())).thenReturn(new StringReader(""));
        Versions versions = instance.getVersions(stream, "M:\\view");
        verify(ct).lsactivityIn(eq("stream:name@\\pvob"), eq("%[versions]p\\n"), eq("M:\\view"));
        assertThat(versions.getVersions()).isEmpty();
    }
    
    @Test
    public void testGetVersionsOne() throws IOException, InterruptedException{
        Stream stream = UcmSelector.parse("stream:name@\\pvob", Stream.class);
        when(ct.lsactivityIn(anyString(), anyString(), anyString())).thenReturn(new StringReader("M:\\jcp_v17.0_be_int\\be1111_core\\config\\pom-templates\\build.properties@@\\main\\jcp_main_be_root\\jcp_v17.0_be_int\\1"));
        when(ct.pwv("jcp_v17.0_be_int")).thenReturn("M:\\jcp_v17.0_be_int");
        Versions versions = instance.getVersions(stream, "jcp_v17.0_be_int");
        verify(ct).lsactivityIn(eq("stream:name@\\pvob"), eq("%[versions]p\\n"), eq("jcp_v17.0_be_int"));
        assertThat(versions.getVersions()).hasSize(1);
        assertThat(versions.iterator().next().getPath()).isEqualTo("be1111_core\\config\\pom-templates\\build.properties@@\\main\\jcp_main_be_root\\jcp_v17.0_be_int\\1");
    }

}
