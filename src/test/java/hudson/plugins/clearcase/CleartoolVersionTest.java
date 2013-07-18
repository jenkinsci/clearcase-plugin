package hudson.plugins.clearcase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.Test;

public class CleartoolVersionTest extends AbstractWorkspaceTest {
    @Test
    public void testParseVersion() throws IOException, CleartoolVersionParsingException {
        parseFileThenAssertVersion("ct-version-1.log", "7.1.2.6");
        parseFileThenAssertVersion("ct-version-2.log", "7.1.1.1");
        parseFileThenAssertVersion("ct-version-4.log", "2003.06.10");
        parseFileThenAssertVersion("ct-version-5.log", "2002.05.00");
    }

    @Test(expected = CleartoolVersionParsingException.class)
    public void testParseInvalidVersion() throws IOException, CleartoolVersionParsingException {
        CleartoolVersion.parseCmdOutput(getReaderOn("ct-version-3.log"));
    }
    
    @Test
    public void testCompareTo() {
        CleartoolVersion v7 = new CleartoolVersion("7");
        CleartoolVersion v7111 = new CleartoolVersion("7.1.1.1");
        CleartoolVersion v7126 = new CleartoolVersion("7.1.2.6");
        CleartoolVersion v2002 = new CleartoolVersion("2002.05.00");
        CleartoolVersion v2003 = new CleartoolVersion("2003.06.10");
        assertTrue(v2002.compareTo(v2003) < 0);
        assertTrue(v2003.compareTo(v2002) > 0);
        assertTrue(v2002.compareTo(v7) < 0);
        assertTrue(v7.compareTo(v2002) > 0);
        assertTrue(v2003.compareTo(v7) < 0);
        assertTrue(v2003.compareTo(v7111) < 0);
        assertTrue(v2003.compareTo(v7126) < 0);
        assertTrue(v7.compareTo(v7111) < 0);
        assertTrue(v7.compareTo(v7126) < 0);
        assertTrue(v7111.compareTo(v7126) < 0);
        assertTrue(v7111.compareTo(v7) > 0);
        assertTrue(v7126.compareTo(v7) > 0);
        assertTrue(v7126.compareTo(v7111) > 0);
    }

    private void parseFileThenAssertVersion(String filename, String expectedVersion) throws IOException, CleartoolVersionParsingException {
        Reader readerOn = getReaderOn(filename);
        CleartoolVersion version = CleartoolVersion.parseCmdOutput(readerOn);
        assertEquals(expectedVersion, version.getVersion());
    }

    private Reader getReaderOn(String filename) {
        return new InputStreamReader(getClass().getResourceAsStream(filename));
    }
}
