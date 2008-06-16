package hudson.plugins.clearcase.util;

/**
 *
 * @author Henrik L. Hansen
 */
public interface OutputFormat {
    
    // Format 
    public static final String START_DELIMITER ="\\\"";             
    public static final String END_DELIMITER ="\\\" "; // Note the space!
    public static final String REGEX_GROUP = "\"(.*)\"\\s*";
    public static final String LINEEND = "\\n";
    public static final String PLACEHOLDER = "\\\" \\\" ";
   
    
    //Comment
    public static final String COMMENT="%c";
    public static final String COMMENT_NONEWLINE="%Nc";
    
    //Date
    public static final String DATE = "%d";
    public static final String DATE_NUMERIC = "%Nd";
    
    //Event
    public static final String EVENT ="%e";
    
    // Name
    public static final String NAME = "%n";
    public static final String NAME_ELEMENTNAME = "%En";
    public static final String NAME_VERSIONID="%Vn";

    //Event
    public static final String OPERATION ="%o";
    
       
    //User
    public static final String USER_ID = "%u";
    public static final String USER_FULLNAME = "%Fu";
    public static final String USER_GROUPNAME = "%Gu";
    public static final String USER_LOGIN_AND_GROUP = "%Lu";
    
    
    // UCM Activities
    public static final String UCM_ACTIVITY_HEADLINE=  "%[headline]p";
    public static final String UCM_ACTIVITY_STREAM=  "%[stream]p";
    public static final String UCM_ACTIVITY_VIEW=  "%[view]p";
    public static final String UCM_ACTIVITY_CONTRIBUTING=  "%[contrib_acts]p";    
    
    // UCM Versions
    public static final String UCM_VERSION_ACTIVITY="%[activity]p";
}
