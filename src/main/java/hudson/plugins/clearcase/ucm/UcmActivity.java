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
package hudson.plugins.clearcase.ucm;

import hudson.model.User;
import hudson.scm.EditType;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * Changelog entry for UCM ClearCase
 * 
 * @author Henrik L. Hansen
 */
public class UcmActivity extends ChangeLogSet.Entry {

    @ExportedBean(defaultVisibility = 999)
    public static class File implements AffectedFile {

        private String comment;
        private Date   date;
        private String event;    // can maybe be dumbed
        private String name;
        private String operation;

        private String version;

        public File() {
            /* empty by design */
        }

        public File(File other) {
            this.date = other.date;
            this.name = other.name;
            this.version = other.version;
            this.operation = other.operation;
            this.event = other.event;
        }

        @Exported
        public String getComment() {
            return comment;
        }

        @Exported
        public Date getDate() {
            return date;
        }

        public String getDateStr() {
            if (date == null) {
                return "";
            } else {
                return DATE_FORMATTER.format(date);
            }
        }

        @Override
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

        @Exported
        public String getEvent() {
            return event;
        }

        @Exported
        public String getName() {
            return name;
        }

        @Exported
        public String getOperation() {
            return operation;
        }

        @Override
        public String getPath() {
            return name;
        }

        @Exported
        public String getShortVersion() {
            return version.substring(version.lastIndexOf("/") + 1);
        }

        @Exported
        public String getVersion() {
            return version;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public void setDateStr(String date) {
            try {
                this.date = DATE_FORMATTER.parse(date);
            } catch (ParseException e) {
                // TODO: error handling
            }
        }

        public void setEvent(String event) {
            this.event = event;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    private static final DateFormat DATE_FORMATTER = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private List<File>              files          = new ArrayList<File>();
    private String                  headline;
    private String                  name;
    private String                  stream;
    private List<UcmActivity>       subActivities  = new ArrayList<UcmActivity>();

    private String                  user;

    public UcmActivity() {
        // empty by design
    }

    /**
     * Copy contructor
     * 
     * @param other
     *            the activity to copy
     */
    public UcmActivity(UcmActivity other) {
        this.name = other.name;
        this.headline = other.headline;
        this.stream = other.stream;
        this.user = other.user;
        this.setParent(other.getParent());

        for (UcmActivity subAct : other.getSubActivities()) {
            UcmActivity child = new UcmActivity(subAct);
            addSubActivity(child);
        }

        for (UcmActivity.File otherFile : other.files) {
            UcmActivity.File child = new UcmActivity.File(otherFile);
            this.files.add(child);
        }
    }

    public void addFile(File file) {
        files.add(file);
    }

    public void addFiles(Collection<File> files) {
        this.files.addAll(files);
    }

    public void addSubActivities(Collection<UcmActivity> activities) {
        this.subActivities.addAll(activities);
    }

    public void addSubActivity(UcmActivity activity) {
        subActivities.add(activity);
    }

    @Override
    public Collection<? extends AffectedFile> getAffectedFiles() {
        return files;
    }

    @Override
    public Collection<String> getAffectedPaths() {
        Collection<String> paths = new ArrayList<String>(files.size());
        for (File file : files) {
            paths.add(file.getName());
        }
        return paths;
    }

    @Override
    public User getAuthor() {
        return User.get(user);
    }

    @Exported
    public List<File> getFiles() {
        return files;
    }

    @Exported
    public String getHeadline() {
        return headline;
    }

    @Override
    public String getMsg() {
        return headline;
    }

    @Exported
    public String getName() {
        return name;
    }

    @Exported
    public String getStream() {
        return stream;
    }

    @Exported
    public List<UcmActivity> getSubActivities() {
        return subActivities;
    }

    @Exported
    public String getUser() {
        return user;
    }

    @Exported
    public boolean hasFiles() {
        return files.size() > 0;
    }

    @Exported
    public boolean hasSubActivities() {
        return subActivities.size() > 0;
    }

    @Exported
    public boolean isIntegrationActivity() {
        return name.startsWith("deliver.");
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Overrides the setParent() method so the ClearCaseChangeLogSet can access it.
     */
    @Override
    public void setParent(@SuppressWarnings("unchecked") ChangeLogSet parent) {
        super.setParent(parent);
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return name + ": " + headline;
    }
}
