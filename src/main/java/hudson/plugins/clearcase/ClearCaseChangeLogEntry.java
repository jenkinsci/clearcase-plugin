/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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
import hudson.scm.EditType;

import org.kohsuke.stapler.export.ExportedBean;
import org.kohsuke.stapler.export.Exported;

/**
 * ClearCase change log entry.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogEntry extends ChangeLogSet.Entry {

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private String user = "";
    private String dateStr = "";
    private Date date = null;
    private String comment = "";
    private List<FileElement> files = new ArrayList<FileElement>();

    public ClearCaseChangeLogEntry() {
    }

    public ClearCaseChangeLogEntry(Date date, String user, String action, String comment, String file, String version) {
        this(date, user, comment);
        files.add(new FileElement(file, version, action, ""));
    }

    public ClearCaseChangeLogEntry(Date date, String user, String comment) {
        this.date = (Date) date.clone();
        this.user = user;
        this.comment = comment;
    }

    public void addElement(FileElement element) {
        files.add(element);
    }

    public void addElements(Collection<FileElement> files) {
        this.files.addAll(files);
    }
    
    @Exported
    public List<FileElement> getElements() {
        return files;
    }
    
    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Exported
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

    @Exported
    public Date getDate() {
        return (Date) date.clone();
    }

    public void setDate(Date date) {
        this.date = (Date) date.clone();
    }

    @Exported
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Deprecated
    public void setFile(String file) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement(file, "", "", ""));
        else
            files.get(0).setFile(file);
    }

    @Deprecated
    public void setVersion(String version) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", version, "", ""));
        else
            files.get(0).setVersion(version);
    }

    @Deprecated
    public void setAction(String action) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", "", action, ""));
        else
            files.get(0).setAction(action);
    }

    @Override
    @Exported
    public User getAuthor() {
        return User.get(user);
    }

    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<String>(files.size());
        for (FileElement file : files) {
            paths.add(file.getFile());
        }
        return paths;
    }

    @Override
    @Exported
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

    @ExportedBean(defaultVisibility=999)
    public static class FileElement {
        private String name = "";
        private String version = "";
        private String action = "";
        private String operation = "";
        
        public FileElement() {
        }
        
        public FileElement(String fileName, String version, String action, String operation) {
            this.name = fileName;
            this.version = version;
            this.action = action;
            this.operation = operation;
        }
        
        @Exported
        public String getFile() {
            return name;
        }
        
        public void setFile(String fileName) {
            this.name = fileName;
        }

        @Exported
        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Exported
        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        @Exported        
        public String getOperation() {
            return operation;
        }

        public void setOperation(String status) {
            this.operation = status;
        }

        @Exported
        public EditType getEditType() {
            if (operation.equalsIgnoreCase("mkelem")) {
                return EditType.ADD;
            } else if (operation.equalsIgnoreCase("rmelem")) {
                return EditType.DELETE;
            } else if (operation.equalsIgnoreCase("checkin")) {
                return EditType.EDIT;
            }
            return null;
        }
    }
}
