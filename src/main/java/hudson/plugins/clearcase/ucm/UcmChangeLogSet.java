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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.kohsuke.stapler.export.Exported;

import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseChangeLogSet;
import hudson.scm.ChangeLogSet;

/**
 * UCM ClearCase change log set.
 * 
 * @author Henrik L. Hansen
 */
public class UcmChangeLogSet extends ChangeLogSet<UcmActivity> {

    static final String[] ACTIVITY_TAGS = new String[]{"name", "headline", "stream", "user"};
    static final String[] FILE_TAGS = new String[]{"name", "date", "comment", "version", "event", "operation"};
    private List<UcmActivity> history = null;

    public UcmChangeLogSet(AbstractBuild<?, ?> build, List<UcmActivity> logs) {
        super(build);
        for (UcmActivity entry : logs) {
            entry.setParent(this);
        }
        this.history = Collections.unmodifiableList(logs);
    }

    @Override
    public boolean isEmptySet() {
        return history.size() == 0;
    }

    public Iterator<UcmActivity> iterator() {
        return history.iterator();
    }

    @Exported
    public List<UcmActivity> getLogs() {
        return history;
    }

    /**
     * Stores the history objects to the output stream as xml
     * 
     * @param outputStream the stream to write to
     * @param history the history objects to store
     * @throws IOException
     */
    public static void saveToChangeLog(OutputStream outputStream, List<UcmActivity> history)
            throws IOException {
        PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<history>");
        for (UcmActivity entry : history) {
            stream.println("\t<entry>");
            String[] activityValues = getEntryAsStrings(entry);
            for (int tag = 0; tag < ACTIVITY_TAGS.length; tag++) {
                stream.print("\t\t<");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.print('>');
                stream.print(ClearCaseChangeLogSet.escapeForXml(activityValues[tag]));
                stream.print("</");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.println('>');
            }
            for (UcmActivity subActivity : entry.getSubActivities()) {
                writeSubActivity(stream,subActivity);
            }            
            for (UcmActivity.File file : entry.getFiles()) {
                stream.println("\t\t<file>");
                String[] fileValues = getFileAsStrings(file);
                for (int tag = 0; tag < FILE_TAGS.length; tag++) {
                    stream.print("\t\t\t<");
                    stream.print(UcmChangeLogSet.FILE_TAGS[tag]);
                    stream.print('>');
                    stream.print(ClearCaseChangeLogSet.escapeForXml(fileValues[tag]));
                    stream.print("</");
                    stream.print(UcmChangeLogSet.FILE_TAGS[tag]);
                    stream.println('>');

                }
                stream.println("\t\t</file>");
            }
            stream.println("\t</entry>");
        }
        stream.println("</history>");
        stream.close();
    }

    private static String[] getEntryAsStrings(UcmActivity entry) {
        String[] array = new String[ACTIVITY_TAGS.length];
        array[0] = entry.getName();
        array[1] = entry.getHeadline();
        array[2] = entry.getStream();
        array[3] = entry.getUser();
        return array;
    }

    private static String[] getFileAsStrings(UcmActivity.File entry) {
        String[] array = new String[FILE_TAGS.length];
        array[0] = entry.getName();
        array[1] = entry.getDateStr();
        array[2] = entry.getComment();
        array[3] = entry.getVersion();
        array[4] = entry.getEvent();
        array[5] = entry.getOperation();
        return array;
    }
    
    private static void writeSubActivity(PrintStream stream, UcmActivity activity) {
            stream.println("<subactivity>");
            String[] activityValues = getEntryAsStrings(activity);
            for (int tag = 0; tag < ACTIVITY_TAGS.length; tag++) {
                stream.print("\t<");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.print('>');
                stream.print(ClearCaseChangeLogSet.escapeForXml(activityValues[tag]));
                stream.print("</");
                stream.print(UcmChangeLogSet.ACTIVITY_TAGS[tag]);
                stream.println('>');
            }
            for (UcmActivity subActivity : activity.getSubActivities()) {
                writeSubActivity(stream,subActivity);
            }
            stream.println("</subactivity>");
        
    }
    
}
