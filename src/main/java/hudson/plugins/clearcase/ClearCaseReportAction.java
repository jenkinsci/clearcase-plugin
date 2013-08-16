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
