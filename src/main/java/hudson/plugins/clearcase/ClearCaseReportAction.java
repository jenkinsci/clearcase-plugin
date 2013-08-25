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
import hudson.model.AbstractBuild;

import java.util.List;

public class ClearCaseReportAction implements Action {

    private static String       urlName = "clearcaseInformation";
    private AbstractBuild<?, ?> build;

    public ClearCaseReportAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public List<Baseline> getBaselines() {
        ClearCaseDataAction clearCaseDataAction = build.getAction(ClearCaseDataAction.class);

        if (clearCaseDataAction != null) {
            return clearCaseDataAction.getLatestBlsOnConfiguredStream();
        }
        return null;
    }

    public String getConfigSpecHtml() {
        String configSpecHtml = getCspec();
        configSpecHtml = configSpecHtml.replaceAll("\n", "<br/>");
        return configSpecHtml;
    }

    @Override
    public String getDisplayName() {
        return "ClearCase Information";
    }

    @Override
    public String getIconFileName() {
        return "gear2.gif";
    }

    // Used by the index.jelly of this class to include the sidebar.jelly
    public AbstractBuild<?, ?> getOwner() {
        return build;
    }

    public String getStream() {
        String stream = null;

        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            stream = dataAction.getStream();

        return stream;
    }

    @Override
    public String getUrlName() {
        return urlName;
    }

    public boolean isBaselineInfo() {
        ClearCaseDataAction baselinesAction = build.getAction(ClearCaseDataAction.class);
        return (baselinesAction != null);
    }

    public boolean isCspec() {
        String cspec = getCspec();
        return (cspec != null && cspec.trim().length() > 0);
    }

    private String getCspec() {
        String cspec = null;

        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            cspec = dataAction.getCspec();

        return cspec;
    }

    public static String getUrlNameStat() {
        return urlName;
    }

}
