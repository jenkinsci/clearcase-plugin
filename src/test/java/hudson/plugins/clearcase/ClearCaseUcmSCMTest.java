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

    /**
     * The stream cant be used as a branch name directly if it contains a vob selector.
     * cleartool lshistory -r <snip/> -branch brtype:shared_development_2_1@/vobs/UCM_project 
     * cleartool: Error: Object is in unexpected VOB: "brtype:shared_development_2_1@/vobs/UCM_project".
     */
    @Test
    public void testGetBranchNamesWithVobSelector() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream@/vob/paths", "loadrules", "viewname", "option");
        assertArrayEquals("The branch name array is incorrect", new String[]{"stream"}, scm.getBranchNames());
    }

    @Test
    public void testGetVobPaths() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "loadrules", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "loadrules", scm.getVobPaths());
    }

    /**
     * Test for (issue 1706).
     * VOBPaths are used by the lshistory command, and should not start with a 
     * "\\" or "/" as that would make the cleartool command think the view is
     * located by an absolute path and not an relative path.
     */
    @Test
    public void assertVobPathDoesNotStartWithFileSeparator() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\\\load\\ruleone\n/load/ruletwo", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "load\\ruleone load/ruletwo", scm.getVobPaths());
    }

    @Test
    public void testGetVobPathsWithSpaces() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "file with space\nanotherfile", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "\"file with space\" anotherfile", scm.getVobPaths());
    }
    
    @Test
    public void testGetWindowsVobPaths() {
        ClearCaseUcmSCM scm = new ClearCaseUcmSCM("stream", "\\ \\ Windows\n\\\\C\\System32", "viewname", "option");
        assertEquals("The vob path is not the same as the load rules", "\" \\ Windows\" C\\System32", scm.getVobPaths());
    }    
}
