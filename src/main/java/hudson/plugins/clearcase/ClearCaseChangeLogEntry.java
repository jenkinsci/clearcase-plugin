/**
 * The MIT License
 *
 * Copyright (c) 2007-2011, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer,
 *                          Krzysztof Malinowski
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

import hudson.model.User;
import hudson.scm.EditType;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * ClearCase change log entry.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogEntry extends ChangeLogSet.Entry {

    @ExportedBean(defaultVisibility = 999)
    public static class FileElement implements ChangeLogSet.AffectedFile {
        private String action    = "";
        private String name      = "";
        private String operation = "";
        private String version   = "";

        public FileElement() {
        }

        public FileElement(String fileName, String version, String action, String operation) {
            this.name = fileName;
            this.version = version;
            this.action = action;
            this.operation = operation;
        }

        @Exported
        public String getAction() {
            return action;
        }

        @Override
        @Exported
        public EditType getEditType() {
            if (operation.equalsIgnoreCase("mkelem")) {
                return EditType.ADD;
            } else if (operation.equalsIgnoreCase("rmelem")) {
                return EditType.DELETE;
            } else if (operation.equalsIgnoreCase("checkin") || operation.equalsIgnoreCase("mklabel") || operation.equalsIgnoreCase("rmlabel")) {
                return EditType.EDIT;
            }
            return null;
        }

        @Exported
        public String getFile() {
            return name;
        }

        @Exported
        public String getOperation() {
            return operation;
        }

        @Override
        public String getPath() {
            return String.format("%s@@%s", name, version);
        }

        @Exported
        public String getVersion() {
            return version;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public void setFile(String fileName) {
            this.name = fileName;
        }

        public void setOperation(String status) {
            this.operation = status;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    private static final String DATE_FORMAT = "dd/MM/yyyy HH:mm:ss";
    private String              comment     = "";
    private Date                date        = null;
    private String              dateStr     = "";
    private List<FileElement>   files       = new ArrayList<FileElement>();

    private String              user        = "";

    public ClearCaseChangeLogEntry() {
    }

    public ClearCaseChangeLogEntry(Date date, String user, String comment) {
        this.date = (Date) date.clone();
        this.user = user;
        this.comment = comment;
    }

    public ClearCaseChangeLogEntry(Date date, String user, String action, String comment, String file, String version) {
        this(date, user, comment);
        files.add(new FileElement(file, version, action, ""));
    }

    public void addElement(FileElement element) {
        files.add(element);
    }

    public void addElements(Collection<FileElement> files) {
        this.files.addAll(files);
    }

    @Override
    public Collection<? extends AffectedFile> getAffectedFiles() {
        return files;
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
    public User getAuthor() {
        return User.get(user);
    }

    public String getComment() {
        return comment;
    }

    @Exported
    public Date getDate() {
        return (Date) date.clone();
    }

    @Exported
    public String getDateStr() {
        if (date == null) {
            return dateStr;
        } else {
            return new SimpleDateFormat(DATE_FORMAT).format(date);
        }
    }

    @Exported
    public List<FileElement> getElements() {
        return files;
    }

    @Override
    @Exported
    public String getMsg() {
        return comment;
    }

    @Exported
    public String getUser() {
        return user;
    }

    @Deprecated
    public void setAction(String action) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", "", action, ""));
        else
            files.get(0).setAction(action);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setDate(Date date) {
        this.date = (Date) date.clone();
    }

    public void setDateStr(String date) {
        try {
            this.date = new SimpleDateFormat(DATE_FORMAT).parse(date);
        } catch (ParseException e) {
            this.dateStr = date;
        }
    }

    @Deprecated
    public void setFile(String file) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement(file, "", "", ""));
        else
            files.get(0).setFile(file);
    }

    /**
     * Overrides the setParent() method so the ClearCaseChangeLogSet can access it.
     */
    @Override
    public void setParent(@SuppressWarnings("unchecked") ChangeLogSet parent) {
        super.setParent(parent);
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Deprecated
    public void setVersion(String version) {
        if ((files == null) || (files.size() == 0))
            addElement(new FileElement("", version, "", ""));
        else
            files.get(0).setVersion(version);
    }
}
