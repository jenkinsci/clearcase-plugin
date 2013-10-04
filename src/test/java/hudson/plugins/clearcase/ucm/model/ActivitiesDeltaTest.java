package hudson.plugins.clearcase.ucm.model;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Test;

public class ActivitiesDeltaTest {
    @Test
    public void rightActivityWithQuotesInHeadline() throws IOException{
        ActivitiesDelta delta = ActivitiesDelta.parse(new StringReader(">> id@\\pvob \"my headline \"with\" quotes\""));
        assertThat(delta.getLeft().size()).isEqualTo(0);
        assertThat(delta.getRight().size()).isEqualTo(1);
        Activity activity = delta.getRight().iterator().next();
        assertThat(activity.getName()).isEqualTo("id");
        assertThat(activity.getPvob()).isEqualTo("\\pvob");
        assertThat(activity.getHeadline()).isEqualTo("my headline \"with\" quotes");
    }
}
