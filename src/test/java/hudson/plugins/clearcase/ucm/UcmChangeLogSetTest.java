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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class UcmChangeLogSetTest {
    
    @Test
    public void assertSavedLogSetCanBeParsed() throws Exception {
        
        UcmActivity activity = new UcmActivity();
        activity.setHeadline("headline");
        activity.setName("name");
        activity.setStream("stream");
        activity.setUser("user");
        
        UcmActivity.File activityFile = new UcmActivity.File();
        activityFile.setComment("file-comment");
        activityFile.setVersion("version1");
        activityFile.setName("file1");
        activityFile.setOperation("file-operation");
        activityFile.setEvent("file-event");
        activity.addFile(activityFile);

        File tempLogFile = File.createTempFile("clearcase", "xml");
        tempLogFile.deleteOnExit();
        FileOutputStream fileOutputStream = new FileOutputStream(tempLogFile);
        
        List<UcmActivity> activities = new ArrayList<UcmActivity>();
        activities.add(activity);
        UcmChangeLogSet.saveToChangeLog(fileOutputStream, activities);
        fileOutputStream.close();

        FileInputStream fileInputStream = new FileInputStream(tempLogFile);
        UcmChangeLogParser parser = new UcmChangeLogParser();
        UcmChangeLogSet logSet = parser.parse(null, fileInputStream);
        fileInputStream.close();
        
        List<UcmActivity> logs = logSet.getLogs();
        assertEquals("The number of activities is incorrect", 1, logs.size());
        assertEquals("The number of files in the first activity is incorrect", 1, logs.get(0).getFiles().size());
        assertEquals("The first file name is incorrect", "file1", logs.get(0).getFiles().get(0).getName());
        assertEquals("The first version is incorrect", "version1", logs.get(0).getFiles().get(0).getVersion());
    }
}
