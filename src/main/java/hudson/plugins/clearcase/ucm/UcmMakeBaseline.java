package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import hudson.model.Result;
import hudson.model.StringParameterValue;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.PluginImpl;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.StaplerRequest;

/**
 * UcmMakeBaseline creates baselines on a ClearCase stream after a successful
 * build. The name and comment of the baseline can be changed using the
 * namePattern and commentPattern variables.
 * 
 * @author Peter Liljenberg
 */
public class UcmMakeBaseline extends Publisher {

    private static final String ENV_CC_BASELINE_NAME = "CC_BASELINE_NAME";

    private transient List<String> latestBaselines = null;
    
    private transient List<String> createdBaselines = null;

    public final static Descriptor<Publisher> DESCRIPTOR = new UcmMakeBaselineDescriptor();

    private final String namePattern;

    private final String commentPattern;

    private final boolean lockStream;

    private final boolean recommend;

    private transient boolean streamSuccessfullyLocked;

    private final boolean fullBaseline;

    private final boolean identical;

    public String getCommentPattern() {
        return this.commentPattern;
    }

    public boolean isLockStream() {
        return this.lockStream;
    }

    public String getNamePattern() {
        return this.namePattern;
    }

    public boolean isRecommend() {
        return this.recommend;
    }

    public boolean isFullBaseline() {
        return this.fullBaseline;
    }

    public boolean isIdentical() {
        return this.identical;
    }

    public static final class UcmMakeBaselineDescriptor extends
            Descriptor<Publisher> {

        public UcmMakeBaselineDescriptor() {
            super(UcmMakeBaseline.class);
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Makebaseline";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
            Publisher p = new UcmMakeBaseline(req
                    .getParameter("mkbl.namepattern"), req
                    .getParameter("mkbl.commentpattern"), req
                    .getParameter("mkbl.lock") != null, req
                    .getParameter("mkbl.recommend") != null, req
                    .getParameter("mkbl.fullBaseline") != null, req
                    .getParameter("mkbl.identical") != null);
            return p;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clearcase/ucm/mkbl/help.html";
        }
    }

    private UcmMakeBaseline(final String namePattern,
            final String commentPattern, final boolean lock,
            final boolean recommend, final boolean fullBaseline,
            final boolean identical) {
        this.namePattern = namePattern;
        this.commentPattern = commentPattern;
        this.lockStream = lock;
        this.recommend = recommend;
        this.fullBaseline = fullBaseline;
        this.identical = identical;
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {

        ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();

        FilePath filePath = build.getProject().getWorkspace().child(
                scm.getViewName());
        Launcher launcher = Executor.currentExecutor().getOwner().getNode()
                .createLauncher(listener);
        HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
                PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), getDescriptor()
                        .getDisplayName(), listener, filePath, launcher);
        if (this.lockStream) {
            try {
                this.streamSuccessfullyLocked = lockStream(scm.getStream(),
                        clearToolLauncher, filePath);
            } catch (Exception ex) {
                listener.getLogger().println("Failed to lock stream: " + ex);
                return false;
            }
        }
        try {
            makeBaseline(build, clearToolLauncher, filePath);
            this.latestBaselines = getLatestBaselineNames(clearToolLauncher,
                    filePath);
            addBuildParameter(build);
        } catch (Exception ex) {
            listener.getLogger().println("Failed to create baseline: " + ex);
            return false;
        }

        return true;

    }

    private void addBuildParameter(AbstractBuild<?, ?> build) {
        if (this.latestBaselines != null && !this.latestBaselines.isEmpty()) {
            ArrayList<ParameterValue> parameters = new ArrayList<ParameterValue>();
            String baselineName = latestBaselines.get(0);
            parameters.add(new StringParameterValue(ENV_CC_BASELINE_NAME,
                    baselineName));
            build.addAction(new ParametersAction(parameters, build));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {

        if (build.getProject().getScm() instanceof ClearCaseUcmSCM) {
            ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();
            FilePath filePath = build.getProject().getWorkspace().child(
                    scm.getViewName());

            HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
                    PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(),
                    getDescriptor().getDisplayName(), listener, filePath,
                    launcher);

            if (build.getResult().equals(Result.SUCCESS)) {
            	// On success, promote all current baselines in stream
				for (String baselineName : this.latestBaselines) {
					promoteBaselineToBuiltLevel(scm.getStream(), clearToolLauncher,
							filePath, baselineName);
				}
                if (this.recommend) {
                    recommedBaseline(scm.getStream(), clearToolLauncher,
                            filePath);
                }
            } else if (build.getResult().equals(Result.FAILURE)) {
            	// On failure, demote only baselines created in this build
            	for (String baselineName : this.createdBaselines) {
            		// Find full baseline name from latest baselines
            		String realBaselineName = null;
            		for (String fullBaselineName : this.latestBaselines ) {
            			if (fullBaselineName.startsWith(baselineName)) {
            				realBaselineName = fullBaselineName;
            			}
            		}
            		if (realBaselineName == null) {
            			listener.getLogger().println("Couldn't find baseline name for "+baselineName);
            		} else {
            			demoteBaselineToRejectedLevel(scm.getStream(), clearToolLauncher,
            					filePath, realBaselineName);
            		}
            	}
            }

            if (this.lockStream && this.streamSuccessfullyLocked) {
                unlockStream(scm.getStream(), clearToolLauncher, filePath);
            }
        } else {
            listener.getLogger().println(
                    "Not a UCM clearcase SCM, cannot create baseline");
        }
        return true;
    }

    @Override
    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }

    private void unlockStream(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("unlock");
        cmd.add("stream:");
        cmd.add(stream);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);

    }

    /**
     * Locks the stream used during build to ensure the streams integrity during
     * the whole build process, i.e. we want to make sure that no DELIVERs are
     * made to the stream during build.
     * 
     * @return true if the stream was locked
     */
    private boolean lockStream(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lock");
        cmd.add("stream:");
        cmd.add(stream);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            return false;
        }
        baos.close();
        return true;
    }

    @SuppressWarnings("unchecked")
    private void makeBaseline(AbstractBuild build,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws Exception {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        String baselineName = Util
                .replaceMacro(namePattern, build.getEnvVars());
        String baselineComment = Util.replaceMacro(commentPattern, build
                .getEnvVars());
        cmd.add("mkbl");
        if (this.identical) {
            cmd.add("-identical");
        }
        cmd.add("-comment");
        cmd.add(baselineComment);
        if (fullBaseline) {
            cmd.add("-full");
        } else {
            cmd.add("-incremental");
        }
        cmd.add(baselineName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            throw new Exception("Failed to make baseline, reason: "
                    + cleartoolResult);
        }
        
        this.createdBaselines = new ArrayList<String>();
        
    	Pattern pattern = Pattern.compile("Created baseline \".+?\"");
    	Matcher matcher = pattern.matcher(cleartoolResult);
    	while (matcher.find()) {
    		String match = matcher.group();
    		String newBaseline = match.substring(match.indexOf("\"")+1, match.length()-1);
    		this.createdBaselines.add(newBaseline);
    	}        

    }

    private void recommedBaseline(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chstream");
        cmd.add("-rec");
        cmd.add("-def");
        cmd.add(stream);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);
    }

    private void promoteBaselineToBuiltLevel(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath,
            String blName) throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chbl");
        cmd.add("-c");
        cmd.add("Hudson promoted baseline to BUILT");
        cmd.add("-level");
        cmd.add("BUILT");

        cmd.add(blName);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);
    }
    
    private void demoteBaselineToRejectedLevel(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath,
            String blName) throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chbl");
        cmd.add("-c");
        cmd.add("Hudson demoted baseline to REJECTED");
        cmd.add("-level");
        cmd.add("REJECTED");

        cmd.add(blName);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);
    }    

    private List<String> getLatestBaselineNames(
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws Exception {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lsstream");
        cmd.add("-fmt");
        cmd.add("%[latest_bls]Xp");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        String cleartoolResult = baos.toString();
        String prefix = "baseline:";
        if (cleartoolResult != null && cleartoolResult.startsWith(prefix)) {
			List<String> baselineNames = new ArrayList<String>();
			String[] baselineNamesSplit = cleartoolResult.split("baseline:");
			for (String baselineName : baselineNamesSplit) {
				String baselineNameTrimmed = baselineName.trim();
				if (!baselineNameTrimmed.equals("")) {
					baselineNames.add(baselineNameTrimmed);
				}
			}
            return baselineNames;
        }
        throw new Exception("Failed to get baselinename, reason: "
                + cleartoolResult);

    }

}
