/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer, Vincent Latombe
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

import hudson.Extension;
import hudson.Plugin;

/**
 * ClearCase plugin.
 * 
 * @author Erik Ramfelt
 */
public class PluginImpl extends Plugin {

    @Extension
    public static final ClearCaseSCM.ClearCaseScmDescriptor BASE_DESCRIPTOR = new ClearCaseSCM.ClearCaseScmDescriptor();
    @Extension
    public static final ClearCaseUcmSCM.ClearCaseUcmScmDescriptor UCM_DESCRIPTOR = new ClearCaseUcmSCM.ClearCaseUcmScmDescriptor(BASE_DESCRIPTOR);
    @Extension
    public static final ClearCasePublisher.DescriptorImpl PUBLISHER_DESCRIPTOR = ClearCasePublisher.DescriptorImpl.DESCRIPTOR;

    public static ClearCaseSCM.ClearCaseScmDescriptor getDescriptor() {
        return BASE_DESCRIPTOR;
    }
}
