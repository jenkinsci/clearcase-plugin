/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.BuildTimeBased;
import hudson.plugins.clearcase.LoadRulesAware;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.scm.SCMRevisionState;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;

import org.kohsuke.stapler.Stapler;

/**
 * Represents the repository state at a given point of time
 */
public class UcmRevisionState extends SCMRevisionState implements BuildTimeBased, LoadRulesAware {

    private final Baseline[] baselines;

    private final String[]   loadRules;

    private final long       timestamp;

    public UcmRevisionState(Baseline[] baselines, String[] loadRules, long timestamp) {
        super();
        this.baselines = baselines;
        this.loadRules = loadRules;
        this.timestamp = timestamp;
    }

    public Baseline[] getBaselines() {
        return Arrays.copyOf(baselines, baselines.length);
    }

    @Override
    public Date getBuildTime() {
        return new Date(timestamp);
    }

    @Override
    public String getDisplayName() {
        return "UCM Revision State";
    }

    public String getFormattedTimestamp() {
        Date date = new Date(timestamp);
        return DateFormat.getDateTimeInstance().format(date);
    }

    @Override
    public String getIconFileName() {
        return "clipboard.png";
    }

    @Override
    public String[] getLoadRules() {
        return Arrays.copyOf(loadRules, loadRules.length);
    }

    public AbstractBuild getOwner() {
        return Stapler.getCurrentRequest().findAncestorObject(AbstractBuild.class);
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public String getUrlName() {
        return "revisionState";
    }

    @Override
    public String toString() {
        return "UcmRevisionState[timestamp=" + timestamp + ", baselines=" + Arrays.asList(baselines) + ", loadRules=" + Arrays.asList(loadRules) + "]";
    }

}
