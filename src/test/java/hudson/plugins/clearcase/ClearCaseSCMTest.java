package hudson.plugins.clearcase;

import hudson.Util;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.plugins.clearcase.action.ChangeLogAction;
import hudson.plugins.clearcase.base.BaseChangeLogAction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClearCaseSCMTest extends AbstractWorkspaceTest {

    private Mockery classContext;
    private AbstractProject project;
    private Build build;

    @Before
    public void setUp() throws Exception {
        createWorkspace();
        classContext = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
    }
    @After
    public void tearDown() throws Exception {
        deleteWorkspace();
    }
    
    @Test
    public void testCreateChangeLogParser() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false);
        assertNotNull("The change log parser is null", scm.createChangeLogParser());
        assertNotSame("The change log parser is re-used", scm.createChangeLogParser(), scm.createChangeLogParser());
    }
    
    @Test
    public void testSnapshotBuildEnvVars() {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false);
        Map<String, String> env = new HashMap<String, String>();
        env.put("WORKSPACE", "/hudson/jobs/job/workspace");
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/hudson/jobs/job/workspace" + File.separator +"viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVars() {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "/views", null, false);
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertEquals("The env var VIEWPATH wasnt set", "/views" + File.separator +"viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testDynamicBuildEnvVarsNoViewDrive() {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, null, null, false);
        Map<String, String> env = new HashMap<String, String>();
        scm.buildEnvVars(build, env);
        assertEquals("The env var VIEWNAME wasnt set", "viewname", env.get(ClearCaseSCM.CLEARCASE_VIEWNAME_ENVSTR));
        assertFalse("The env var VIEWPATH was set", env.containsKey(ClearCaseSCM.CLEARCASE_VIEWPATH_ENVSTR));
    }

    @Test
    public void testGetBranch() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false);
        assertEquals("The branch isnt correct", "branch", scm.getBranch());
    }
    
    @Test
    public void testGetConfigSpec() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false);
        assertEquals("The config spec isnt correct", "configspec", scm.getConfigSpec());
    }

    @Test
    public void testIsUseUpdate() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", false, "", null, false);
        assertTrue("The isUpdate isnt correct", scm.isUseUpdate());
        
        scm = new ClearCaseSCM("branch", "configspec", "viewname", false, "", false, "", null, false);
        assertFalse("The isUpdate isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testIsDynamicView() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "", null, false);
        assertTrue("The dynamic isnt correct", scm.isUseDynamicView());
        assertFalse("The use update isnt correct", scm.isUseUpdate());
    }

    @Test
    public void testGetViewDrive() {
        ClearCaseSCM scm = new ClearCaseSCM("branch", "configspec", "viewname", true, "", true, "/tmp/c", null, false);
        assertEquals("The view drive isnt correct", "/tmp/c", scm.getViewDrive());
    }

    @Test
    public void testGetBranchNames() {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "", true, "/tmp/c", null, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"branchone", "branchtwo"}, scm.getBranchNames());
    }

    @Test
    public void assertEmptyBranchIsReturnedAsABranch() {
        ClearCaseSCM scm = new ClearCaseSCM("", "configspec", "viewname", true, "", true, "/tmp/c", null, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{""}, scm.getBranchNames());
    }

    @Test
    public void assertBranchWithSpaceWorks() {
        ClearCaseSCM scm = new ClearCaseSCM("branch\\ one", "configspec", "viewname", true, "", true, "/tmp/c", null, false);
        assertArrayEquals("The branch name array is incorrect", new String[]{"branch one"}, scm.getBranchNames());
    }

    @Test
    public void testGetViewPaths() throws Exception {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "tmp", true, "", null, false);
        assertEquals("The view paths string is incorrect", "tmp", scm.getViewPaths(workspace)[0]);
    }

    @Test
    public void assertGetVobPaths() throws Exception {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "tmp", true, "", null, false);
        assertEquals("The vob paths string is incorrect", "tmp", scm.getVobPaths());
    }
    
    @Test
    public void assertViewPathIsCopiedFromVobPaths() throws Exception {
        ClearCaseSCM scm = new ClearCaseSCM("branchone branchtwo", "configspec", "viewname", true, "vob1 vob2 vob\\ 3", true, "", null, false);
        String[] viewPaths = scm.getViewPaths(workspace.child("viewName"));
        assertEquals("The size of view paths array is incorrect", 3, viewPaths.length);
        assertObjectInArray(viewPaths, "vob1");
        assertObjectInArray(viewPaths, "vob2");
        assertObjectInArray(viewPaths, "vob 3");
    }
    
    @Test
    public void assertViewPathsAreReadFromViewFolder() throws Exception {
        workspace.child("viewName").mkdirs();
        workspace.child("viewName").child("vob1").mkdirs();
        workspace.child("viewName").child("vob2").child("vob2-1").mkdirs();
        workspace.child("viewName").child("vob 4").mkdirs();
        workspace.child("viewName").createTextTempFile("view", ".dat", "text");
        ClearCaseSCM scm = new ClearCaseSCM("branchone", "configspec", "viewname", true, " ", true, "", null, false);
        String[] viewPaths = scm.getViewPaths(workspace.child("viewName"));
        assertEquals("The size of view paths array is incorrect", 3, viewPaths.length);
        assertObjectInArray(viewPaths, "vob1");
        assertObjectInArray(viewPaths, "vob2");
        assertObjectInArray(viewPaths, "vob 4");
    }

    @Test
    public void assertExtendedViewPathIsSetForDynamicViews() throws Exception {
        classContext.checking(new Expectations() {
            {
                ignoring(build).getParent(); will(returnValue(project));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCM("branchone", "configspec", "viewname", true, "vob", true, "/view", null, false);
        BaseChangeLogAction action = scm.createChangeLogAction(null, build, 0);
        assertEquals("The extended view path is incorrect", "/view/viewname", action.getExtendedViewPath());
    }

    @Test
    public void assertExtendedViewPathUsesNormalizedViewName() throws Exception {
        classContext.checking(new Expectations() {
            {
                one(build).getParent(); will(returnValue(project));
                one(project).getName(); will(returnValue("ClearCase"));
            }
        });
        ClearCaseSCM scm = new ClearCaseSCM("branchone", "configspec", "viewname-${JOB_NAME}", true, "vob", true, "/view", null, false);
        BaseChangeLogAction action = scm.createChangeLogAction(null, build, 0);
        assertEquals("The extended view path is incorrect", "/view/viewname-clearcase", action.getExtendedViewPath());
        classContext.assertIsSatisfied();
    }
    
    private void assertObjectInArray(Object[] array, Object obj) {
        boolean found = false;
        for (Object objInArray : array) {
            if (obj.equals(objInArray)) {
                found = true;
            }
        }
        if (!found) {
            fail(obj + " was not found in array " + Arrays.toString(array));
        }
    }
}
