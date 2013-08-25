/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe, Yosi Kalmanson
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

import hudson.model.Action;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ClearCaseDataAction implements Action {

    @Exported(visibility = 3)
    public String         cspec;

    @Exported(visibility = 3)
    public List<Baseline> latestBlsOnConfiguredStream;

    @Exported(visibility = 3)
    public String         stream;

    public ClearCaseDataAction() {
        super();
    }

    public String getCspec() {
        return cspec;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    public List<Baseline> getLatestBlsOnConfiguredStream() {
        return latestBlsOnConfiguredStream;
    }

    public String getStream() {
        return stream;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public void setCspec(String cspec) {
        this.cspec = cspec;
    }

    public void setLatestBlsOnConfiguredStream(List<Baseline> latestBlsOnConfiguredStream) {
        this.latestBlsOnConfiguredStream = latestBlsOnConfiguredStream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

}
