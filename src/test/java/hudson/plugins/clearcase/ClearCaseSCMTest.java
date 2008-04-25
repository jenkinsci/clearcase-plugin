package hudson.plugins.clearcase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import org.junit.Test;

public class ClearCaseSCMTest {

    @Test
    public void testSnapshotBuildEnvVars() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.buildEnvVars(null, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator +"viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVars() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "/views", null);
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(null, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/views" + File.separator +"viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVarsNoViewDrive() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, null, null);
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(null, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testGetBranch() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null);
        assertEquals("The branch isnt correct", "branch", scm.getBranch());
    }
    
    @Test
    public void testGetConfigSpec() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null);
        assertEquals("The config spec isnt correct", "configspec", scm.getConfigSpec());
    }

    @Test
    public void testIsUseUpdate() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null);
        assertTrue("The isUpdate isnt correct", scm.isUseUpdate());
        
        scm = new ClearCaseSCM("branch", "configspec", "viewname", false, "", false, "", null);
        assertFalse("The isUpdate isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testIsDynamicView() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "", null);
        assertTrue("The dynamic isnt correct", scm.isUseDynamicView());
        assertFalse("The use update isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testGetViewDrive() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "/tmp/c", null);
        assertEquals("The view drive isnt correct", "/tmp/c", scm.getViewDrive());
    }

    @Test
    public void testGetBranchNames() {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "", true, "/tmp/c", null);
        assertArrayEquals("The branch name array is incorrect", new String[]{"branchone", "branchtwo"}, scm.getBranchNames());
    }

    @Test
    public void testGetVobPaths() {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "tmp/c aa", true, "", null);
        assertEquals("The vob paths string is incorrect", "tmp/c aa", scm.getVobPaths());
    }
}
