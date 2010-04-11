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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class ClearToolDynamic extends ClearToolExec {

    private transient String viewDrive;
    private String optionalMkviewParameters;

    public ClearToolDynamic(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String viewDrive, String optionalMkviewParameters) {
        super(variableResolver, launcher);
        this.viewDrive = viewDrive;
        this.optionalMkviewParameters = optionalMkviewParameters;
    }

    @Override
    protected FilePath getRootViewPath(ClearToolLauncher launcher) {
        return new FilePath(launcher.getWorkspace().getChannel(), viewDrive);
    }

    /**
     * The view tag does need not be active. However, it is possible to set the config spec of a dynamic view from
     * within a snapshot view using "-tag view-tag"
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException, InterruptedException {
        if (configSpec == null) {
            configSpec = "";
        }

        FilePath configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);
        String csLocation = "";
        if (!configSpec.equals("")) {
            csLocation = configSpecFile.getRemote();
        }

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        cmd.add("-tag");
        cmd.add(viewName);
        if (!csLocation.equals("")) {
            cmd.add(csLocation);
        } else {
            cmd.add("-current");
        }
        launcher.run(cmd.toCommandArray(), null, null, getRootViewPath(launcher).child(viewName));

        configSpecFile.delete();
    }

    public void mkview(String viewName, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException {

        boolean isOptionalParamContainsHost = false;

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
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

        // add the default storage directory only if gpath/hpath are not set (only for windows)
        if (!isOptionalParamContainsHost && defaultStorageDir != null && defaultStorageDir.length() > 0) {
            Integer rndNum = new Random().nextInt();
            String seperator = PathUtil.fileSepForOS(getLauncher().getLauncher().isUnix());
            String viewStorageDir = defaultStorageDir + seperator + viewName + "." + rndNum.toString();
            cmd.add(viewStorageDir);
        }

        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support mkview (String, String)");
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support rmview");
    }

    public void rmviewtag(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmtag");
        cmd.add("-view");
        cmd.add(viewName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, null);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
        baos.close();
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append(line);
            line = reader.readLine();
        }
        reader.close();

        if (builder.toString().contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + builder.toString());
        }

    }

    public void update(String viewName, String[] loadRules) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support update");
    }

    public void syncronizeViewWithStream(String viewName, String stream) throws IOException, InterruptedException {
        launcher.getListener().fatalError("Dynamic view does not support syncronize");
    }

    public void startView(String viewTags) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("startview");
        cmd.addTokenized(viewTags);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

}
