package hudson.plugins.clearcase;

import hudson.AbortException;
import hudson.FilePath;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOException2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClearToolExec implements ClearTool {

	private Pattern viewListPattern;
	private String clearToolExec;

	public ClearToolExec(String clearToolExec) {
		this.clearToolExec = clearToolExec;
	}

	public void mkview(ClearToolLauncher launcher, String viewName) throws IOException, InterruptedException {
		
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("mkview");
		cmd.add("-snapshot");
		cmd.add("-tag");
		cmd.add(viewName);
		cmd.add(viewName);
		launcher.run(cmd.toCommandArray(), null, null, null);
	}

	public void setcs(ClearToolLauncher launcher, String viewName, String configSpec) throws IOException, InterruptedException {
		FilePath configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);
		
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("setcs");
		cmd.add(".." + File.separatorChar + configSpecFile.getName());
		launcher.run(cmd.toCommandArray(), null, null, viewName);
		
		configSpecFile.delete();
	}
	
	public void rmview(ClearToolLauncher launcher, String viewName) throws IOException, InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("rmview");
		cmd.add("-force");
		cmd.add(viewName);
		launcher.run(cmd.toCommandArray(), null, null, null);
		FilePath viewFilePath = launcher.getWorkspace().child(viewName);
		if (viewFilePath.exists()) {
			launcher.getListener().getLogger().println("Removing view folder as it was not removed when the view was removed.");
			viewFilePath.deleteRecursive();
		}
	}
	
	public void update(ClearToolLauncher launcher, String viewName) throws IOException, InterruptedException {
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("update");
		cmd.add("-force");
		cmd.add("-log", "NUL");
		cmd.add(viewName);		
		launcher.run(cmd.toCommandArray(), null, null, null);
	}

	public List<ClearCaseChangeLogEntry> lshistory(ClearToolLauncher launcher, Date lastBuildDate, String viewName, String branch) throws IOException,
			InterruptedException {
		SimpleDateFormat formatter = new SimpleDateFormat("d-MMM.HH:mm:ss");
		FilePath viewPath = launcher.getWorkspace().child(viewName);
		String[] vobNames = null;
		List<FilePath> subFilePaths = viewPath.list((FileFilter) null);
		if ((subFilePaths != null) && (subFilePaths.size() > 0)) {
			vobNames = new String[subFilePaths.size()];
			for (int i = 0; i < subFilePaths.size(); i++) {
				if (subFilePaths.get(i).isDirectory()) {
					vobNames[i] = subFilePaths.get(i).getName();
				}
			}
		}

		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
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

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (launcher.run(cmd.toCommandArray(), null, baos, viewName)) {
			try {
				ClearToolHistoryParser parser = new ClearToolHistoryParser();
				return parser.parse(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
			} catch (ParseException pe) {
				throw new IOException2("There was a problem parsing the history log.", pe);
			}
		}
		return new ArrayList<ClearCaseChangeLogEntry>();
	}

	public void mklabel(ClearToolLauncher launcher, String viewName, String label) throws IOException, InterruptedException {
		throw new AbortException();
	}

	public List<String> lsview(ClearToolLauncher launcher, boolean onlyActiveDynamicViews) throws IOException, InterruptedException {
		viewListPattern = getListPattern();
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("lsview");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
			return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyActiveDynamicViews);
		}
		return new ArrayList<String>();
	}

	public List<String> lsvob(ClearToolLauncher launcher, boolean onlyMOunted) throws IOException, InterruptedException {
		viewListPattern = getListPattern();
		ArgumentListBuilder cmd = new ArgumentListBuilder();
		cmd.add(clearToolExec);
		cmd.add("lsvob");

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
			return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyMOunted);
		}
		return new ArrayList<String>();
	}
	
	private List<String> parseListOutput(Reader consoleReader, boolean onlyStarMarked) throws IOException {
		List<String> views = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(consoleReader);
		String line = reader.readLine();
		while (line != null) {
			Matcher matcher = viewListPattern.matcher(line);
			if (matcher.find() && matcher.groupCount() == 3) {
				if ((!onlyStarMarked) ||
					(onlyStarMarked && matcher.group(1).equals("*")) ) {
					String vob = matcher.group(2);
					int pos = Math.max(vob.lastIndexOf('\\'), vob.lastIndexOf('/'));
					if (pos != -1) {
						vob = vob.substring(pos + 1);
					}
					views.add(vob);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		return views;
	}

	private Pattern getListPattern() {
		if (viewListPattern == null) {
			viewListPattern = Pattern.compile("(.)\\s*(\\S*)\\s*(\\S*)");
		}
		return viewListPattern;
	}
}
