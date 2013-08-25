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
import static org.fest.assertions.api.Assertions.extractProperty;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.model.Versions;
import hudson.plugins.clearcase.ucm.model.Activity;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.model.UcmSelector;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ActivityServiceTest {
    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    ClearTool               ct;

    private ActivityService instance;

    @Test
    public void getContributingActivitiesEmpty() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[contrib_acts]Xp"), anyString())).thenReturn(new StringReader(""));
        Collection<Activity> contributingActivities = instance.getContributingActivities(UcmSelector.parse("act1@\\pvob", Activity.class));
        assertThat(contributingActivities).isEmpty();
    }

    @Test
    public void getContributingActivitiesSeveralActivities() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[contrib_acts]Xp"), anyString())).thenReturn(
                new StringReader("activity:act2@\\pvob activity:act3@\\pvob"));
        Collection<Activity> contributingActivities = instance.getContributingActivities(UcmSelector.parse("act1@\\pvob", Activity.class));
        assertThat(contributingActivities).hasSize(2);
        assertThat(extractProperty("name").from(contributingActivities)).containsExactly("act2", "act3");
    }

    @Before
    public void setUp() {
        instance = new ActivityService(ct);
    }

    @Test
    public void testGetHeadline() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[headline]p"), anyString())).thenReturn(new StringReader("Something"));
        assertThat(instance.getHeadline(UcmSelector.parse("act1@\\pvob", Activity.class))).isEqualTo("Something");
    }

    @Test
    public void testGetStream() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[stream]Xp"), anyString())).thenReturn(new StringReader("stream:stream1@\\pvob"));
        assertThat(instance.getStream(UcmSelector.parse("activity:act1@\\pvob", Activity.class))).isEqualTo(
                UcmSelector.parse("stream:stream1@\\pvob", Stream.class));
    }

    @Test
    public void testGetVersionsEmptyActivity() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[versions]Cp"), anyString())).thenReturn(new StringReader(""));
        Versions versions = instance.getVersions(UcmSelector.parse("activity:act1@\\pvob", Activity.class), "M:");
        assertThat(versions.getVersions()).isEmpty();
    }

    @Test
    public void testGetVersionsMultipleVersion() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[versions]Cp"), anyString())).thenReturn(
                new StringReader("M:\\view\\vob\\folder\\file\\1 M:\\view\\vob\\folder\\file\\2"));
        Versions versions = instance.getVersions(UcmSelector.parse("activity:act1@\\pvob", Activity.class), "M:\\view\\");
        assertThat(versions.getVersions()).hasSize(2);
        assertThat(extractProperty("path").from(versions.getVersions())).containsExactly("vob\\folder\\file\\1", "vob\\folder\\file\\2");
    }

    @Test
    public void testGetVersionsOneVersion() throws IOException, InterruptedException {
        when(ct.lsactivity(eq("activity:act1@\\pvob"), eq("%[versions]Cp"), anyString())).thenReturn(new StringReader("M:\\view\\vob\\folder\\file\\1"));
        Versions versions = instance.getVersions(UcmSelector.parse("activity:act1@\\pvob", Activity.class), "M:\\view\\");
        assertThat(versions.getVersions()).hasSize(1);
        assertThat(extractProperty("path").from(versions.getVersions())).containsExactly("vob\\folder\\file\\1");
    }

}
