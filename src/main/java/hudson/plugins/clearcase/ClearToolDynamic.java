package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.util.ArgumentListBuilder;
import java.io.IOException;

public class ClearToolDynamic extends ClearToolExec {

    private transient String viewDrive;

    public ClearToolDynamic(ClearToolLauncher launcher, String clearToolExec, String viewDrive) {
        super(launcher, clearToolExec);
        this.viewDrive = viewDrive;
    }

    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return new FilePath(launcher.getWorkspace().getChannel(), viewDrive);
    }

    public void setcs(String viewName, String configSpec) throws IOException,
            InterruptedException {
        FilePath configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("setcs");
        cmd.add("-tag");
        cmd.add(viewName);
        cmd.add(configSpecFile.getName());
        launcher.run(cmd.toCommandArray(), null, null, null);

        configSpecFile.delete();
    }

    public void mkview(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support mkview");
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support mkview");        
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support rmview");
    }

    public void update(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support update");
    }

    public void setView(String viewTag)  throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("setview");
        cmd.add(viewTag);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }
}
