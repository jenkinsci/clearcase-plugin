package hudson.plugins.clearcase.ucm;

import static hudson.plugins.clearcase.util.OutputFormat.*;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.HistoryEntry;
import hudson.plugins.clearcase.util.ClearToolFormatHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * 
 * @author Henrik L. Hansen
 */
public class UcmChangeLogAction implements ChangeLogAction {

	// full lshistory output and parsing
	private static final String[] HISTORY_FORMAT = { DATE_NUMERIC,
			NAME_ELEMENTNAME, NAME_VERSIONID, UCM_VERSION_ACTIVITY, EVENT,
			OPERATION, USER_ID };
	private static final String[] ACTIVITY_FORMAT = { UCM_ACTIVITY_HEADLINE,
			UCM_ACTIVITY_STREAM, USER_ID, };
	private static final String[] INTEGRATION_ACTIVITY_FORMAT = {
			UCM_ACTIVITY_HEADLINE, UCM_ACTIVITY_STREAM, USER_ID,
			UCM_ACTIVITY_CONTRIBUTING };

	private ClearTool cleartool;

	private ClearToolFormatHandler historyHandler = new ClearToolFormatHandler(
			HISTORY_FORMAT);
	private SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"yyyyMMdd.HHmmss");
	private Map<String, UcmActivity> activityNameToEntry = new HashMap<String, UcmActivity>();
	private List<Filter> filters;

	/**
	 * Extended view path that should be removed file paths in entries.
	 */
	private String extendedViewPath;

	public UcmChangeLogAction(ClearTool cleartool, List<Filter> filters) {
		this.cleartool = cleartool;
		this.filters = filters;
		if (this.filters == null) {
			this.filters = new ArrayList<Filter>();
		}
	}

	@Override
	public List<UcmActivity> getChanges(Date time, String viewName,
			String[] branchNames, String[] viewPaths) throws IOException,
			InterruptedException {
		// ISSUE-3097
		// Patched since this command must allow paths that do not contain
		// the specified branch name (happens, for instance, if changes has
		// been made only in one of several vobs
		List<UcmActivity> history = new ArrayList<UcmActivity>();
		boolean ok = false;
		IOException exception = null;
		for (String path : viewPaths) {
			try {
				// Added the view name as part of the path (as the
				// currentdirectory
				// is the workspace root and the view will be checked out in a
				// directory with the name of the view)
				String fullpath = viewName + File.separator + path;
				BufferedReader reader = new BufferedReader(cleartool.lshistory(
						historyHandler.getFormat() + COMMENT + LINEEND, time,
						viewName, branchNames[0], new String[] { fullpath }));
				history.addAll(parseHistory(reader, viewName));
				reader.close();
				ok = true; // At least one path was successful
			} catch (IOException e) {
				exception = e;
			}
		}
		if (ok || exception == null) {
			return history;
		} else {
			// No path was successful, throwing the last one
			throw exception;
		}
	}

	private List<UcmActivity> parseHistory(BufferedReader reader,
			String viewname) throws InterruptedException, IOException {
		List<UcmActivity> result = new ArrayList<UcmActivity>();
		try {
			StringBuilder commentBuilder = new StringBuilder();
			String line = reader.readLine();

			UcmActivity.File currentFile = null;
			outer: while (line != null) {

				// TODO: better error handling
				if (line.startsWith("cleartool: Error:")) {
					line = reader.readLine();
					continue;
				}
				Matcher matcher = historyHandler.checkLine(line);

				// finder find start of lshistory entry
				if (matcher != null) {

					if (currentFile != null) {
						currentFile.setComment(commentBuilder.toString());
					}
					commentBuilder = new StringBuilder();
					currentFile = new UcmActivity.File();

					// read values;
					currentFile.setDate(dateFormatter.parse(matcher.group(1)));

					String fileName = matcher.group(2).trim();
					if (extendedViewPath != null) {
						if (fileName.toLowerCase().startsWith(extendedViewPath)) {
							fileName = fileName.substring(extendedViewPath
									.length());
						}
					}

					currentFile.setName(fileName);
					currentFile.setVersion(matcher.group(3));
					currentFile.setEvent(matcher.group(5));
					currentFile.setOperation(matcher.group(6));

					HistoryEntry historyEntry = new HistoryEntry();
					historyEntry.setLine(line);
					historyEntry.setDateText(matcher.group(1).trim());
					historyEntry.setElement(matcher.group(2).trim());
					historyEntry.setVersionId(matcher.group(3).trim());
					historyEntry.setActivityName(matcher.group(4).trim());
					historyEntry.setEvent(matcher.group(5).trim());
					historyEntry.setOperation(matcher.group(6).trim());
					historyEntry.setUser(matcher.group(7).trim());

					for (Filter filter : filters) {
						if (!filter.accept(historyEntry)) {
							line = reader.readLine();
							continue outer;
						}
					}

					String activityName = matcher.group(4);

					UcmActivity activity = activityNameToEntry
							.get(activityName);
					if (activity == null) {
						activity = new UcmActivity();
						activity.setName(activityName);
						if (activityName.length() != 0) {
							callLsActivity(activity, viewname, 1);
						} else {
							activity.setHeadline("Unknown activity");
							activity.setUser("Unknown");
							activity.setStream("");
						}
						activityNameToEntry.put(activityName, activity);
						result.add(activity);
					}

					activity.addFile(currentFile);
				} else {
					if (commentBuilder.length() > 0) {
						commentBuilder.append("\n");
					}
					commentBuilder.append(line);
				}
				line = reader.readLine();
			}
			if (currentFile != null) {
				currentFile.setComment(commentBuilder.toString());
			}
		} catch (ParseException ex) {
			IOException ioe = new IOException(
					"Could not parse cleartool output");
			ioe.setStackTrace(ex.getStackTrace());
			throw ioe;
		}
		return result;
	}

	private void callLsActivity(UcmActivity activity, String viewname,
			int numberOfContributingActivitiesToFollow) throws IOException,
			InterruptedException {
		ClearToolFormatHandler handler = null;
		if (activity.isIntegrationActivity()) {
			handler = new ClearToolFormatHandler(INTEGRATION_ACTIVITY_FORMAT);
		} else {
			handler = new ClearToolFormatHandler(ACTIVITY_FORMAT);
		}

		BufferedReader reader = new BufferedReader(cleartool.lsactivity(
				activity.getName(), handler.getFormat(), viewname));

		String line = reader.readLine();
		Matcher matcher = handler.checkLine(line);
		if (matcher != null) {
			activity.setHeadline(matcher.group(1));
			activity.setStream(matcher.group(2));
			activity.setUser(matcher.group(3));

			if (activity.isIntegrationActivity()
					&& numberOfContributingActivitiesToFollow > 0) {

				String contributingActivities = matcher.group(4);

				for (String contributing : contributingActivities.split(" ")) {

					UcmActivity subActivity = null;
					UcmActivity cachedActivity = activityNameToEntry
							.get(contributing);

					if (cachedActivity == null) {
						subActivity = new UcmActivity();
						subActivity.setName(contributing);
						callLsActivity(subActivity, viewname,
								--numberOfContributingActivitiesToFollow);
						activityNameToEntry.put(contributing, subActivity);
					} else {
						/* do deep copy */
						subActivity = new UcmActivity(cachedActivity);
					}
					activity.addSubActivity(subActivity);
				}
			}
		}

		reader.close();
	}

	/**
	 * Sets the extended view path. The extended view path will be removed from
	 * file paths in the event. The extended view path is for example the view
	 * root + view name; and this path shows up in the history and can be
	 * conusing for users.
	 * 
	 * @param path
	 *            the new extended view path.
	 */
	public void setExtendedViewPath(String path) {
		if (path != null) {
			this.extendedViewPath = path.toLowerCase();
		} else {
			this.extendedViewPath = null;
		}
	}

	public String getExtendedViewPath() {
		return extendedViewPath;
	}
}
