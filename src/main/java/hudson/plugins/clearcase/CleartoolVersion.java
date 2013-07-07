package hudson.plugins.clearcase;

import hudson.util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * This class represents a cleartool version
 * @author vlatombe
 *
 */
public class CleartoolVersion implements Comparable<CleartoolVersion> {
    private static final Pattern CLEARTOOL_VERSION_PATTERN = Pattern.compile("^cleartool\\s*(\\d+(?:\\.\\d+)*).*$");
    
    private final String version;
    
    private int[] parsedVersion;
    
    public CleartoolVersion(String version) {
        this.version = version;
        parsedVersion = parseVersion(version);
    }
    
    private int[] parseVersion(String version) {
        if (version == null) return new int[0];
        String[] elements = StringUtils.split(version, '.');
        int[] parsedVersion = new int[elements.length];
        int i = 0;
        for (String s : elements) {
            try {
            parsedVersion[i++] = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return parsedVersion;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int compareTo(CleartoolVersion o) {
        if (o == null) {
            return 1;
        }
        if (parsedVersion == null) {
            return o.parsedVersion == null ? 0 : -1;
        }
        int l = Math.min(parsedVersion.length, o.parsedVersion.length);
        for (int i = 0; i < l; i++) {
            int op1 = parsedVersion[i];
            int op2 = o.parsedVersion[i];
            if (op1 != op2) {
                if (i == 0) {
                    // Handle Clearcase 2003 versions
                    if (op1 == 2003) {
                        return -1;
                    }
                    if (op2 == 2003) {
                        return 1;
                    }
                }
                return op1 - op2;
            }
        }
        return parsedVersion.length - o.parsedVersion.length;
    }

    public static CleartoolVersion parseCmdOutput(Reader reader) throws IOException, CleartoolVersionParsingException {
        BufferedReader br = new BufferedReader(reader);
        try {
            String line = br.readLine();
            while (line != null) {
                Matcher matcher = CLEARTOOL_VERSION_PATTERN.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1) {
                    return new CleartoolVersion(matcher.group(1));
                }
                line = br.readLine();
            }
        } finally {
            IOUtils.closeQuietly(reader);
        }
        throw new CleartoolVersionParsingException();
    }
    
}
