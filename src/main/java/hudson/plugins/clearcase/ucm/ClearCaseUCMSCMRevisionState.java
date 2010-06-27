/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
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
package hudson.plugins.clearcase.ucm;


import hudson.plugins.clearcase.AbstractClearCaseSCMRevisionState;
import hudson.plugins.clearcase.Baseline;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Represents the repository state at a given point of time
 */
public class ClearCaseUCMSCMRevisionState extends AbstractClearCaseSCMRevisionState {
    private final List<Baseline> baselines;
    private final String stream;
    
    public ClearCaseUCMSCMRevisionState(List<Baseline> baselines, Date buildTime, String stream) {
        super(buildTime);
        this.baselines = baselines;
        this.stream = stream;
    }
    
    public List<Baseline> getBaselines() {
        return Collections.unmodifiableList(baselines);
    }
    
    public String getStream() {
        return stream;
    }
    

}
