package hudson.plugins.clearcase;

import java.util.Collection;
import java.util.Collections;

import hudson.model.User;
import hudson.scm.ChangeLogSet;

/**
 * Clear case change log entry.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogEntry extends ChangeLogSet.Entry {

	private String user = null;
	private String action = null;
	private String date = null;
	private String comment = null;
	private String file = null;
	private String version = null;

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

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
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
		return Collections.singletonList(file);
	}

	@Override
	public String getMsg() {
		return comment;
	}
}
