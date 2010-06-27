package hudson.plugins.clearcase;

import hudson.model.Action;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class ClearCaseDataAction implements Action {

    @Exported(visibility = 3)
    public List<Baseline> latestBlsOnConfiguredStream;

    @Exported(visibility = 3)
    public String cspec;

    @Exported(visibility = 3)
    public String stream;

    public ClearCaseDataAction() {
        super();
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    public List<Baseline> getLatestBlsOnConfiguredStream() {
        return latestBlsOnConfiguredStream;
    }

    public void setLatestBlsOnConfiguredStream(List<Baseline> latestBlsOnConfiguredStream) {
        this.latestBlsOnConfiguredStream = latestBlsOnConfiguredStream;
    }

    public String getCspec() {
        return cspec;
    }

    public void setCspec(String cspec) {
        this.cspec = cspec;
    }

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

}
