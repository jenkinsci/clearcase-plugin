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
 * 
 * @author Gregory Boissinot 2008-10-11 Add the rebase dynamic view feature
 *         2008-11-21 Restric the baseline creation on read/write components
 *         2009-03-02 Add the dynamic view support for the make baseline
 *         2009-03-22 'The createdBaselines' follow now the same model of the
 *         'latestBaselines' and 'readWriteComponents' fields.
 */
public class UcmMakeBaseline extends Publisher {

	private static final String ENV_CC_BASELINE_NAME = "CC_BASELINE_NAME";

	private transient List<String> readWriteComponents = null;

	private transient List<String> latestBaselines = new ArrayList<String>();

	private transient List<String> createdBaselines = null;

	public final static Descriptor<Publisher> DESCRIPTOR = new UcmMakeBaselineDescriptor();

	private final String namePattern;

	private final String commentPattern;

	private final boolean lockStream;

	private final boolean recommend;

	private transient boolean streamSuccessfullyLocked;

	private final boolean fullBaseline;

	private final boolean identical;

	private final String dynamicViewName;

	private final boolean rebaseDynamicView;

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

	public String getDynamicViewName() {
		return this.dynamicViewName;
	}

	public boolean isRebaseDynamicView() {
		return this.rebaseDynamicView;
	}

	public List<String> getReadWriteComponents() {
		return readWriteComponents;
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
					.getParameter("mkbl.identical") != null, req
					.getParameter("mkbl.rebaseDynamicView") != null, req
					.getParameter("mkbl.dynamicViewName"));
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
			final boolean identical, final boolean rebaseDynamicView,
			final String dynamicViewName) {
		this.namePattern = namePattern;
		this.commentPattern = commentPattern;
		this.lockStream = lock;
		this.recommend = recommend;
		this.fullBaseline = fullBaseline;
		this.identical = identical;
		this.rebaseDynamicView = rebaseDynamicView;
		this.dynamicViewName = dynamicViewName;
	}

	@Override
	public boolean needsToRunAfterFinalized() {
		return true;
	}

	@Override
	public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {

		ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();

		Launcher launcher = Executor.currentExecutor().getOwner().getNode()
				.createLauncher(listener);
		HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(
				build, listener, launcher);

		FilePath filePath = build.getProject().getWorkspace().child(
				scm.generateNormalizedViewName(build, launcher));

		if (this.lockStream) {
			try {
				this.streamSuccessfullyLocked = lockStream(scm.getStream(),
						clearToolLauncher, scm, filePath);
			} catch (Exception ex) {
				listener.getLogger().println("Failed to lock stream: " + ex);
				return false;
			}
		}
		try {

			// Get read/write component
			this.readWriteComponents = getReadWriteComponent(clearToolLauncher,
					scm, filePath);

			if (readWriteComponents.size() != 0) {
				this.createdBaselines = makeBaseline(build, clearToolLauncher,
						scm, filePath);
				this.latestBaselines = getLatestBaselineNames(
						clearToolLauncher, scm, filePath);

				addBuildParameter(build);
			}

		} catch (Exception ex) {
			listener.getLogger().println("Failed to create baseline: " + ex);
			return false;
		}

		return true;

	}

	private HudsonClearToolLauncher getHudsonClearToolLauncher(
			AbstractBuild<?, ?> build, BuildListener listener, Launcher launcher) {
		FilePath workspaceRoot = build.getProject().getWorkspace();
		HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
				PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(), getDescriptor()
						.getDisplayName(), listener, workspaceRoot, launcher);
		return clearToolLauncher;
	}

	private void addBuildParameter(AbstractBuild<?, ?> build) {
		if (this.latestBaselines != null && !this.latestBaselines.isEmpty()) {
			ArrayList<ParameterValue> parameters = new ArrayList<ParameterValue>();
			String baselineName = latestBaselines.get(0);
			parameters.add(new StringParameterValue(ENV_CC_BASELINE_NAME,
					baselineName));
			build.addAction(new ParametersAction(parameters));
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		if (build.getProject().getScm() instanceof ClearCaseUcmSCM) {
			ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();
			FilePath filePath = build.getProject().getWorkspace().child(
					scm.generateNormalizedViewName(build, launcher));

			HudsonClearToolLauncher clearToolLauncher = getHudsonClearToolLauncher(
					build, listener, launcher);

			if (build.getResult().equals(Result.SUCCESS)) {
				// On success, promote all current baselines in stream
				for (String baselineName : this.latestBaselines) {
					promoteBaselineToBuiltLevel(scm.getStream(),
							clearToolLauncher, scm, filePath, baselineName);
				}
				if (this.recommend) {
					recommedBaseline(scm.getStream(), clearToolLauncher, scm,
							filePath);
				}

				// Rebase a dynamic view
				if (this.rebaseDynamicView) {
					for (String baseline : this.latestBaselines) {
						rebaseDynamicView(clearToolLauncher, scm, filePath,
								this.dynamicViewName, baseline);
					}
				}

			} else if (build.getResult().equals(Result.FAILURE)) {

				List<String> alreadyRejected = new ArrayList<String>();

				// On failure, demote only baselines created in this build
				for (String baselineName : this.createdBaselines) {

					// Find full baseline name from latest baselines
					String realBaselineName = null;
					for (String fullBaselineName : this.latestBaselines) {
						if (fullBaselineName.startsWith(baselineName)) {
							if (!alreadyRejected.contains(fullBaselineName)) {
								realBaselineName = fullBaselineName;
							}
						}
					}
					if (realBaselineName == null) {
						listener.getLogger().println(
								"Couldn't find baseline name for "
										+ baselineName);
					} else {
						demoteBaselineToRejectedLevel(scm.getStream(),
								clearToolLauncher, scm, filePath,
								realBaselineName);
						alreadyRejected.add(realBaselineName);
					}
				}
			}

			if (this.lockStream && this.streamSuccessfullyLocked) {
				unlockStream(scm.getStream(), clearToolLauncher, scm, filePath);
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

	private void rebaseDynamicView(HudsonClearToolLauncher clearToolLauncher,
			ClearCaseUcmSCM scm, FilePath filePath, String dynamicView,
			String blName) throws InterruptedException, IOException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add("rebase");
		cmd.add("-baseline");
		cmd.add(blName);
		cmd.add("-view");
		cmd.add(dynamicView);
		cmd.add("-complete");

		clearToolLauncher.run(cmd.toCommandArray(), null, null,
				clearToolLauncherPath);
	}

	private void unlockStream(String stream,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws IOException, InterruptedException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		cmd.add("unlock");
		cmd.add("stream:" + stream);

		clearToolLauncher.run(cmd.toCommandArray(), null, null,
				clearToolLauncherPath);

	}

	/**
	 * Locks the stream used during build to ensure the streams integrity during
	 * the whole build process, i.e. we want to make sure that no DELIVERs are
	 * made to the stream during build.
	 * 
	 * @return true if the stream was locked
	 */
	private boolean lockStream(String stream,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws IOException, InterruptedException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		cmd.add("lock");
		cmd.add("stream:" + stream);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		clearToolLauncher.run(cmd.toCommandArray(), null, baos,
				clearToolLauncherPath);
		String cleartoolResult = baos.toString();
		if (cleartoolResult.contains("cleartool: Error")) {
			return false;
		}
		baos.close();
		return true;
	}

	@SuppressWarnings("unchecked")
	private List<String> makeBaseline(AbstractBuild build,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws Exception {

		List<String> createdBaselinesList = new ArrayList<String>();

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

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			cmd.add("-view");
			cmd.add(scm.getViewName());
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		// Make baseline only for read/write components (identical or not)
		cmd.add("-comp");
		StringBuffer lstComp = new StringBuffer();
		for (String comp : this.readWriteComponents) {
			lstComp.append(",");
			lstComp.append(comp);
		}
		lstComp.delete(0, 1);
		cmd.add(lstComp.toString());

		cmd.add(baselineName);

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		clearToolLauncher.run(cmd.toCommandArray(), null, baos,
				clearToolLauncherPath);
		baos.close();
		String cleartoolResult = baos.toString();
		if (cleartoolResult.contains("cleartool: Error")) {
			throw new Exception("Failed to make baseline, reason: "
					+ cleartoolResult);
		}

		Pattern pattern = Pattern.compile("Created baseline \".+?\"");
		Matcher matcher = pattern.matcher(cleartoolResult);
		while (matcher.find()) {
			String match = matcher.group();
			String newBaseline = match.substring(match.indexOf("\"") + 1, match
					.length() - 1);
			createdBaselinesList.add(newBaseline);
		}

		return createdBaselinesList;

	}

	private void recommedBaseline(String stream,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws InterruptedException, IOException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		cmd.add("chstream");
		cmd.add("-rec");
		cmd.add("-def");
		cmd.add(stream);

		clearToolLauncher.run(cmd.toCommandArray(), null, null,
				clearToolLauncherPath);
	}

	private void promoteBaselineToBuiltLevel(String stream,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath, String blName) throws InterruptedException,
			IOException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		cmd.add("chbl");
		cmd.add("-c");
		cmd.add("Hudson promoted baseline to BUILT");
		cmd.add("-level");
		cmd.add("BUILT");

		cmd.add(blName);

		clearToolLauncher.run(cmd.toCommandArray(), null, null,
				clearToolLauncherPath);
	}

	private void demoteBaselineToRejectedLevel(String stream,
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath, String blName) throws InterruptedException,
			IOException {

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		cmd.add("chbl");
		cmd.add("-c");
		cmd.add("Hudson demoted baseline to REJECTED");
		cmd.add("-level");
		cmd.add("REJECTED");

		cmd.add(blName);

		clearToolLauncher.run(cmd.toCommandArray(), null, null,
				clearToolLauncherPath);
	}

	/**
	 * Retrieve the read/write component list with PVOB
	 * 
	 * @param clearToolLauncher
	 * @param filePath
	 * @return the read/write component like 'DeskCore@\P_ORC DeskShared@\P_ORC
	 *         build_Product@\P_ORC'
	 * @throws Exception
	 */
	private List<String> getReadWriteComponent(
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws Exception {

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		FilePath clearToolLauncherPath = filePath;

		cmd.add("lsproject");
		if (scm.isUseDynamicView()) {
			cmd.add("-view");
			cmd.add(scm.getViewName());
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		} else {
			cmd.add("-cview");
		}
		cmd.add("-fmt");
		cmd.add("%[mod_comps]Xp");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		clearToolLauncher.run(cmd.toCommandArray(), null, baos,
				clearToolLauncherPath);
		baos.close();
		String cleartoolResult = baos.toString();

		final String prefix = "component:";
		if (cleartoolResult != null && cleartoolResult.startsWith(prefix)) {
			List<String> componentNames = new ArrayList<String>();
			String[] componentNamesSplit = cleartoolResult.split(" ");
			for (String componentName : componentNamesSplit) {
				String componentNameTrimmed = componentName.substring(
						componentName.indexOf(prefix) + prefix.length()).trim();
				if (!componentNameTrimmed.equals("")) {
					componentNames.add(componentNameTrimmed);
				}
			}
			return componentNames;
		}
		throw new Exception("Failed to get read/write component, reason: "
				+ cleartoolResult);
	}

	/**
	 * Get the component binding to the baseline
	 * 
	 * @param clearToolLauncher
	 * @param filePath
	 * @param blName
	 *            the baseline name like
	 *            'deskCore_3.2-146_2008-11-14_18-07-22.3543@\P_ORC'
	 * @return the component name like 'Desk_Core@\P_ORC'
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private String getComponentforBaseline(
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath, String blName) throws InterruptedException,
			IOException {

		ArgumentListBuilder cmd = new ArgumentListBuilder();

		FilePath clearToolLauncherPath = filePath;
		if (scm.isUseDynamicView()) {
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}

		cmd.add("lsbl");
		cmd.add("-fmt");
		cmd.add("%[component]Xp");
		cmd.add(blName);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		clearToolLauncher.run(cmd.toCommandArray(), null, baos,
				clearToolLauncherPath);
		baos.close();
		String cleartoolResult = baos.toString();

		String prefix = "component:";
		return cleartoolResult.substring(cleartoolResult
				.indexOf(cleartoolResult)
				+ prefix.length());
	}

	private List<String> getLatestBaselineNames(
			HudsonClearToolLauncher clearToolLauncher, ClearCaseUcmSCM scm,
			FilePath filePath) throws Exception {

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		FilePath clearToolLauncherPath = filePath;

		cmd.add("lsstream");
		if (scm.isUseDynamicView()) {
			cmd.add("-view");
			cmd.add(scm.getViewName());
			clearToolLauncherPath = clearToolLauncher.getWorkspace();
		}
		cmd.add("-fmt");
		cmd.add("%[latest_bls]Xp");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		clearToolLauncher.run(cmd.toCommandArray(), null, baos,
				clearToolLauncherPath);
		baos.close();
		String cleartoolResult = baos.toString();
		String prefix = "baseline:";
		if (cleartoolResult != null && cleartoolResult.startsWith(prefix)) {
			List<String> baselineNames = new ArrayList<String>();
			String[] baselineNamesSplit = cleartoolResult.split("baseline:");
			for (String baselineName : baselineNamesSplit) {
				String baselineNameTrimmed = baselineName.trim();
				if (!baselineNameTrimmed.equals("")) {
					// Retrict to baseline bind to read/write component
					String blComp = getComponentforBaseline(clearToolLauncher,
							scm, filePath, baselineNameTrimmed);
					if (this.readWriteComponents.contains(blComp))
						baselineNames.add(baselineNameTrimmed);
				}
			}
			return baselineNames;
		}
		throw new Exception("Failed to get baselinename, reason: "
				+ cleartoolResult);

	}

}
