package hudson.plugins.clearcase;

import java.io.InputStream;
import java.io.OutputStream;

import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

/**
 * JMock action to feed an OutputStream with data.
 * 
 * @author Erik Ramfelt
 */
public class StreamCopyAction implements Action {
    private InputStream inputStream;
    private int parameterIndex;

    public StreamCopyAction(int parameterIndex, InputStream inputStream) {
        this.inputStream = inputStream;
        this.parameterIndex = parameterIndex;
    }

    public void describeTo(Description description) {
    }

    public Object invoke(Invocation invocation) throws Throwable {
        int read = inputStream.read();
        while (read != -1) {
            ((OutputStream) invocation.getParameter(parameterIndex)).write(read);
            read = inputStream.read();
        }
        inputStream.close();
        return null;
    }
}
