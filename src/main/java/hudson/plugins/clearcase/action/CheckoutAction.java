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
package hudson.plugins.clearcase.action;

import hudson.FilePath;
import hudson.Launcher;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.viewstorage.ViewStorage;

import java.io.IOException;

import org.apache.commons.lang.Validate;

/**
 * Action for performing check outs from ClearCase.
 */
public abstract class CheckoutAction {

    private ClearTool cleartool;

    private ViewStorage viewStorage;

    public CheckoutAction(ClearTool cleartool, ViewStorage viewStorage) {
        Validate.notNull(cleartool);
        this.cleartool = cleartool;
        this.viewStorage = viewStorage;
    }

    public abstract boolean checkout(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException;
    
    /**
     * @deprecated Use {@link #isViewValid(FilePath,String)} instead
     */
    @Deprecated
    public abstract boolean isViewValid(Launcher launcher, FilePath workspace, String viewTag) throws IOException, InterruptedException;

    public abstract boolean isViewValid(FilePath workspace, String viewTag) throws IOException, InterruptedException;

    public FilePath getUpdtFile() {
        throw new IllegalArgumentException("getUpdtFile() not implemented");
    }
    
    public ClearTool getCleartool() {
        return cleartool;
    }

    public ViewStorage getViewStorage() {
        return viewStorage;
    }
}
