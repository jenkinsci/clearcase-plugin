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

import hudson.Extension;
import hudson.Util;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.VariableResolver;

import org.kohsuke.stapler.DataBoundConstructor;

public class SpecificViewStorage extends ViewStorage {

    @Extension
    public static class DescriptorImpl extends ViewStorageDescriptor<SpecificViewStorage> {

        @Override
        public String getDisplayName() {
            return "Use explicit path";
        }

        public String getUnixStorageDir() {
            ViewStorage defaultViewStorage = getDefaultViewStorage();
            if (defaultViewStorage instanceof SpecificViewStorage) {
                return ((SpecificViewStorage) defaultViewStorage).getUnixStorageDir();
            }
            return getClearcaseDescriptor().getDefaultUnixDynStorageDir();
        }

        public String getWinStorageDir() {
            ViewStorage defaultViewStorage = getDefaultViewStorage();
            if (defaultViewStorage instanceof SpecificViewStorage) {
                return ((SpecificViewStorage) defaultViewStorage).getWinStorageDir();
            }
            return getClearcaseDescriptor().getDefaultWinDynStorageDir();
        }

    }

    private final String unixStorageDir;

    private final String winStorageDir;

    /**
     * @param unix
     * @param winStorageDir
     * @param unixStorageDir
     */
    @DataBoundConstructor
    public SpecificViewStorage(String winStorageDir, String unixStorageDir) {
        this.winStorageDir = winStorageDir;
        this.unixStorageDir = unixStorageDir;
    }

    @Override
    public SpecificViewStorage decorate(VariableResolver<String> resolver) {
        return new SpecificViewStorage(Util.replaceMacro(winStorageDir, resolver), Util.replaceMacro(unixStorageDir, resolver));
    }

    @Override
    public String[] getCommandArguments(boolean unix, String viewTag) {
        return new String[] { "-vws", getStorageDir(unix) + sepFor(unix) + viewTag + ".vws" };
    }

    public String getUnixStorageDir() {
        return unixStorageDir;
    }

    public String getWinStorageDir() {
        return winStorageDir;
    }

    private String getStorageDir(boolean unix) {
        if (unix) {
            return unixStorageDir;
        }
        return winStorageDir;
    }

    private String sepFor(boolean unix) {
        return PathUtil.fileSepForOS(unix);
    }

}
