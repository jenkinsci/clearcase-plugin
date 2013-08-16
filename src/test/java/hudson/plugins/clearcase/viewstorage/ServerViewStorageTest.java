package hudson.plugins.clearcase.viewstorage;

import static org.junit.Assert.assertArrayEquals;
import hudson.util.VariableResolver;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class ServerViewStorageTest {

    @Test
    public void server() {
        ViewStorage instance = new ServerViewStorage("server1");
        assertArrayEquals(new String[] { "-stgloc", "server1" }, instance.getCommandArguments(true, "viewtag"));
    }

    @Test
    public void serverResolved() {
        ViewStorage instance = new ServerViewStorage("${server}");
        Map<String, String> data = new HashMap<String, String>();
        data.put("server", "server1");
        instance = instance.decorate(new VariableResolver.ByMap<String>(data));
        assertArrayEquals(new String[] { "-stgloc", "server1" }, instance.getCommandArguments(true, "viewtag"));
    }
}
