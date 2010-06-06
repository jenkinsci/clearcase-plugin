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
package hudson.plugins.clearcase.base;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.plugins.clearcase.AbstractClearCaseScm;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry;
import hudson.plugins.clearcase.ClearCaseSCM;
import hudson.plugins.clearcase.ClearCaseSCMDummy;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.plugins.clearcase.ClearCaseChangeLogEntry.FileElement;
import hudson.plugins.clearcase.history.DefaultFilter;
import hudson.plugins.clearcase.history.DestroySubBranchFilter;
import hudson.plugins.clearcase.history.FileFilter;
import hudson.plugins.clearcase.history.Filter;
import hudson.plugins.clearcase.history.FilterChain;
import hudson.plugins.clearcase.util.BuildVariableResolver;
import hudson.util.VariableResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Bug;

public class BaseHistoryActionTest {

    private static final String VALID_HISTORY_FORMAT="\\\"%Nd\\\" \\\"%u\\\" \\\"%En\\\" \\\"%Vn\\\" \\\"%e\\\" \\\"%o\\\" \\n%c\\n";
    private Mockery context;
    private Mockery classContext;
    private AbstractProject project;
    private Build build;
    private Launcher launcher;
    private ClearToolLauncher clearToolLauncher;
    private ClearCaseSCM.ClearCaseScmDescriptor clearCaseScmDescriptor;

    private ClearTool cleartool;
    
    @Before
    public void setUp() throws Exception {
        context = new Mockery();
        cleartool = context.mock(ClearTool.class);
        clearToolLauncher = context.mock(ClearToolLauncher.class);
        classContext = new Mockery() {
                {
                    setImposteriser(ClassImposteriser.INSTANCE);
                }
            };
        project = classContext.mock(AbstractProject.class);
        build = classContext.mock(Build.class);
        launcher = classContext.mock(Launcher.class);
        clearCaseScmDescriptor = classContext.mock(ClearCaseSCM.ClearCaseScmDescriptor.class);
    }

    /*
     * Below is taken from DefaultPollAction
     */

    @Test
    public void assertSeparateBranchCommands() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader("")));
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchtwo")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" ")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);
        context.assertIsSatisfied();
    }

    //    @Test
    //    public void assertFirstFoundChangeStopsPolling() throws Exception {
    //        context.checking(new Expectations() {
    //            {
    //                one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branchone")), with(equal(new String[]{"vobpath"})));
    //                will(returnValue(new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\" \"mkelem\" ")));
    //            }
    //        });
    //
    //        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
    //        boolean hasChange = action.hasChanges(null, "view", new String[]{"branchone", "branchtwo"}, new String[]{"vobpath"});
    //        assertTrue("The getChanges() method did not report a change", hasChange);
    //        context.assertIsSatisfied();
    //    }

    @Test
    public void assertSuccessfulParse() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader(
                                                      "\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" "
                                                      + "\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\2\" \"create version\"  \"mkelem\" ")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method did not report a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringErrors() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader("cleartool: Error: Not an object in a vob: \"view.dat\".\n")));
                }
            });
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,new DefaultFilter(),0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringVersionZero() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\0\" \"create version\"  \"mkelem\" ")));
                }
            });
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,new DefaultFilter(),0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader(
                                                      "\"20080326.110739\" \"user\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\"")));
                }
            });
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,new DestroySubBranchFilter(),0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertFalse("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test
    public void assertNotIgnoringDestroySubBranchEvent() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(new StringReader(
                                                      "\"20080326.110739\" \"user\" \"vobs/gtx2/core/src/foo/bar/MyFile.java\" \"/main/feature_1.23\" \"destroy sub-branch \"esmalling_branch\" of branch\" \"rmbranch\"")));
                }
            });


        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
        boolean hasChange = action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        assertTrue("The getChanges() method reported a change", hasChange);
        context.assertIsSatisfied();
    }

    @Test(expected=IOException.class)
    public void assertReaderIsClosed() throws Exception {
        final StringReader reader = new StringReader("\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" ");
        context.checking(new Expectations() {
                {
                    ignoring(cleartool).lshistory(with(aNonNull(String.class)), with(aNull(Date.class)), with(equal("view")), with(equal("branch")), with(equal(new String[]{"vobpath"})));
                    will(returnValue(reader));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
        action.hasChanges(null, "view", new String[]{"branch"}, new String[]{"vobpath"});
        reader.ready();
    }



    /*
     * Below is taken from BaseChangelogAction
     */

    @Test
    public void assertFormatContainsComment() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                                             with(any(String[].class)));
                    will(returnValue(new StringReader("")));
                }
            });
        
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null,0);
        action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        context.assertIsSatisfied();
    }

    @Test
    public void assertDestroySubBranchEventIsIgnored() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                                             with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070906.091701\"   \"egsperi\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"destroy sub-branch \"esmalling_branch\" of branch\"   \"mkelem\"\n")));
                }
            });
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,new DestroySubBranchFilter(), 10000);
        List<ClearCaseChangeLogEntry> changes = (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("The event record should be ignored", 0, changes.size());        
        context.assertIsSatisfied();        
    }

    @Test
    public void assertExcludedRegionsAreIgnored() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                                             with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20071015.151822\" \"user\" \"Customer\\DataSet.xsd\" \"\\main\\sit_r6a\\1\" \"create version\"  \"mkelem\" ")));


                }
            });
        
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(new DefaultFilter());
        filters.add(new FileFilter(FileFilter.Type.DoesNotContainRegxp, "Customer"));

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,new FilterChain(filters), 10000);
        List<ClearCaseChangeLogEntry> changes = (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("The event record should be ignored", 0, changes.size());        
        context.assertIsSatisfied();        
    }

    @Test
    public void assertMergedLogEntries() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)), with(any(String.class)), with(any(String.class)), 
                                             with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070906.091701\"   \"egsperi\"  \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"create version\"   \"mkelem\"\n"
                                                      + "\"20070906.091705\"   \"egsperi\"  \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"   \"create version\"  \"mkelem\"\n")));
                }
            }); 
        
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        List<ClearCaseChangeLogEntry> changes = (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Two entries should be merged into one", 1, changes.size());        
        context.assertIsSatisfied();        
    }

    @Test(expected=IOException.class)
    public void assertReaderIsClosed2() throws Exception {
        final StringReader reader = new StringReader("\"20070906.091701\"   \"egsperi\" \"\\ApplicationConfiguration\" \"\\main\\sit_r6a\\2\"  \"create version\"  \"mkelem\"\n");
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)),
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(reader));
                }
            });
        
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});        
        context.assertIsSatisfied();
        reader.ready();
    }

    @Test
    public void testSorted() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)),
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.084801\"   \"inttest2\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n"
                                                      + "\"20070825.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n"
                                                      + "\"20070830.084801\"   \"inttest1\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\"  \"create version\" \"mkelem\"\n\n")));
                }
            });
        
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        List<ClearCaseChangeLogEntry> changes = (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 3, changes.size());
        assertEquals("First entry is incorrect", "inttest1", changes.get(0).getUser());
        assertEquals("First entry is incorrect", "inttest2", changes.get(1).getUser());
        assertEquals("First entry is incorrect", "inttest3", changes.get(2).getUser());
        context.assertIsSatisfied();
    }

    @Test
    public void testMultiline() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(equal(VALID_HISTORY_FORMAT)),
                                             with(any(Date.class)),
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070830.084801\"   \"inttest2\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n"
                                                      + "\"20070830.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n")));
                }
            });
        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        List<ClearCaseChangeLogEntry> changes =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, changes.size());
    }

    @Test
    public void testErrorOutput() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070830.084801\"   \"inttest3\"  \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n"
                                                      + "cleartool: Error: Branch type not found: \"sit_r6a\".\n"
                                                      + "\"20070829.084801\"   \"inttest3\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\"   \"mkelem\"\n\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        assertEquals("First entry is incorrect", "", entries.get(0).getComment());
        assertEquals("Scond entry is incorrect", "", entries.get(1).getComment());
    }

    @Test
    public void testUserOutput() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new InputStreamReader(
                                                           AbstractClearCaseScm.class.getResourceAsStream( "ct-lshistory-1.log"))));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 2, entries.size());
    }

    @Test
    public void testOperation() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\"  \"create directory version\"  \"mkelem\"\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 10000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        FileElement element = entries.get(0).getElements().get(0);
        assertEquals("Status is incorrect", "mkelem", element.getOperation());
    }

    @Test
    public void testParseNoComment() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.084801\" \"inttest14\" \"Source\\Definitions\\Definitions.csproj\" \"\\main\\sit_r5_maint\\1\" \"create version\" \"mkelem\"\n\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});

        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Definitions\\Definitions.csproj", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "inttest14", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 48, 1), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\1", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testEmptyComment() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\" \"create directory version\" \"mkelem\"\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Comment is incorrect", "", entry.getComment());
    }

    @Test
    public void testCommentWithEmptyLine() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070906.091701\"   \"egsperi\" \"\\Source\\ApplicationConfiguration\" \"\\main\\sit_r6a\\1\" \"create directory version\"   \"mkelem\"\ntext\n\nend of comment")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});

        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Comment is incorrect", "text\n\nend of comment", entry.getComment());
    }

    @Test
    public void testParseWithComment() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.085901\"   \"aname\"   \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\" \"create version\"   \"mkelem\"\nBUG8949")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949", entry.getComment());
    }

    @Test
    public void testParseWithTwoLineComment() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.085901\"   \"aname\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\"   \"create version\"   \"mkelem\"\nBUG8949\nThis fixed the problem")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());

        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File is incorrect", "Source\\Operator\\FormMain.cs", entry.getElements().get(0).getFile());
        assertEquals("User is incorrect", "aname", entry.getUser());
        assertEquals("Date is incorrect", getDate(2007, 7, 27, 8, 59, 01), entry.getDate());
        assertEquals("Action is incorrect", "create version", entry.getElements().get(0).getAction());
        assertEquals("Version is incorrect", "\\main\\sit_r5_maint\\2", entry.getElements().get(0).getVersion());
        assertEquals("Comment is incorrect", "BUG8949\nThis fixed the problem", entry.getComment());
    }

    @Test
    public void testParseWithLongAction() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.085901\"   \"aname\" \"Source\\Operator\\FormMain.cs\" \"\\main\\sit_r5_maint\\2\" \"create a version\"  \"mkelem\"\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("Action is incorrect", "create a version", entry.getElements().get(0).getAction());
    }

    @Test
    public void assertViewPathIsRemovedFromFilePaths() throws Exception {
        context.checking(new Expectations() {
                {
                    one(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                             with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20070827.085901\" \"user\" \"/view/ralef_0.2_nightly/vobs/Tools/framework/util/QT.h\" \"/main/comain\" \"action\"   \"mkelem\"\n")));
                }
            });

        BaseHistoryAction action = new BaseHistoryAction(cleartool,false,null, 1000);
        action.setExtendedViewPath("/view/ralef_0.2_nightly");
        List<ClearCaseChangeLogEntry> entries =  (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(), "IGNORED", new String[]{"Release_2_1_int"}, new String[]{"vobs/projects/Server"});
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File path is incorrect", "/vobs/Tools/framework/util/QT.h", entry.getElements().get(0).getFile());
    }

    /**
     * Bug was that we had (pre-1.0) been converting extended view path to lower case whenever we used it
     * or compared against it. I believe this was done because the view drive was manually specified, and so
     * on Windows, the configured value could be, say, m:\ or M:\ and either would be valid. In that context,
     * normalizing to lower-case meant we wouldn't have to worry about how view drive was specified, case-wise.
     * But with 1.0 and later, we're actually getting extended view path directly from cleartool pwv, and it
     * got changed in some places to no longer do toLowerCase() before comparisons, while the setter was still
     * converting to lower-case, which caused any path in a view with upper-case to be rejected by the filters.
     *
     * Now, we never call toLowerCase() on the extended view path, at any point, since it's just going to be the
     * output of pwv, which will have consistent case usage regardless of what we do.
     */
    @Bug(3666)
    @Test
    public void testCaseSensitivityInViewName() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(build).getParent(); will(returnValue(project));
                    allowing(project).getName(); will(returnValue("Issue3666"));
                    allowing(clearCaseScmDescriptor).getLogMergeTimeWindow(); will(returnValue(5));
                    allowing(launcher).isUnix(); will(returnValue(false));
                }});
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher();
                    will(returnValue(launcher));
                    allowing(cleartool).pwv(with(any(String.class)));
                    will(returnValue("Y:\\Hudson.SAP.ICI.7.6.Quick"));
                    allowing(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                                  with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20090909.151109\" \"nugarov\" " +
                                                      "\"Y:\\Hudson.SAP.ICI.7.6.Quick\\sapiciadapter\\Tools\\gplus_tt\\gplus_tt_config.py\" \"\\main\\dev-kiev-7.6\\10\" \"create version\" \"checkin\"\nvolatile")));
                    allowing(cleartool).startView("Hudson.SAP.ICI.7.6.Quick");
                    allowing(cleartool).mountVobs();
                    
                }
            });

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("", "configspec", "Hudson.SAP.ICI.7.6.Quick",
                                                      false, "load /sapiciadapter", true,
                                                      "Y:\\", "", false, false, false, "", "",
                                                      false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);
        List<ClearCaseChangeLogEntry> entries =
            (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(),
                                                              scm.generateNormalizedViewName((BuildVariableResolver)variableResolver),
                                                              scm.getBranchNames(),
                                                              scm.getViewPaths());
        assertEquals("Number of history entries are incorrect", 1, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File path is incorrect", "sapiciadapter\\Tools\\gplus_tt\\gplus_tt_config.py", entry.getElements().get(0).getFile());
    }

    /**
     * Very similar to above - but here, the upper-case is in the path to the workspace. Also, we're verifying that
     * an lshistory item for a path *not* specified in the load rules doesn't get included in the changelog.
     */
    @Bug(4430)
    @Test
    public void testCaseSensitivityInExtendedViewPath() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(build).getParent(); will(returnValue(project));
                    allowing(project).getName(); will(returnValue("Issue4430"));
                    allowing(clearCaseScmDescriptor).getLogMergeTimeWindow(); will(returnValue(5));
                    allowing(launcher).isUnix(); will(returnValue(false));
                }});
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher();
                    will(returnValue(launcher));
                    allowing(cleartool).pwv(with(any(String.class)));
                    will(returnValue("D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot"));
                    allowing(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                                  with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20090909.124752\" \"erustt\" " +
                                                      "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\apps\\esa\\ecl\\sot\\sot_impl\\src\\main\\java\\com\\ascom\\ecs3\\ecl\\sot\\nodeoperationstate\\OperationStateManagerImpl.java\" " +
                                                      "\"\\main\\refact_structure\\2\" \"create version\" \"checkin\"\n\n" +
                                                      "\"20090909.105713\" \"eveter\" " +
                                                      "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\apps\\confcmdnet\\ecl\\confcmdnet_webapp\\doc\" " +
                                                      "\"\\main\\refact_structure\\13\" \"create directory version\" \"checkin\"\nUncataloged file element \"ConfCmdNet_PendenzenListe.xlsx\".\n" +
                                                      "\"20090909.091004\" \"eruegr\" " +
                                                      "\"D:\\hudson\\jobs\\refact_structure__SOT\\workspace\\sa-seso-tempusr4__refact_structure__sot\\ecs3cop\\projects\\components\\ecc_dal\\dal_impl_hibernate\\src\\main\\java\\com\\ascom\\ecs3\\ecc\\dal\\impl\\hibernate\\ctrl\\SotButtonController.java\" " +
                                                      "\"\\main\\refact_structure\\16\" \"create version\" \"checkin\"\n\n"
                                                      )));
                    
                }
            });

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("refact_structure", "configspec",
                                                      "sa-seso-tempusr4__refact_structure__sot",
                                                      true, "load \\ecs3cop\\projects\\buildconfigurations\n" +
                                                      "load \\ecs3cop\\projects\\apps\\esa\n" +
                                                      "load \\ecs3cop\\projects\\apps\\tmp\n" +
                                                      "load \\ecs3cop\\projects\\components\n" +
                                                      "load \\ecs3cop\\projects\\test\n", false,
                                                      "", "", false, false, false, "", "",
                                                      false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);

        List<ClearCaseChangeLogEntry> entries =
            (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(),
                                                              scm.generateNormalizedViewName((BuildVariableResolver)variableResolver),
                                                              scm.getBranchNames(),
                                                              scm.getViewPaths());
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File path is incorrect", "ecs3cop\\projects\\apps\\esa\\ecl\\sot\\sot_impl\\src\\main\\java\\com\\ascom\\ecs3\\ecl\\sot\\nodeoperationstate\\OperationStateManagerImpl.java", entry.getElements().get(0).getFile());
    }



    /**
     * Making sure that load rules using "/" are handled properly on Windows.
     */
    @Bug(4781)
    @Test
    public void testUnixSlashesInWindowsLoadRules() throws Exception {
        classContext.checking(new Expectations() {
                {
                    allowing(build).getParent(); will(returnValue(project));
                    allowing(project).getName(); will(returnValue("Issue4781"));
                    allowing(clearCaseScmDescriptor).getLogMergeTimeWindow(); will(returnValue(5));
                    allowing(launcher).isUnix(); will(returnValue(false));
                }});
        
        context.checking(new Expectations() {
                {
                    allowing(clearToolLauncher).getLauncher();
                    will(returnValue(launcher));
                    allowing(cleartool).pwv(with(any(String.class)));
                    will(returnValue("D:\\hudson\\jobs\\somejob\\workspace\\someview"));
                    allowing(cleartool).lshistory(with(any(String.class)), with(any(Date.class)), 
                                                  with(any(String.class)), with(any(String.class)), with(any(String[].class)));
                    will(returnValue(new StringReader(
                                                      "\"20090909.124752\" \"erustt\" " +
                                                      "\"D:\\hudson\\jobs\\somejob\\workspace\\someview\\some_vob\\path\\to\\file.java\" " +
                                                      "\"\\main\\some_branch\\2\" \"create version\" \"checkin\"\n\n" +
                                                      "\"20090909.091004\" \"eruegr\" " +
                                                      "\"D:\\hudson\\jobs\\somejob\\workspace\\someview\\some_vob\\another\\path\\to\\anotherFile.java\" " +
                                                      "\"\\main\\some_branch\\16\" \"create version\" \"checkin\"\n\n"
                                                      )));
                    
                }
            });

        ClearCaseSCMDummy scm = new ClearCaseSCMDummy("some_branch", "configspec",
                                                      "someview",
                                                      true, "load /some_vob/path\n" +
                                                      "load /some_vob/another\n",
                                                      false, "", "", false, false, false, "", "",
                                                      false, false, cleartool, clearCaseScmDescriptor);

        VariableResolver variableResolver = new BuildVariableResolver(build, scm.getCurrentComputer());

        BaseHistoryAction action = (BaseHistoryAction) scm.createHistoryAction(variableResolver, clearToolLauncher, build);

        List<ClearCaseChangeLogEntry> entries =
            (List<ClearCaseChangeLogEntry>) action.getChanges(new Date(),
                                                              scm.generateNormalizedViewName((BuildVariableResolver)variableResolver),
                                                              scm.getBranchNames(),
                                                              scm.getViewPaths());
        assertEquals("Number of history entries are incorrect", 2, entries.size());
        ClearCaseChangeLogEntry entry = entries.get(0);
        assertEquals("File path is incorrect", "some_vob\\path\\to\\file.java", entry.getElements().get(0).getFile());
    }


    private Date getDate(int year, int month, int day, int hour, int min, int sec) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DATE, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, min);
        calendar.set(Calendar.SECOND, sec);
        return calendar.getTime();
    }
}
