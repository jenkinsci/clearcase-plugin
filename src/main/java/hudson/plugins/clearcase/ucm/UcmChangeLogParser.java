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

import hudson.model.AbstractBuild;
import hudson.plugins.clearcase.ClearCaseChangeLogSet;
import hudson.plugins.clearcase.util.DigesterUtil;
import hudson.scm.ChangeLogParser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.commons.digester3.Digester;
import org.xml.sax.SAXException;

/**
 * ClearCase change log parser.
 * 
 * @author Erik Ramfelt
 */
public class UcmChangeLogParser extends ChangeLogParser {

    /**
     * Parses the change log file and returns a ClearCase change log set.
     * 
     * @param build
     *            the build for the change log
     * @param changeLogFile
     *            the change log file
     * @return the change log set
     */
    @Override
    public UcmChangeLogSet parse(AbstractBuild build, File changeLogFile) throws IOException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(changeLogFile);
        UcmChangeLogSet logSet = parse(build, fileInputStream);
        fileInputStream.close();
        return logSet;
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
    UcmChangeLogSet parse(AbstractBuild<?, ?> build, InputStream changeLogStream) throws IOException, SAXException {

        ArrayList<UcmActivity> history = new ArrayList<UcmActivity>();

        // Parse the change log file.
        boolean secure = (!Boolean.getBoolean(UcmChangeLogParser.class.getName() + ".UNSAFE"));
        Digester digester = DigesterUtil.createDigester( secure);
        digester.setClassLoader(UcmChangeLogSet.class.getClassLoader());
        digester.push(history);
        digester.addObjectCreate("*/entry", UcmActivity.class);

        digester.addBeanPropertySetter("*/entry/name");
        digester.addBeanPropertySetter("*/entry/headline");
        digester.addBeanPropertySetter("*/entry/stream");
        digester.addBeanPropertySetter("*/entry/user");

        digester.addObjectCreate("*/subactivity", UcmActivity.class);
        digester.addBeanPropertySetter("*/subactivity/name");
        digester.addBeanPropertySetter("*/subactivity/headline");
        digester.addBeanPropertySetter("*/subactivity/stream");
        digester.addBeanPropertySetter("*/subactivity/user");
        digester.addBeanPropertySetter("*/subactivity/modifier");
        digester.addSetNext("*/subactivity", "addSubActivity");

        digester.addObjectCreate("*/entry/file", UcmActivity.File.class);
        digester.addBeanPropertySetter("*/entry/file/name");
        digester.addBeanPropertySetter("*/entry/file/date", "dateStr");
        digester.addBeanPropertySetter("*/entry/file/comment");
        digester.addBeanPropertySetter("*/entry/file/version");
        digester.addBeanPropertySetter("*/entry/file/event");
        digester.addBeanPropertySetter("*/entry/file/operation");
        digester.addSetNext("*/entry/file", "addFile");

        digester.addSetNext("*/entry", "add");
        digester.parse(changeLogStream);

        return new UcmChangeLogSet(build, history);
    }

}
