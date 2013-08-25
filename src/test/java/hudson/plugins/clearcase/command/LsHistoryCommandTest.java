package hudson.plugins.clearcase.command;

import static org.fest.assertions.api.Assertions.assertThat;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearToolLauncher;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LsHistoryCommandTest {
    @Mock ClearToolLauncher launcher;
    @Mock TaskListener listener;
    LsHistoryCommand instance;
    
    @Before
    public void setUp(){
        instance = new LsHistoryCommand().viewPath(new FilePath(new File("")));
    }
    
    @Test
    public void testDefaults(){
        List<String> cmd = buildCmd();
        assertThat(cmd).doesNotContain("-recurse", "-since", "-last", "-branch", "-minor", "-fmt");
        assertThat(cmd).contains("-all", "-nco");
    }
    
    @Test
    public void testBranch() {
        instance.branch("mybranch");
        assertThat(buildCmd()).containsSequence("-branch", "brtype:mybranch");
    }
    
    @Test
    public void testUseRecurse(){
        instance.useRecurse();
        List<String> cmd = buildCmd();
        assertThat(cmd).contains("-recurse");
        assertThat(cmd).doesNotContain("-all");
    }
    
    @Test
    public void testDoesNotUseRecurse(){
        List<String> cmd = buildCmd();
        assertThat(cmd).contains("-all");
        assertThat(cmd).doesNotContain("-recurse");
    }
    
    @Test
    public void testSince(){
        Date since = new Date(1);
        instance.since(since);
        List<String> cmd = buildCmd();
        SimpleDateFormat formatter = instance.getFormatter();
        assertThat(cmd).containsSequence("-since", formatter.format(since).toLowerCase());
    }
    
    @Test
    public void testNumberOfLastEvents(){
        instance.numberOfLastEvents(1);
        List<String> cmd = buildCmd();
        assertThat(cmd).containsSequence("-last", "1");
    }
    
    @Test
    public void testPathsInView(){
        instance.pathsInView("vob/file");
        List<String> cmd = buildCmd();
        assertThat(cmd).contains("vob/file");
    }
    
    @Test
    public void testPathsInViewWithSpaces(){
        instance.pathsInView("vob/file 1");
        List<String> cmd = buildCmd();
        assertThat(cmd).contains("\"vob/file 1\"");
    }

    private List<String> buildCmd() {
        return instance.generateCommandLine().toList();
    }

}
