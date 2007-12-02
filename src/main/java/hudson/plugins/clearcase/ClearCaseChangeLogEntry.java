package hudson.plugins.clearcase;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
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
    private String dateStr = null;
    private Date date = null;
    private String comment = null;
    private List<FileElement> files = new ArrayList<FileElement>();

    public ClearCaseChangeLogEntry() {
    }

    public ClearCaseChangeLogEntry(Date date, String user, String action, String comment, String file, String version) {
        this.date = date;
        this.user = user;
        this.comment = comment;
        addElement(new FileElement(file, version, action));
    }

    public void addElement(FileElement element) {
        files.add(element);
    }

    public void addElements(Collection<FileElement> files) {
        this.files.addAll(files);
    }
    
    public List<FileElement> getElements() {
        return files;
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

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Deprecated
    public void setFile(String file) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement(file, "", ""));
        else
            files.get(0).setFile(file);
    }

    @Deprecated
    public void setVersion(String version) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", version, ""));
        else
            files.get(0).setVersion(version);
    }

    @Deprecated
    public void setAction(String action) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", "", action));
        else
            files.get(0).setAction(action);
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new HashSet<String>(files.size());
        for (FileElement file : files) {
            paths.add(file.getFile());
        }
        return paths;
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
    
    public static class FileElement {
        private String name;
        private String version;
        private String action;
        
        public FileElement() {
        }
        
        public FileElement(String fileName, String version, String action) {
            this.name = fileName;
            this.version = version;
            this.action = action;
        }
        
        public String getFile() {
            return name;
        }
        
        public void setFile(String fileName) {
            this.name = fileName;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
