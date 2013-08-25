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
package hudson.plugins.clearcase.ucm.model;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.io.input.CharSequenceReader;
import org.fest.util.Strings;
import org.junit.Test;

public class ActivitiesTest {

    @Test
    public void parseEmpty() throws IOException {
        Reader reader = new StringReader("");
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).isEmpty();
        assertThat(activities.getRight()).isEmpty();
    }

    @Test
    public void parseInvalid() throws IOException {
        Reader reader = new StringReader("Some invalid output\nOijgdsoijgo");
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).isEmpty();
        assertThat(activities.getRight()).isEmpty();
    }

    @Test
    public void parseInvalidThatLooksLikeALine() throws IOException {
        Reader reader = new StringReader(">> Some invalid output\nOijgdsoijgo");
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).isEmpty();
        assertThat(activities.getRight()).isEmpty();
    }

    @Test
    public void parseOneActivityLeft() throws IOException {
        Reader reader = new StringReader("<< act1@\\pvob \"Activity 1\"");
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).hasSize(1);
        assertThat(activities.getRight()).isEmpty();
        Activity activity = activities.getLeft().iterator().next();
        assertThat(activity.getSelector()).isEqualTo("activity:act1@\\pvob");
        assertThat(activity.getHeadline()).isEqualTo("Activity 1");
    }

    @Test
    public void parseOneActivityRight() throws IOException {
        Reader reader = new StringReader(">> act1@\\pvob \"Activity 1\"");
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).isEmpty();
        assertThat(activities.getRight()).hasSize(1);
        Activity activity = activities.getRight().iterator().next();
        assertThat(activity.getSelector()).isEqualTo("activity:act1@\\pvob");
        assertThat(activity.getHeadline()).isEqualTo("Activity 1");
    }

    @Test
    public void parseTwoActivitiesOneLeftOneRight() throws IOException {
        String lines = Strings.join("<< act1@\\pvob \"Activity 1\"", ">> act2@\\pvob \"Activity 2\"").with("\n");
        Reader reader = new CharSequenceReader(lines);
        ActivitiesDelta activities = ActivitiesDelta.parse(reader);
        assertThat(activities.getLeft()).hasSize(1);
        assertThat(activities.getRight()).hasSize(1);
        Activity leftActivity = activities.getLeft().iterator().next();
        assertThat(leftActivity.getSelector()).isEqualTo("activity:act1@\\pvob");
        assertThat(leftActivity.getHeadline()).isEqualTo("Activity 1");
        Activity rightActivity = activities.getRight().iterator().next();
        assertThat(rightActivity.getSelector()).isEqualTo("activity:act2@\\pvob");
        assertThat(rightActivity.getHeadline()).isEqualTo("Activity 2");
    }

}
