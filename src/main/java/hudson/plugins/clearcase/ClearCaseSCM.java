package hudson.plugins.clearcase;

import static hudson.Util.fixEmpty;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.kohsuke.stapler.StaplerRequest;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ForkOutputStream;

/**
 * Clear case SCM.
 * 
 * This SCM uses the cleartool to update and get the change log.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseSCM extends SCM {

	public static final ClearCaseSCM.ClearCaseScmDescriptor DESCRIPTOR = new ClearCaseSCM.ClearCaseScmDescriptor();

	private String branch;
	private String modules;
	private boolean isSnapshot;

	public ClearCaseSCM(String branch, String modules, boolean isSnapshot) {
		this.branch = branch;
		this.modules = modules;
		this.isSnapshot = isSnapshot;
	}

	// Get methods
	public String getBranch() {
		return branch;
	}

	public String getAllModules() {
		return modules;
	}

	public boolean getIsSnapshot() {
		return isSnapshot;
	}

	@Override
	public ClearCaseScmDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	// TODO untested
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
			File changelogFile) throws IOException, InterruptedException {

		for (String modulePath : getAllModulesNormalized()) {
			if (isSnapshot(modulePath, launcher, workspace, listener)) {
				ArgumentListBuilder cmd = new ArgumentListBuilder();
				cmd.add(getDescriptor().getCleartoolExe());
				cmd.add("update");
				cmd.add("-force");
				cmd.add(modulePath);
		
				// TODO check value from run
				run(launcher, cmd, listener, workspace, listener.getLogger());
			}
		}

		if (build.getPreviousBuild() == null) {
			// nothing to compare against, or no changes
			return createEmptyChangeLog(changelogFile, listener, "changelog");
		} else {
			List<Object[]> history = getHistoryEntries(build.getPreviousBuild().getTimestamp().getTime(), launcher,
					workspace, listener);
			ClearCaseChangeLogSet.saveToChangeLog(new FileOutputStream(changelogFile), history);
			return true;
		}
	}

	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener)
			throws IOException, InterruptedException {

		Build lastBuild = (Build) project.getLastBuild();
		if (lastBuild == null) {
			return true;
		} else {
			Date buildTime = lastBuild.getTimestamp().getTime();
			return !getHistoryEntries(buildTime, launcher, workspace, listener).isEmpty();
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ClearCaseChangeLogParser();
	}

	private List<Object[]> getHistoryEntries(Date startDate, Launcher launcher, FilePath workspace,
			TaskListener listener) throws IOException, InterruptedException {
		try {
			SimpleDateFormat formatter = new SimpleDateFormat("d-MMM.HH:mm");

			List<Object[]> historyEntries = new ArrayList<Object[]>();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			ArgumentListBuilder cmd = new ArgumentListBuilder();
			cmd.add(getDescriptor().getCleartoolExe());
			cmd.add("lshistory");
			cmd.add("-since", formatter.format(startDate));
			cmd.add("-branch", branch);
			cmd.add("-recurse");
			cmd.add("-nco");
			cmd.add(getAllModulesNormalized());

			if (run(launcher, cmd, listener, workspace, new ForkOutputStream(baos, listener.getLogger()))) {
				ClearToolHistoryParser parser = new ClearToolHistoryParser();
				parser.parse(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), historyEntries);
			}

			return historyEntries;
		} catch (RuntimeException error) {
			// Some COM error.
			throw new IOException(error.getMessage());
		}
	}

	// TODO untested
	private boolean isSnapshot(String modulePath, Launcher launcher, FilePath workspace, TaskListener listener)
		throws IOException, InterruptedException {
		boolean isSnapshot = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("lsview");
		cmd.add("-cview");
		cmd.add("-properties");
		cmd.add("-full");
		cmd.add(modulePath);

		if (run(launcher, cmd, listener, workspace, baos)) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
			String line = reader.readLine();
			while ((line != null) && (!isSnapshot)){
				if (line.contains("Properties: snapshot")) {
					isSnapshot = true;
				}
				line = reader.readLine();
			}
		}

		return isSnapshot;
	}

	private String[] getAllModulesNormalized() {
		// split by whitespace, except "\ "
		String[] r = modules.split("(?<!\\\\)[ \\r\\n]+");
		// now replace "\ " to " ".
		for (int i = 0; i < r.length; i++)
			r[i] = r[i].replaceAll("\\\\ ", " ");
		return r;
	}

	private final boolean run(Launcher launcher, ArgumentListBuilder cmd, TaskListener listener, FilePath dir,
			OutputStream out) throws IOException, InterruptedException {
		Map<String, String> env = new HashMap<String, String>();
		int r = launcher.launch(cmd.toCommandArray(), env, out, dir).join();
		if (r != 0)
			listener.fatalError(getDescriptor().getDisplayName() + " failed. exit code=" + r);
		return r == 0;
	}

	/**
	 * Clear case SCM descriptor
	 * 
	 * @author Erik Ramfelt
	 */
	public static final class ClearCaseScmDescriptor extends SCMDescriptor<ClearCaseSCM> implements ModelObject {
		private String cleartoolExe;

		protected ClearCaseScmDescriptor() {
			super(ClearCaseSCM.class, null);
			load();
		}

		public String getCleartoolExe() {
			if (cleartoolExe == null) {
				return "cleartool";
			} else {
				return cleartoolExe;
			}
		}

		@Override
		public String getDisplayName() {
			return "Clear Case";
		}

		@Override
		public boolean configure(StaplerRequest req) {
			cleartoolExe = fixEmpty(req.getParameter("clearcase.cleartoolExe").trim());
			save();
			return true;
		}

		@Override
		public SCM newInstance(StaplerRequest req) throws FormException {
			return new ClearCaseSCM(req.getParameter("clearcase.branch"), req.getParameter("clearcase.module"), true);
		}
	}
}
