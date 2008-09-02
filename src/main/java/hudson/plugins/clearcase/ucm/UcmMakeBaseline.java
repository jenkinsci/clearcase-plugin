package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
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
 * UcmMakeBaseline creates baselines on a ClearCase stream after a successful
 * build. The name and comment of the baseline can be changed using the
 * namePattern and commentPattern variables.
 * 
 * @author Peter Liljenberg
 */
public class UcmMakeBaseline extends Publisher {

	public final static Descriptor<Publisher> DESCRIPTOR = new UcmMakeBaselineDescriptor();
	private String namePattern;
	private String commentPattern;
	private boolean lockStream;
	private boolean recommend;
	private boolean streamSuccessfullyLocked;

	public String getCommentPattern() {
		return commentPattern;
	}

	public boolean isLockStream() {
		return lockStream;
	}

	public String getNamePattern() {
		return namePattern;
	}

	public boolean isRecommend() {
		return recommend;
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
					.getParameter("mkbl.recommend") != null);
			return p;
		}
	}

	private UcmMakeBaseline(String namePattern, String commentPattern,
			boolean lock, boolean recommend) {
		this.namePattern = namePattern;
		this.commentPattern = commentPattern;
		this.lockStream = lock;
		this.recommend = recommend;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
		if (this.lockStream) {
			ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();

			FilePath filePath = build.getProject().getWorkspace().child(
					scm.getViewName());
			Launcher launcher = Executor.currentExecutor().getOwner().getNode()
					.createLauncher(listener);
			HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
					PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(),
					getDescriptor().getDisplayName(), listener, filePath,
					launcher);
			try {
				this.streamSuccessfullyLocked = lockStream(scm.getStream(),
						clearToolLauncher, filePath);
			} catch (Exception ex) {
				listener.getLogger().println("Failed to lock stream: " + ex);
			}
			return this.streamSuccessfullyLocked;
		}
		return true;

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
				boolean createdBaseline = makeBaseline(build, clearToolLauncher, filePath);
				if (this.recommend && createdBaseline) {
					recommedBaseline(scm.getStream(), clearToolLauncher,
							filePath);
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
	private boolean makeBaseline(AbstractBuild build,
			HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
			throws InterruptedException, IOException {

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		String baselineName = Util
				.replaceMacro(namePattern, build.getEnvVars());
		String baselineComment = Util.replaceMacro(commentPattern, build
				.getEnvVars());
		cmd.add("mkbl");
		cmd.add("-comment");
		cmd.add(baselineComment);
		cmd.add("-incremental");
		cmd.add(baselineName);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
		baos.close();
		String cleartoolResult = baos.toString();
		if (cleartoolResult.contains("cleartool: Error")) {
			return false;
		}
		return true;
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
}
