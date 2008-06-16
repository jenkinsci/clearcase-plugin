package hudson.plugins.clearcase.ucm;

import static org.junit.Assert.*;
import hudson.plugins.clearcase.ucm.UcmActivity.File;
import java.util.Calendar;
import org.junit.Test;

public class UcmChangeLogParserTest {

    @Test
    public void assertParseOutput() throws Exception {
        UcmChangeLogParser parser = new UcmChangeLogParser();
        UcmChangeLogSet logSet = parser.parse(null, UcmChangeLogParserTest.class.getResourceAsStream("ucmchangelog.xml"));
        assertEquals("The log set should only contain 1 entry", 1, logSet.getItems().length);
        
        UcmActivity activity = logSet.getLogs().get(0);
        assertEquals("Activity name is incorrect", "name", activity.getName());
        assertEquals("Activity headline is incorrect", "headline", activity.getHeadline());
        assertEquals("Activity stream is incorrect", "stream", activity.getStream());
        assertEquals("Activity user is incorrect", "user", activity.getUser());
        
        assertEquals("Activity should contain one file", 1, activity.getFiles().size());
        
        //test subactivities
        assertEquals("There should be 2 subactivities", 2 , activity.getSubActivities().size());
        
        UcmActivity subActivity = activity.getSubActivities().get(0);
        assertEquals("SubActivity name is incorrect", "sub_name1", subActivity.getName());
        assertEquals("SubActivity headline is incorrect", "sub_headline1", subActivity.getHeadline());
        assertEquals("SubActivity stream is incorrect", "sub_stream1", subActivity.getStream());
        assertEquals("SubActivity user is incorrect", "sub_user1", subActivity.getUser());
        
        assertEquals("SubActivity should contain no files", 0, subActivity.getFiles().size());
        assertEquals("SubActivity should contain 1 subSubActivity", 1, subActivity.getSubActivities().size());
        
        UcmActivity subSubActivity = subActivity.getSubActivities().get(0);

        assertEquals("SubSubActivity name is incorrect", "sub_sub_name1", subSubActivity.getName());
        assertEquals("SubSubActivity headline is incorrect", "sub_sub_headline1", subSubActivity.getHeadline());
        assertEquals("SubSubActivity stream is incorrect", "sub_sub_stream1", subSubActivity.getStream());
        assertEquals("SubSubActivity user is incorrect", "sub_sub_user1", subSubActivity.getUser());
        
        assertEquals("SubSubActivity should contain no files", 0, subSubActivity.getFiles().size());
        assertEquals("SubSubActivity should contain 1 subSubActivity", 0, subSubActivity.getSubActivities().size());
        
        
        File file = activity.getFiles().get(0);
        assertEquals("File name is incorrect", "file-name", file.getName());
        assertEquals("File comment is incorrect", "file-comment", file.getComment());
        assertEquals("File event is incorrect", "file-event", file.getEvent());
        assertEquals("File operation is incorrect", "file-operation", file.getOperation());
        assertEquals("File version is incorrect", "file-version", file.getVersion());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(2008, 4, 8, 12, 20, 00);
        assertEquals("File date is incorrect", cal.getTime(), file.getDate());
    }
}
