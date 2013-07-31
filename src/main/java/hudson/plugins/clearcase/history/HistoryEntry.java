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
package hudson.plugins.clearcase.history;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

/**
 * @author Henrik L. Hansen (henrik.lynggaard@gmail.com)
 */
public class HistoryEntry {

    Date date;
    String dateText;
    String element;
    String versionId;
    String event;
    String user;
    String operation;
    String activityName;
    StringBuilder commentBuilder = new StringBuilder();
    String activityHeadline;
    String line;

    public String getComment() {
        return StringUtils.chomp(commentBuilder.toString());
    }
    
    public HistoryEntry appendComment(String commentFragment) {
        commentBuilder.append(commentFragment);
        return this;
    }

    public String getActivityHeadline() {
        return activityHeadline;
    }

    public void setActivityHeadline(String activityHeadline) {
        this.activityHeadline = activityHeadline;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd.HHmmss");
        date = format.parse(dateText);
        this.dateText = dateText;
    }

    public String getElement() {
        return element;
    }

    public void setElement(String element) {
        this.element = element;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public Date getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof HistoryEntry)) {
            return false;
        }
        final HistoryEntry other = (HistoryEntry) obj;
        if ((this.line == null) ? (other.line != null) : !this.line.equals(other.line)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 17 * hash + (this.line != null ? this.line.hashCode() : 0);
        return hash;
    }

    public HistoryEntry normalize(String viewPath) {
        element = StringUtils.removeStart(element, viewPath);
        return this;
    }

    @Override public String toString() {
        return "HistoryEntry{" + "date=" + date + ", dateText=" + dateText + ", element=" + element + ", versionId=" + versionId + ", event=" + event + ", user=" + user + ", operation=" + operation + ", activityName=" + activityName + ", commentBuilder=" + commentBuilder + ", activityHeadline=" + activityHeadline + ", line=" + line + '}';
    }

}
