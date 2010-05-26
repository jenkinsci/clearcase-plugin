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

import hudson.AbortException;
import hudson.FilePath;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public abstract class ClearToolExec implements ClearTool {

    private transient Pattern viewListPattern;
    protected ClearToolLauncher launcher;
    protected VariableResolver<String> variableResolver;

    public ClearToolExec(VariableResolver<String> variableResolver, ClearToolLauncher launcher) {
        this.variableResolver = variableResolver;
        this.launcher = launcher;
    }

    public ClearToolLauncher getLauncher() {
        return launcher;
    }

    protected abstract FilePath getRootViewPath(ClearToolLauncher launcher);

    public Reader lshistory(String format, Date lastBuildDate, String viewName, String branch, String[] viewPaths) throws IOException, InterruptedException {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'Z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lshistory");
        cmd.add("-all");
        cmd.add("-since", formatter.format(lastBuildDate).toLowerCase());
        cmd.add("-fmt", format);
        // cmd.addQuoted(format);
        if (StringUtils.isNotEmpty(branch)) {
            cmd.add("-branch", "brtype:" + branch);
        }
        cmd.add("-nco");

        FilePath viewPath = getRootViewPath(launcher).child(viewName);

        for (String path : viewPaths) {
            path = path.replace("\n", "").replace("\r", "");
            if (path.matches(".*\\s.*")) {
                cmd.addQuoted(path);
            } else {
                cmd.add(path);
            }
        }
        Reader returnReader = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, viewPath)) {
            returnReader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        }
        baos.close();

        return returnReader;
    }

    public Reader lsactivity(String activity, String commandFormat, String viewname) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsactivity");
        cmd.add("-fmt", commandFormat);
        cmd.add(activity);

        // changed the path from workspace to getRootViewPath to make Dynamic UCM work
        FilePath viewPath = getRootViewPath(launcher).child(viewname);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, viewPath);
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    public void mklabel(String viewName, String label) throws IOException, InterruptedException {
        throw new AbortException();
    }

    public List<String> lsview(boolean onlyActiveDynamicViews) throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyActiveDynamicViews);
        }
        return new ArrayList<String>();
    }

    @Override
    public String lscurrentview(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview").add("-cview").add("-s");
        
        List<IOException> exceptions = new ArrayList<IOException>();
        String output = runAndProcessOutput(cmd, null, getLauncher().getWorkspace().child(viewPath), true, exceptions);
        if (!exceptions.isEmpty()) {
            if (output.contains("cleartool: Error: Cannot get view info for current view: not a ClearCase object.")) {
                output = null;
            } else {
                throw exceptions.get(0);
            }
        }
        return output;
    }

    public boolean doesViewExist(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add(viewName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            return launcher.run(cmd.toCommandArray(), null, baos, null);
        } catch (IOException e) {
            return false;
        }
    }

    public List<String> lsvob(boolean onlyMounted) throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsvob");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyMounted);
        }
        return new ArrayList<String>();
    }

    public String pwv(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("pwv");
        cmd.add("-root");
        return runAndProcessOutput(cmd, null, getRootViewPath(launcher).child(viewName), false, null);
    }

    public String catcs(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("catcs");
        cmd.add("-tag", viewName);
        return runAndProcessOutput(cmd, null, null, false, null);
    }

    private List<String> parseListOutput(Reader consoleReader, boolean onlyStarMarked) throws IOException {
        List<String> views = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(consoleReader);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = viewListPattern.matcher(line);
            if (matcher.find() && matcher.groupCount() == 3) {
                if ((!onlyStarMarked) || (onlyStarMarked && matcher.group(1).equals("*"))) {
                    String vob = matcher.group(2);
                    int pos = Math.max(vob.lastIndexOf('\\'), vob.lastIndexOf('/'));
                    if (pos != -1) {
                        vob = vob.substring(pos + 1);
                    }
                    views.add(vob);
                }
            }
            line = reader.readLine();
        }
        reader.close();
        return views;
    }

    private Pattern getListPattern() {
        if (viewListPattern == null) {
            viewListPattern = Pattern.compile("(.)\\s*(\\S*)\\s*(\\S*)");
        }
        return viewListPattern;
    }

    public void mountVobs() throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mount");
        cmd.add("-all");

        try {
            launcher.run(cmd.toCommandArray(), null, baos, null);
        } catch (IOException ex) {
            logRedundantCleartoolError(cmd.toCommandArray(), ex);
        } finally {
            baos.close();
        }
    }

    public Properties getViewData(String viewName) throws IOException, InterruptedException {
        Properties resPrp = new Properties();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add("-l", viewName);


        Pattern uuidPattern = Pattern.compile("View uuid: (.*)");
        Pattern globalPathPattern = Pattern.compile("View server access path: (.*)");
        boolean res = true;
        IOException exception = null;
        List<IOException> exceptions = new ArrayList<IOException>();
        
        String output = runAndProcessOutput(cmd, null, null, true, exceptions);
        // handle the use case in which view doesn't exist and therefore error is thrown
        if (!exceptions.isEmpty() && !output.contains("No matching entries found for view")) {
            throw exceptions.get(0);
        }

        if (res && exception == null) {
            String[] lines = output.split("\n");
            for (String line : lines) {
                Matcher matcher = uuidPattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1)
                    resPrp.put("UUID", matcher.group(1));

                matcher = globalPathPattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1)
                    resPrp.put("STORAGE_DIR", matcher.group(1));
            }
        }

        return resPrp;
    }

    public void endView(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("endview");
        cmd.add(viewName);

        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to end view tag: " + output);
        }
    }

    public void rmviewtag(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-tag");
        cmd.add(viewName);

        String output = runAndProcessOutput(cmd, null, null, false, null);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + output);
        }

    }

    public void unregisterView(String uuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("unregister");
        cmd.add("-view");
        cmd.add("-uuid");
        cmd.add(uuid);

        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to unregister view: " + output);
        }

    }

    public void rmviewUuid(String viewUuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-avobs");
        cmd.add("-uuid");
        cmd.add(viewUuid);

        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

    }
    
    protected String runAndProcessOutput(ArgumentListBuilder cmd, InputStream in, FilePath workFolder, boolean catchExceptions, List<IOException> exceptions) throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            launcher.run(cmd.toCommandArray(), in, baos, workFolder);
        } catch (IOException e) {
            if (!catchExceptions) {
                throw e;
            } else {
                exceptions.add(e);
            }
        }
    
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
        return builder.toString();
    }
    
    public void logRedundantCleartoolError(String[] cmd, Exception ex) {
        getLauncher().getListener().getLogger().println("Redundant Cleartool Error ");

        if (cmd != null)
            getLauncher().getListener().getLogger().println("command: " + getLauncher().getCmdString(cmd));

        getLauncher().getListener().getLogger().println(ex.getMessage());
    }
}
