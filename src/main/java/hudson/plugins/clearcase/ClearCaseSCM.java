package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Proc;
import static hudson.Util.fixEmpty;
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
import hudson.util.IOException2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	private boolean useUpdate;
	private String configSpec;
	private String viewName;

	public ClearCaseSCM(String branch, String configSpec, String viewName, boolean useUpdate) {
		this.branch = branch;
		this.configSpec = configSpec;
		this.viewName = viewName;
		this.useUpdate = useUpdate;
	}

	// Get methods
	public String getBranch() {
		return branch;
	}
	public String getConfigSpec() {
		return configSpec;
	}
	public String getViewName() {
		if (viewName == null) {
			return "hudson_view";
		} else {
			return viewName;
		}
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
		
		boolean localViewPathExists = new FilePath(workspace, viewName).exists();
		
		if ((! useUpdate) && localViewPathExists) {
			removeView(launcher, workspace, listener, viewName);
			localViewPathExists = false;
		}
		
		if (! localViewPathExists) {
			createView(launcher, workspace, listener, "HUDSON", viewName);
			editConfigSpec(launcher, workspace, listener, viewName);
		}

		if (useUpdate) {
			updateViewPath(launcher, workspace, listener, viewName);                
		}
		
		if (build.getPreviousBuild() != null) {
			history.addAll(getHistoryEntries(build.getPreviousBuild().getTimestamp().getTime(), 
				launcher, workspace, listener, viewName));
		}

		if (history.isEmpty()) {
			// nothing to compare against, or no changes
			return createEmptyChangeLog(changelogFile, listener, "changelog");
		} else {
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
			return !getHistoryEntries(buildTime, launcher, workspace, listener, viewName).isEmpty();
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
			listener.getLogger().println("Removing view folder as it was not removed when the view was removed.");
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
	 * @param viewName the name of the view
	 * @return array of objects containing history entries
	 * @throws IOException thrown if there was a problem reading from the output from the tool
	 * @throws InterruptedException
	 */
	private List<Object[]> getHistoryEntries(Date lastBuildDate, Launcher launcher, FilePath workspace,
			TaskListener listener, String viewName) throws IOException, InterruptedException {
		
		SimpleDateFormat formatter = new SimpleDateFormat("d-MMM.HH:mm:ss");
		FilePath viewPath = workspace.child(viewName);
		String[] vobNames = null;
		List<FilePath> subFilePaths = viewPath.list((FileFilter) null);
		if ((subFilePaths != null) && (subFilePaths.size() > 0)) {
			vobNames = new String[subFilePaths.size()];
			for (int i = 0; i < subFilePaths.size(); i++) {
				vobNames[i] = subFilePaths.get(i).getName();
			}
		}
		
		List<Object[]> historyEntries = new ArrayList<Object[]>();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(getDescriptor().getCleartoolExe());
		cmd.add("lshistory");
		cmd.add("-r");
		cmd.add("-since", formatter.format(lastBuildDate));
		cmd.add("-fmt", ClearToolHistoryParser.getLogFormat());
		if ((branch != null) && (branch.length() > 0)) {
			cmd.add("-branch", branch);
		}
		cmd.add("-nco");
		if (vobNames != null) {
			cmd.add(vobNames);
		}
		if (run(launcher, cmd, listener, viewPath, new ForkOutputStream(baos, listener.getLogger()))) {
			ClearToolHistoryParser parser = new ClearToolHistoryParser();
			try {
				parser.parse(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), historyEntries);
			} catch (ParseException pe) {
				throw new IOException2("There was a problem parsing the history log.", pe);
			}
		}
		baos.close();
		return historyEntries;
	}

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
					req.getParameter("clearcase.configspec"),
					req.getParameter("clearcase.viewname"),
					req.getParameter("clearcase.useupdate") != null);
		}
		
        /**
         * Checks if clear tool executable exists.
         */
        public void doCleartoolExeCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator.Executable(req,rsp).process();
        }
        
        public void doViewNameCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if(v==null) {
                        error("View name is mandatory");
                        return;
                    }
                    // all tests passed so far
                    ok();
                }
            }.process();
        }
        
        public void doConfigSpecCheck(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        	System.out.println("doConfigSpecCheck");
            new FormFieldValidator(req,rsp,false) {
                protected void check() throws IOException, ServletException {
                    String v = fixEmpty(request.getParameter("value"));
                    if ((v==null) || (v.length() == 0)) {
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
