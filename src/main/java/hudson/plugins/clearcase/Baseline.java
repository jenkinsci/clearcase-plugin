package hudson.plugins.clearcase;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author kyosi
 */
@ExportedBean
public class Baseline {

    @Exported(visibility = 3)
    public String    baselineName;

    @Exported(visibility = 3)
    public Component componentDesc;

    @Exported(visibility = 3)
    public String    componentName;

    boolean          isNotLabeled;

    public Baseline(String componentName, boolean isNotLabeled) {
        super();
        this.componentName = componentName;
        this.isNotLabeled = isNotLabeled;
    }

    public Baseline(String baselineName, Component componentDesc) {
        super();
        this.baselineName = baselineName;
        this.componentDesc = componentDesc;
        this.componentName = componentDesc.getName();
    }

    public Baseline(String baselineName, Component componentDesc, boolean isNotLabeled) {
        super();
        this.baselineName = baselineName;
        this.componentDesc = componentDesc;
        this.componentName = componentDesc.getName();
        this.isNotLabeled = isNotLabeled;
    }

    public Baseline(String baselineName, String componentName) {
        super();
        this.baselineName = baselineName;
        this.componentName = componentName;
        this.componentDesc = null;
    }

    public String getBaselineName() {
        return baselineName;
    }

    public Component getComponentDesc() {
        return componentDesc;
    }

    public String getComponentName() {
        return (componentDesc != null ? componentDesc.getName() : componentName);
    }

    public boolean isNotLabeled() {
        return isNotLabeled;
    }

    public void setBaselineName(String baselineName) {
        this.baselineName = baselineName;
    }

    public void setComponentDesc(Component componentDesc) {
        this.componentDesc = componentDesc;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setNotLabeled(boolean isNotLabeled) {
        this.isNotLabeled = isNotLabeled;
    }
}