/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Interface to hide the Hudson launch parts so other parts can mock the actual launch.
 * 
 * @author Erik Ramfelt
 */
public interface ClearToolLauncher {
    
    
    /**
     * Launches a cleartool command with arguments. 
     * @param cmd the command to launch using the clear tool executable
     * @param filePath
     * @return execPath optional, the path where the command should be launched
     */
    boolean run(String[] cmd, FilePath execPath) throws IOException, InterruptedException;
    /**
     * Launches a cleartool command with arguments. 
     * 
     * @param cmd the command to launch using the clear tool executable
     * @param in optional, if the command should be able to receive input
     * @param out optional, can be used to gather the output stream
     * @param execPath optional, the path where the command should be launched
     * @return true if the command was successful, false otherwise
     */
    boolean run(String[] cmd, InputStream in, OutputStream out, FilePath execPath) throws IOException,
            InterruptedException;

    /**
     * Returns a task listener for a hudson job
     * 
     * @return a task listener
     */
    TaskListener getListener();

    /**
     * Returns the workspace file path for a hudson job
     * 
     * @return the workspace file path
     */
    FilePath getWorkspace(); 
    
    /**
     * @return the Hudsonlauncher
     */
    Launcher getLauncher();
}
