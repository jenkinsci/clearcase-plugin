package hudson.plugins.clearcase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

/**
 * Clear case change log entry.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogEntry extends ChangeLogSet.Entry {

	private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"); 
	
	private String user = null;
	private String action = null;
	private String dateStr = null;
	private Date date = null;
	private String comment = null;
	private List<String> files = null;
	private String version = null;

	public ClearCaseChangeLogEntry() {
	}
	
	public ClearCaseChangeLogEntry(Date date, String user, String action, String comment, String file, String version) {
		this.date = date;
		this.user = user;
		this.action = action;
		this.comment = comment;
		this.version = version;
		addFile(file);
	}

	public void addFile(String file) {
		if (files == null) {
			files = new ArrayList<String>();
		}
		files.add(file);
	}
	public void addFiles(Collection<String> files) {
		if (files == null) {
			files = new ArrayList<String>();
		}
		this.files.addAll(files);
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDateStr() {
		if (date == null) {
			return dateStr;
		} else {
			return DATE_FORMATTER.format(date);
		}
	}

	public void setDateStr(String date) {
		try {
			this.date = DATE_FORMATTER.parse(date);
		} catch (ParseException e) {
			this.dateStr = date;
		}
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getFile() {
		if ((files == null) || (files.size() == 0))
			return "";
		else
			return files.get(0);
	}

	public void setFile(String file) {
		addFile(file);
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public User getAuthor() {
		return User.get(user);
	}

	@Override
	public Collection<String> getAffectedPaths() {
		return files;
	}

	@Override
	public String getMsg() {
		return comment;
	}

	/**
	 * Overrides the setParent() method so the ClearCaseChangeLogSet can access it.
	 */
	@Override
	public void setParent(ChangeLogSet parent) {
		super.setParent(parent);
	}
}
