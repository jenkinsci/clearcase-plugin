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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.DiffBlOptions;
import hudson.plugins.clearcase.ucm.model.ActivitiesDelta;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.ucm.model.UcmSelector;

import java.io.IOException;
import java.io.StringReader;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaselineServiceTest {

    @Mock(answer = Answers.RETURNS_SMART_NULLS)
    ClearTool       ct;
    BaselineService instance;

    @Test
    public void compareTwoBaselines() throws IOException, InterruptedException {
        Baseline from = UcmSelector.parse("b1@\\pvob", Baseline.class);
        Baseline to = UcmSelector.parse("b2@\\pvob", Baseline.class);
        String result = ">> act1@\\pvob \"Activity 1\"";

        when(ct.diffbl(eq(EnumSet.of(DiffBlOptions.ACTIVITIES)), eq("baseline:b1@\\pvob"), eq("baseline:b2@\\pvob"), anyString())).thenReturn(
                new StringReader(result));
        ActivitiesDelta activities = instance.compare(from, to);
        assertThat(activities.getLeft()).isEmpty();
        assertThat(activities.getRight()).hasSize(1);
    }

    @Test
    public void getComponent() throws IOException, InterruptedException {
        Baseline baseline = UcmSelector.parse("baseline:name@\\pvob", Baseline.class);
        when(ct.describe(eq("%[component]Xp"), anyString(), eq("baseline:name@\\pvob"))).thenReturn(new StringReader("component:cname@\\pvob"));
        Component component = instance.getComponent(baseline);
        assertThat(component.getName()).isEqualTo("cname");
        assertThat(component.getPvob()).isEqualTo("\\pvob");
    }

    @Before
    public void setUp() {
        instance = new BaselineService(ct);
    }

}
