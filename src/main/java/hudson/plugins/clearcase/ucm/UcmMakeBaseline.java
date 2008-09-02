package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Descriptor.FormException;
import hudson.model.Result;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.PluginImpl;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.kohsuke.stapler.StaplerRequest;

/**
 * UcmMakeBaseline creates baselines on a ClearCase stream after a successful build.
 * The name and comment of the baseline can be changed using the namePattern and commentPattern
 * variables.
 * 
 * TODO: Handle stream locking during build to prevent 'faulty' baselines. 
 * Another solution might be to get the list of delivered activities (from the pre-build check) and 
 * only make a baseline for these activities. 
 * 
 * @author Peter Liljenberg
 */
public class UcmMakeBaseline extends Publisher {

    public final static Descriptor<Publisher> DESCRIPTOR = new UcmMakeBaselineDescriptor();
    private String namePattern;
    private String commentPattern;
    private boolean lockStream;
    private String user;
    private String password;

    public String getCommentPattern() {
        return commentPattern;
    }

    public boolean isLockStream() {
        return lockStream;
    }

    public String getNamePattern() {
        return namePattern;
    }

    public String getPassword() {
        return password;
    }

    public String getUser() {
        return user;
    }

    public static final class UcmMakeBaselineDescriptor extends Descriptor<Publisher> {

        public UcmMakeBaselineDescriptor() {
            super(UcmMakeBaseline.class);
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Makebaseline";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
            Publisher p = new UcmMakeBaseline(
                req.getParameter("mkbl.namepattern"),
                req.getParameter("mkbl.commentpattern"),
                req.getParameter("mkbl.lock") != null,
                req.getParameter("mkbl.user"),
                req.getParameter("mkbl.password"));
            return p;
        }
    }

    private UcmMakeBaseline(String namePattern, String commentPattern,
        boolean lock, String user, String password) {
        this.namePattern = namePattern;
        this.commentPattern = commentPattern;
        this.lockStream = lock;
        this.user = user;
        this.password = password;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    /*
    @Override
    public boolean prebuild(Build builder, BuildListener listener) {
    System.out.println("Locking stream during build");
    return true;
    }
    
    @Override
    public boolean prebuild(AbstractBuild<?, ?> builder, BuildListener listener) {
    if (this.lockStream) {
    System.out.println("Locking stream during build");
    }
    return true;
    }
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
        BuildListener listener) throws InterruptedException, IOException {

        if (build.getProject().getScm() instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();
            FilePath filePath = build.getProject().getWorkspace().child(scm.getViewName());

            HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(),
                getDescriptor().getDisplayName(), listener, filePath, launcher);

            if (build.getResult().equals(Result.SUCCESS)) {
                makeBaseline(build, clearToolLauncher, filePath);
                recommedBaseline(scm.getStream(), clearToolLauncher, filePath);
            } else {
                listener.getLogger().println("Not a UCM clearcase SCM, cannot create baseline");
            }
        /*
        if (this.lockStream) {
        System.out.println("Unlocking stream after build");
        }
         */
        }
        return true;
    }

    @Override
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }
    
    @SuppressWarnings("unchecked")
    private void makeBaseline(AbstractBuild build,
        HudsonClearToolLauncher clearToolLauncher, FilePath filePath) throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        String baselineName = Util.replaceMacro(namePattern, build.getEnvVars());
        String baselineComment = Util.replaceMacro(commentPattern, build.getEnvVars());
        cmd.add("mkbl");
        cmd.add("-comment");
        cmd.add(baselineComment);
        cmd.add("-incremental");
        cmd.add(baselineName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        
        // TODO: Parse results, throw exception if not OK         
        // "No changes in component" -> no baseline will be created, this is OK
        // "cleartool: Error" -> NOK
    }

    private void recommedBaseline(String stream,
        HudsonClearToolLauncher clearToolLauncher, FilePath filePath) throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chstream");
        cmd.add("-rec");
        cmd.add("-def");
        cmd.add(stream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        // TODO: Parse results, throw exception if not OK         
    }
}
