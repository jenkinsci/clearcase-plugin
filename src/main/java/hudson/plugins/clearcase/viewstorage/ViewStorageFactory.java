/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.viewstorage;

import org.kohsuke.stapler.DataBoundConstructor;

@Deprecated
public class ViewStorageFactory {

    private String server;

    private String unixStorageDir;

    private String winStorageDir;

    @DataBoundConstructor
    public ViewStorageFactory(String server, String winDynStorageDir, String unixDynStorageDir) {
        this.server = server;
        this.winStorageDir = winDynStorageDir;
        this.unixStorageDir = unixDynStorageDir;
    }

    public String getServer() {
        return server;
    }

    public String getUnixStorageDir() {
        return unixStorageDir;
    }

    public String getWinStorageDir() {
        return winStorageDir;
    }

}
