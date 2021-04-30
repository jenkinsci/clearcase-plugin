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

import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.util.DigesterUtil;
import hudson.scm.ChangeLogSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

/**
 * ClearCase change log set.
 * 
 * @author Erik Ramfelt
 */
public class ClearCaseChangeLogSet extends ChangeLogSet<ClearCaseChangeLogEntry> {

    static final String[]                 TAGS    = new String[] { "user", "comment", "date" };

    private List<ClearCaseChangeLogEntry> history = null;

    public ClearCaseChangeLogSet(AbstractBuild<?, ?> build, List<ClearCaseChangeLogEntry> logs) {
        super(build);
        for (ClearCaseChangeLogEntry entry : logs) {
            entry.setParent(this);
        }
        this.history = Collections.unmodifiableList(logs);
    }

    public List<ClearCaseChangeLogEntry> getLogs() {
        return history;
    }

    @Override
    public boolean isEmptySet() {
        return history.size() == 0;
    }

    @Override
    public Iterator<ClearCaseChangeLogEntry> iterator() {
        return history.iterator();
    }

    public static String escapeForXml(String string) {
        if (string == null) {
            return "";
        }

        // Loop through and replace the special chars.
        int size = string.length();
        char ch = 0;
        StringBuffer escapedString = new StringBuffer(size);
        for (int index = 0; index < size; index++) {
            // Convert special chars.
            ch = string.charAt(index);
            switch (ch) {
            case '&':
                escapedString.append("&amp;");
                break;
            case '<':
                escapedString.append("&lt;");
                break;
            case '>':
                escapedString.append("&gt;");
                break;
            case '\'':
                escapedString.append("&apos;");
                break;
            case '\"':
                escapedString.append("&quot;");
                break;
            default:
                escapedString.append(ch);
            }
        }

        return escapedString.toString().trim();
    }

    /**
     * Parses the change log file and returns a ClearCase change log set.
     * 
     * @param build
     *            the build for the change log
     * @param changeLogFile
     *            the change log file
     * @return the change log set
     */
    public static ClearCaseChangeLogSet parse(AbstractBuild<?, ?> build, File changeLogFile) throws IOException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(changeLogFile);
        ClearCaseChangeLogSet logSet = parse(build, fileInputStream);
        fileInputStream.close();
        return logSet;
    }

    /**
     * Stores the history objects to the output stream as xml
     * 
     * @param outputStream
     *            the stream to write to
     * @param history
     *            the history objects to store
     * @throws IOException
     */
    public static void saveToChangeLog(OutputStream outputStream, List<ClearCaseChangeLogEntry> history) throws IOException {
        PrintStream stream = new PrintStream(outputStream, false, "UTF-8");

        int tagcount = ClearCaseChangeLogSet.TAGS.length;
        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<history>");
        for (ClearCaseChangeLogEntry entry : history) {
            stream.println("\t<entry>");
            String[] strings = getEntryAsStrings(entry);
            for (int tag = 0; tag < tagcount; tag++) {
                stream.print("\t\t<");
                stream.print(ClearCaseChangeLogSet.TAGS[tag]);
                stream.print('>');
                stream.print(escapeForXml(strings[tag]));
                stream.print("</");
                stream.print(ClearCaseChangeLogSet.TAGS[tag]);
                stream.println('>');
            }
            for (ClearCaseChangeLogEntry.FileElement file : entry.getElements()) {
                stream.println("\t\t<element>");
                stream.println("\t\t\t<file>");
                stream.println(escapeForXml(file.getFile()));
                stream.println("\t\t\t</file>");
                stream.println("\t\t\t<action>");
                stream.println(escapeForXml(file.getAction()));
                stream.println("\t\t\t</action>");
                stream.println("\t\t\t<version>");
                stream.println(escapeForXml(file.getVersion()));
                stream.println("\t\t\t</version>");
                stream.println("\t\t\t<operation>");
                stream.println(escapeForXml(file.getOperation()));
                stream.println("\t\t\t</operation>");
                stream.println("\t\t</element>");
            }
            stream.println("\t</entry>");
        }
        stream.println("</history>");
        stream.close();
    }

    /**
     * Parses the change log stream and returns a ClearCase change log set.
     * 
     * @param build
     *            the build for the change log
     * @param changeLogStream
     *            input stream containing the change log
     * @return the change log set
     */
    static ClearCaseChangeLogSet parse(AbstractBuild<?, ?> build, InputStream changeLogStream) throws IOException, SAXException {

        ArrayList<ClearCaseChangeLogEntry> history = new ArrayList<ClearCaseChangeLogEntry>();

        // Parse the change log file.
        boolean secure = (!Boolean.getBoolean(ClearCaseChangeLogSet.class.getName() + ".UNSAFE"));
        Digester digester = DigesterUtil.createDigester( secure);
        digester.setClassLoader(ClearCaseChangeLogSet.class.getClassLoader());
        digester.push(history);
        digester.addObjectCreate("*/entry", ClearCaseChangeLogEntry.class);

        digester.addBeanPropertySetter("*/entry/date", "dateStr");
        digester.addBeanPropertySetter("*/entry/comment");
        digester.addBeanPropertySetter("*/entry/user");
        digester.addBeanPropertySetter("*/entry/file");
        digester.addBeanPropertySetter("*/entry/action");
        digester.addBeanPropertySetter("*/entry/version");

        digester.addObjectCreate("*/entry/element", ClearCaseChangeLogEntry.FileElement.class);
        digester.addBeanPropertySetter("*/entry/element/file");
        digester.addBeanPropertySetter("*/entry/element/version");
        digester.addBeanPropertySetter("*/entry/element/action");
        digester.addBeanPropertySetter("*/entry/element/operation");
        digester.addSetNext("*/entry/element", "addElement");

        digester.addSetNext("*/entry", "add");
        digester.parse(changeLogStream);

        return new ClearCaseChangeLogSet(build, history);
    }

    private static String[] getEntryAsStrings(ClearCaseChangeLogEntry entry) {
        String[] array = new String[TAGS.length];
        array[0] = entry.getUser();
        array[1] = entry.getComment();
        array[2] = entry.getDateStr();
        return array;
    }
}
