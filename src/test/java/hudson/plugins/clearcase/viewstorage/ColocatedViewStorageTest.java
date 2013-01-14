package hudson.plugins.clearcase.viewstorage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ColocatedViewStorageTest {

    @Test
    public void nominal() {
        ViewStorage instance = new ColocatedViewStorage();
        assertEquals(0, instance.getCommandArguments(true, "viewtag").length);
    }
}
