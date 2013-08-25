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

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.model.Versions;
import hudson.plugins.clearcase.ucm.model.Activity;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.model.UcmSelector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ActivityService extends ClearcaseService {

    ActivityService(ClearTool clearTool) {
        super(clearTool);
    }

    public Collection<Activity> getContributingActivities(Activity activity) throws IOException, InterruptedException {
        String output = lsActivityToString(activity, "%[contrib_acts]Xp");
        Collection<Activity> activities = new ArrayList<Activity>();
        for (String activityId : StringUtils.split(output)) {
            activities.add(UcmSelector.parse(activityId, Activity.class));
        }
        return activities;
    }

    public String getHeadline(Activity activity) throws IOException, InterruptedException {
        String headline = activity.getHeadline();
        if (headline == null) {
            headline = lsActivityToString(activity, "%[headline]p");
        }
        return headline;
    }

    public Stream getStream(Activity activity) throws IOException, InterruptedException {
        String output = lsActivityToString(activity, "%[stream]Xp");
        return UcmSelector.parse(output, Stream.class);
    }

    public Versions getVersions(Activity activity, String viewPath) throws IOException, InterruptedException {
        String output = lsActivityToString(activity, "%[versions]Cp", viewPath);
        return Versions.parse(output, viewPath, ", ");
    }

    private String lsActivityToString(Activity activity, String format) throws IOException, InterruptedException {
        return lsActivityToString(activity, format, null);
    }

    private String lsActivityToString(Activity activity, String format, String viewPath) throws IOException, InterruptedException {
        return IOUtils.toString(clearTool.lsactivity(activity.getSelector(), format, viewPath));
    }

}
