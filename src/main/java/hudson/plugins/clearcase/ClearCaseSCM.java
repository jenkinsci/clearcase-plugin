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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Hudson;
import hudson.model.ModelObject;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.ArgumentListBuilder;
import hudson.util.ByteBuffer;
import hudson.util.ForkOutputStream;
import hudson.util.FormFieldValidator;

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
	private String viewPaths;
	
	private boolean useUpdate;
	private String configSpec;
	private String localDirectory;

	public ClearCaseSCM(String branch, String viewPaths, String configSpec, String localDirectory, boolean useUpdate) {
		this.branch = branch;
		this.viewPaths = viewPaths;
		this.configSpec = configSpec;
		this.localDirectory = localDirectory;
		this.useUpdate = useUpdate;
	}

	// Get methods
	public String getBranch() {
		return branch;
	}

	public String getViewPaths() {
		return viewPaths;
	}

	public String getConfigSpec() {
		return configSpec;
	}
	public String getLocalDirectory() {
		return localDirectory;
	}
	public boolean isUseUpdate() {
		return useUpdate;
	}
	
	@Override
	public ClearCaseScmDescriptor getDescriptor() {
		return DESCRIPTOR;
	}

	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
			File changelogFile) throws IOException, InterruptedException {
		List<Object[]> history = new ArrayList<Object[]>();
		
		boolean localViewPathExists = new FilePath(workspace, localDirectory).exists();
		
		if ((! useUpdate) && localViewPathExists) {
			removeView(launcher, workspace, listener, localDirectory);
			localViewPathExists = false;
		}
		
		if (! localViewPathExists) {
			createView(launcher, workspace, listener, "HUDSON", localDirectory);
			editConfigSpec(launcher, workspace, listener, localDirectory);
		}

		if (useUpdate) {
			updateViewPath(launcher, workspace, listener, localDirectory);                
		}
		
		if (build.getPreviousBuild() != null) {
			history.addAll(getHistoryEntries(build.getPreviousBuild().getTimestamp().getTime(), 
				launcher, workspace, listener, new String[] { localDirectory }));
		}

		if (history.isEmpty()) {
			// nothing to compare against, or no changes
			return createEmptyChangeLog(changelogFile, listener, "changelog");
		} else {
			ClearCaseChangeLogSet.saveToChangeLog(new FileOutputStream(changelogFile), history);
			return true;
		}
	}
	
	/*public boolean checkoutOtherViewPaths(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener,
			File changelogFile) throws IOException, InterruptedException {
		List<Object[]> history = new ArrayList<Object[]>();
		
		for (String viewPath : getAllViewPathsNormalized()) {
			if (isSnapshot(viewPath, launcher, workspace, listener)) {
				listener.getLogger().println(viewPath + " is a snapshot, updating view.");				
				updateViewPath(launcher, workspace, listener, viewPath);
			} else {
				listener.getLogger().println(viewPath + " is not a snapshot, no need to update view.");
			}
			
			if (build.getPreviousBuild() != null) {
				history.addAll(getHistoryEntries(build.getPreviousBuild().getTimestamp().getTime(), 
					launcher, workspace, listener, new String[] { viewPath }));
			}
		}

		if (history.isEmpty()) {
			// nothing to compare against, or no changes
			return createEmptyChangeLog(changelogFile, listener, "changelog");
		} else {
			
			Collections.sort(history, new Comparator<Object[]>() {
				public int compare(Object[] arg0, Object[] arg1) {
					return ((Date) arg1[ClearToolHistoryParser.DATE_INDEX]).compareTo(
							((Date) arg0[ClearToolHistoryParser.DATE_INDEX]));
				}				
			});
			
			ClearCaseChangeLogSet.saveToChangeLog(new FileOutputStream(changelogFile), history);
			return true;
		}
	}*/

	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener)
			throws IOException, InterruptedException {

		Build lastBuild = (Build) project.getLastBuild();
		if (lastBuild == null) {
			return true;
		} else {
			Date buildTime = lastBuild.getTimestamp().getTime();
			return !getHistoryEntries(buildTime, launcher, workspace, listener, new String[]{localDirectory}).isEmpty();
		}
	}

	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ClearCaseChangeLogParser();
	}

	/**
	 * Updates the view path
	 * @param launcher
	 * @param workspace
	 * @param listener
	 * @param viewPath
	 * @throws IOException 
	 * @throws InterruptedException
	 */
	private void updateViewPath(Launcher launcher, FilePath workspace, BuildListener listener, String viewPath)
			throws IOException, InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("update");
		cmd.add("-force");
		cmd.add("-log", "NUL");
		cmd.add(viewPath);		
		run(launcher, cmd, listener, workspace, listener.getLogger());
	}

	private void createView(Launcher launcher, FilePath workspace, BuildListener listener, String viewTag, String viewPath)
			throws IOException, InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("mkview");
		cmd.add("-snapshot");
		cmd.add(viewPath);
		run(launcher, cmd, listener, workspace, listener.getLogger());
	}

	private void removeView(Launcher launcher, FilePath workspace, BuildListener listener, String viewPath)
			throws IOException, InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("rmview");
		cmd.add("-force");
		cmd.add(viewPath);
		run(launcher, cmd, listener, workspace, listener.getLogger());
		FilePath viewFilePath = new FilePath(workspace, viewPath);
		if (viewFilePath.exists()) {
			viewFilePath.deleteRecursive();
		}
	}

	private void editConfigSpec(Launcher launcher, FilePath workspace, BuildListener listener, String viewPath)
			throws IOException, InterruptedException {
		FilePath configSpecFile = workspace.createTextTempFile("configspec", ".txt", configSpec);
		
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("setcs");
		cmd.add(".." + File.separatorChar + configSpecFile.getName());
		run(launcher, cmd, listener, new FilePath(workspace, viewPath), listener.getLogger());
		
		configSpecFile.delete();
	}

	/**
	 * Returns the latest history for the specified module paths.
	 * @param lastBuildDate the last time build date
	 * @param launcher 
	 * @param workspace
	 * @param listener
	 * @return array of objects containing history entries
	 * @throws IOException thrown if there was a problem reading from the output from the tool
	 * @throws InterruptedException
	 */
	private List<Object[]> getHistoryEntries(Date lastBuildDate, Launcher launcher, FilePath workspace,
			TaskListener listener, String[] viewPaths) throws IOException, InterruptedException {
		SimpleDateFormat formatter = new SimpleDateFormat("d-MMM.HH:mm:ss");

		List<Object[]> historyEntries = new ArrayList<Object[]>();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("lshistory");
		cmd.add("-since", formatter.format(lastBuildDate));
		cmd.add("-fmt", ClearToolHistoryParser.getLogFormat());
		if ((branch != null) && (branch.length() > 0)) {
			cmd.add("-branch", branch);
		}
		cmd.add("-nco");
		cmd.add("-avobs");
		if (run(launcher, cmd, listener, new FilePath(workspace, viewPaths[0]), new ForkOutputStream(baos, listener.getLogger()))) {
			ClearToolHistoryParser parser = new ClearToolHistoryParser();
			try {
				parser.parse(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), historyEntries);
			} catch (ParseException pe) {
				throw new IOException("There was a problem parsing the history log.", pe);
			}
		}
		baos.close();
		return historyEntries;
	}

	/**
	 * Returns if the view path is a snapshot view or not
	 * @param viewPath view path
	 * @param launcher
	 * @param workspace
	 * @param listener
	 * @return if the view path is a snapshot view or not
	 * @throws IOException
	 * @throws InterruptedException
	 */
	private boolean isSnapshot(String viewPath, Launcher launcher, FilePath workspace, TaskListener listener)
		throws IOException, InterruptedException {
		boolean isSnapshot = false;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("lsview");
		cmd.add("-cview");
		cmd.add("-properties");
		cmd.add("-full");

		if (run(launcher, cmd, listener, new FilePath(new File(viewPath)), new ForkOutputStream(baos, listener.getLogger()))) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
			String line = reader.readLine();
			while ((line != null) && (!isSnapshot)){
				if (line.contains("Properties: snapshot")) {
					isSnapshot = true;
				}
				line = reader.readLine();
			}
		}
		baos.close();
		return isSnapshot;
	}

/*	private String[] getAllViewPathsNormalized() {
		// split by whitespace, except "\ "
		String[] r = viewPaths.split("(?<!\\\\)[ \\r\\n]+");
		// now replace "\ " to " ".
		for (int i = 0; i < r.length; i++)
			r[i] = r[i].replaceAll("\\\\ ", " ");
		return r;
	}
*/
	private final boolean run(Launcher launcher, ArgumentListBuilder cmd, TaskListener listener, FilePath dir,
			OutputStream out) throws IOException, InterruptedException {
		Map<String, String> env = new HashMap<String, String>();
		int r = launcher.launch(cmd.toCommandArray(), env, out, dir).join();
		if (r != 0) {
			StringBuilder builder = new StringBuilder();
			for (String cmdParam : cmd.toList()) {
				if (builder.length() > 0) {
					builder.append(" ");
				}
				builder.append(cmdParam);
			}
			listener.fatalError(getDescriptor().getDisplayName() + " failed. exit code=" + r);
			throw new IOException("Clear tool did not return the expected exit code. Command line=\"" + 
						 builder.toString() + "\", actual exit code=" + r);
		}
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
			return new ClearCaseSCM(req.getParameter("clearcase.branch"), 
					req.getParameter("clearcase.viewpaths"),
					req.getParameter("clearcase.configspec"),
					req.getParameter("clearcase.localdirectory"),
					req.getParameter("clearcase.useupdate") != null);
		}
		
        /**
         * Checks if clear tool executable exists.
         */
        public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req,rsp).process();
        }
        public void doLocalDirectoryCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if(v==null) {
                        error("Local directory is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }
        public void doConfigSpecCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if(v==null) {
                        error("Config spec is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }
		
        /**
         * Displays "cleartool -version" for trouble shooting.
         */
        public void doVersion(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, InterruptedException {
            ByteBuffer baos = new ByteBuffer();
            try {
                Proc proc = Hudson.getInstance().createLauncher(TaskListener.NULL).launch(
                    new String[]{getCleartoolExe(), "-version"}, new String[0], baos, null);
                proc.join();
                rsp.setContentType("text/plain");
                baos.writeTo(rsp.getOutputStream());
            } catch (IOException e) {
                req.setAttribute("error",e);
                rsp.forward(this,"versionCheckError",req);
            }
        }
	}
}
