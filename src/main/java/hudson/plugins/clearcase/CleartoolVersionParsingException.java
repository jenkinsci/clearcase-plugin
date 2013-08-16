package hudson.plugins.clearcase;

/**
 * Exception thrown in case the output of cleartool -version couldn't be parsed into a valid version
 * 
 * @author vlatombe
 * 
 */
public class CleartoolVersionParsingException extends Exception {
    public CleartoolVersionParsingException() {
        super("Unable to parse a cleartool version");
    }

}
