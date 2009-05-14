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
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.IOException;

public class ClearToolDynamic extends ClearToolExec {

    private transient String viewDrive;

    public ClearToolDynamic(VariableResolver variableResolver, ClearToolLauncher launcher, String viewDrive) {
        super(variableResolver, launcher);
        this.viewDrive = viewDrive;
    }

    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return new FilePath(launcher.getWorkspace().getChannel(), viewDrive);
    }

    /**
     * The view tag does need not be active.
     * However, it is possible to set the config spec of a dynamic view from within a snapshot view
     * using "-tag view-tag" 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException,
            InterruptedException {
        FilePath configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        cmd.add("-tag");
        cmd.add(viewName);
        cmd.add(configSpecFile.getName());
        launcher.run(cmd.toCommandArray(), null, null, null);

        configSpecFile.delete();
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support mkview");        
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support rmview");
    }

    public void rmviewtag(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support rmviewtag");
    }

    public void update(String viewName, String loadRules) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support update");
    }
    
    public void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support syncronize");
    }

    public void startView(String viewTags)  throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("startview");
        cmd.addTokenized(viewTags);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

}
