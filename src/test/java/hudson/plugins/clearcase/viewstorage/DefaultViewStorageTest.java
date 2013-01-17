package hudson.plugins.clearcase.viewstorage;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DefaultViewStorageTest {

    @Test
    public void nominal() {
        ViewStorage instance = new DefaultViewStorage();
        assertEquals(0, instance.getCommandArguments(true, "viewtag").length);
    }
}
