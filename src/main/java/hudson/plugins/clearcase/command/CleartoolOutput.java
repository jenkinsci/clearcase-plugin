package hudson.plugins.clearcase.command;

import java.io.InputStream;

public class CleartoolOutput {
    
    private final InputStream inputStream;
    private final boolean successful;
    
    CleartoolOutput(InputStream inputStream, boolean successful){
        this.inputStream = inputStream;
        this.successful = successful;
    }

    public InputStream getInputStream(){
        return inputStream;
    }

    public boolean isSuccessful() {
        return successful;
    }
}
