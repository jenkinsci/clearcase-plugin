package hudson.plugins.clearcase;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClearCaseUcmSCMTest {

    @Test
    public void testGetLoadRules() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The load rules arent correct", "loadrules", scm.getLoadRules());
    }

    @Test
    public void testGetStream() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The stream isnt correct", "stream", scm.getStream());
    }

    @Test
    public void testGetBranchNames() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    @Test
    public void testGetVobPaths() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "loadrules", scm.getVobPaths());
    }

    @Test
    public void testGetVobPathsWithSpaces() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "file with space\nanotherfile", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "\"file with space\" anotherfile", scm.getVobPaths());
    }
}
