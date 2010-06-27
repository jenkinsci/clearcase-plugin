package hudson.plugins.clearcase;

import java.util.List;

import hudson.model.Action;
import hudson.model.AbstractBuild;

public class ClearCaseReportAction implements Action {

    private AbstractBuild<?, ?> build;
    private static String urlName = "clearcaseInformation";

    public ClearCaseReportAction(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    public String getIconFileName() {
        return "gear2.gif";
    }

    public String getDisplayName() {
        return "ClearCase Information";
    }

    public String getUrlName() {
        return urlName;
    }

    public static String getUrlNameStat() {
        return urlName;
    }

    // Used by the index.jelly of this class to include the sidebar.jelly
    public AbstractBuild<?, ?> getOwner() {
        return build;
    }

    public String getConfigSpecHtml() {
        String configSpecHtml = getCspec();
        configSpecHtml = configSpecHtml.replaceAll("\n", "<br/>");
        return configSpecHtml;
    }

    public boolean isCspec() {
        String cspec = getCspec();
        return (cspec != null && cspec.trim().length() > 0);
    }

    public List<Baseline> getBaselines() {
        ClearCaseDataAction clearCaseDataAction = build.getAction(ClearCaseDataAction.class);

        if (clearCaseDataAction != null) {
            return clearCaseDataAction.getLatestBlsOnConfiguredStream();
        } else {
            return null;
        }
    }

    public boolean isBaselineInfo() {
        ClearCaseDataAction baselinesAction = build.getAction(ClearCaseDataAction.class);
        return (baselinesAction != null);
    }

    public String getStream() {
        String stream = null;

        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            stream = dataAction.getStream();

        return stream;
    }

    private String getCspec() {
        String cspec = null;

        ClearCaseDataAction dataAction = build.getAction(ClearCaseDataAction.class);
        if (dataAction != null)
            cspec = dataAction.getCspec();

        return cspec;
    }

}
