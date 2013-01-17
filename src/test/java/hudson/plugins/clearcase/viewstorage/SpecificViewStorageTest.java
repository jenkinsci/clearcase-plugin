package hudson.plugins.clearcase.viewstorage;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public class SpecificViewStorageTest {

    @Test
    public void windows() {
        ViewStorage instance = new SpecificViewStorage("windows", "unix");
        assertArrayEquals(new String[]{"-vws", "windows\\viewTag.vws"}, instance.getCommandArguments(false, "viewTag"));
    }

    @Test
    public void unix() {
        ViewStorage instance = new SpecificViewStorage("windows", "unix");
        assertArrayEquals(new String[]{"-vws", "unix/viewTag.vws"}, instance.getCommandArguments(true, "viewTag"));
    }
}
