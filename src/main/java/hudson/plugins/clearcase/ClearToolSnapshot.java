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

import hudson.FilePath;
import hudson.Util;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import org.apache.commons.lang.ArrayUtils;

public class ClearToolSnapshot extends ClearToolExec {

    private String optionalMkviewParameters;

    public ClearToolSnapshot(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        super(variableResolver, launcher);
    }

    public ClearToolSnapshot(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String optionalParameters) {
        this(variableResolver, launcher);
        this.optionalMkviewParameters = optionalParameters;
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException, InterruptedException {
        if (configSpec == null) {
            configSpec = "";
        }

        FilePath workspace = launcher.getWorkspace();
        FilePath configSpecFile = workspace.createTextTempFile("configspec", ".txt", configSpec);
        String csLocation = "";

        if (!configSpec.equals("")) {
            csLocation = ".." + File.separatorChar + configSpecFile.getName();
            csLocation = PathUtil.convertPathForOS(csLocation, launcher.getLauncher());
        }

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        if (!csLocation.equals("")) {
            cmd.add(csLocation);
        } else {
            cmd.add("-current");
        }
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), workspace.child(viewName), false, null);
        configSpecFile.delete();

        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcsCurrent(String viewName) throws IOException, InterruptedException {
        FilePath workspace = launcher.getWorkspace();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        cmd.add("-current");
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), workspace.child(viewName), false, null);

        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        boolean isOptionalParamContainsHost = false;
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
        cmd.add("-snapshot");
        if (streamSelector != null) {
            cmd.add("-stream");
            cmd.add(streamSelector);
        }
        cmd.add("-tag");
        cmd.add(viewName);

        if ((optionalMkviewParameters != null) && (optionalMkviewParameters.length() > 0)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");
        }

        if (!isOptionalParamContainsHost)
            cmd.add(viewName);

        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    public void mkview(String viewName, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support mkview (String, String)");
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add(viewName);

        String output = runAndProcessOutput(cmd, null, null, false, null);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

        FilePath viewFilePath = launcher.getWorkspace().child(viewName);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println("Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    @Override
    public void update(String viewName, String[] loadRules) throws IOException, InterruptedException {
        FilePath viewPath = getLauncher().getWorkspace().child(viewName);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("update");
        cmd.add("-force");
        cmd.add("-overwrite");
        cmd.add("-log", "NUL");
        if (!ArrayUtils.isEmpty(loadRules)) {
            cmd.add("-add_loadrules");
            for (String loadRule : loadRules) {
                cmd.add(fixLoadRule(loadRule));
            }
        }
        
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), viewPath, false, null);
        
        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    private String fixLoadRule(String loadRule) {
        // Remove leading file separator, we don't need it when using add_loadrules
        String quotedLR = ConfigSpec.cleanLoadRule(loadRule, getLauncher().getLauncher().isUnix());
        if (quotedLR.startsWith("\"") && quotedLR.endsWith("\"")) {
            return "\"" + quotedLR.substring(2);
        } else {
            return quotedLR.substring(1);
        }
    }

    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return launcher.getWorkspace();
    }

    public void startView(String viewTag) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support startview");
    }

    public void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Snapshot view does not support syncronize");
    }
}
