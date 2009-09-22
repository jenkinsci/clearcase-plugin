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

import static org.junit.Assert.*;

import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;
import hudson.scm.EditType;

import java.util.Calendar;
import org.junit.Test;

public class ClearCaseChangeLogEntryTest {

    @Test
    public void testSetFormattedDateStr() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setDateStr("28/08/2007 15:27:00");
        assertEquals("The date str is incorrect", "28/08/2007 15:27:00", entry.getDateStr());

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2007, 7, 28, 15, 27, 0);
        assertEquals("The date is incorrect", calendar.getTime(), entry.getDate());
    }

    @Test
    public void testSetNonFormattedDateStr() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setDateStr("Tue Aug 28 15:27:00 CEST 2007");
        assertEquals("Tue Aug 28 15:27:00 CEST 2007", entry.getDateStr());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSetFileAndThenVersion() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setFile("Filename");
        entry.setVersion("version1");
        assertEquals("Filename", entry.getElements().get(0).getFile());
        assertEquals("version1", entry.getElements().get(0).getVersion());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSetVersionAndThenFile() {
        ClearCaseChangeLogEntry entry = new ClearCaseChangeLogEntry();
        entry.setVersion("version1");
        entry.setFile("Filename");
        assertEquals("Filename", entry.getElements().get(0).getFile());
        assertEquals("version1", entry.getElements().get(0).getVersion());
    }

    @Test
    public void testFileElementAddEditType() {
        FileElement element = new FileElement();
        element.setOperation("mkelem");
        assertSame("Edit type was incorrect", EditType.ADD, element.getEditType());
    }

    @Test
    public void testFileElementEditEditType() {
        FileElement element = new FileElement();
        element.setOperation("checkin");
        assertSame("Edit type was incorrect", EditType.EDIT, element.getEditType());
    }

    @Test
    public void testFileElementDeleteEditType() {
        FileElement element = new FileElement();
        element.setOperation("rmelem");
        assertSame("Edit type was incorrect", EditType.DELETE, element.getEditType());
    }

    @Test
    public void testFileElementUnknownEditType() {
        FileElement element = new FileElement();
        assertNull("Edit type was not null", element.getEditType());
    }
}
