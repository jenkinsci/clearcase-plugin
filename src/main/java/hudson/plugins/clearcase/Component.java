package hudson.plugins.clearcase;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author kyosi
 */
@ExportedBean
public class Component {

    @Exported(visibility = 3)
    private String name;

    @Exported(visibility = 3)
    private boolean modifiable;

    public Component(String name, boolean modifiable) {
        this.name = name;
        this.modifiable = modifiable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean isModifiable) {
        this.modifiable = isModifiable;
    }
}