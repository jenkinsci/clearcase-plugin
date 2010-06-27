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
import hudson.Util;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public abstract class ClearToolExec implements ClearTool {

    private transient Pattern viewListPattern;
    protected ClearToolLauncher launcher;
    protected VariableResolver<String> variableResolver;
    protected String optionalMkviewParameters;

    public ClearToolExec(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String optionalMkviewParameters) {
        this.variableResolver = variableResolver;
        this.launcher = launcher;
        this.optionalMkviewParameters = optionalMkviewParameters;
    }

    public String catcs(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("catcs");
        cmd.add("-tag", viewTag);
        return runAndProcessOutput(cmd, null, null, false, null);
    }

    @Override
    public Reader describe(String format, String objectSelector) throws IOException, InterruptedException {
        Validate.notNull(objectSelector);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("desc");
        if (StringUtils.isNotBlank(format)) {
            cmd.add("-fmt", format);
        }
        cmd.add(objectSelector);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, null);
        Reader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    @Override
    public Reader diffbl(EnumSet<DiffBlOptions> type, String baseline1, String baseline2, String viewPath) {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("diffbl");
        for (DiffBlOptions t : type) {
            cmd.add(getOption(t));
        }
        cmd.add(baseline1);
        cmd.add(baseline2);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            launcher.run(cmd.toCommandArray(), null, baos, launcher.getWorkspace().child(viewPath));
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        return new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
    }

    public boolean doesViewExist(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add(viewTag);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            return launcher.run(cmd.toCommandArray(), null, baos, null);
        } catch (IOException e) {
            return false;
        }
    }

    public void endView(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("endview");
        cmd.add(viewTag);

        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to end view tag: " + output);
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

    public ClearToolLauncher getLauncher() {
        return launcher;
    }

    private Pattern getListPattern() {
        if (viewListPattern == null) {
            viewListPattern = Pattern.compile("(.)\\s*(\\S*)\\s*(\\S*)");
        }
        return viewListPattern;
    }

    private String getOption(DiffBlOptions type) {
        switch (type) {
        case ACTIVITIES:
            return "-activities";
        case VERSIONS:
            return "-versions";
        case BASELINES:
            return "-baselines";
        case FIRST_ONLY:
            return "-first_only";
        case NRECURSE:
            return "-nrecurse";
        default:
            throw new IllegalArgumentException(type + " is not supported by this implementation");
        }
    }

    private String getOption(SetcsOption option) {
        switch (option) {
        case CONFIGSPEC:
            return null;
        case CURRENT:
            return "-current";
        case STREAM:
            return "-stream";
        default:
            throw new IllegalArgumentException(option + " is not supported by this implementation");
        }
    }

    /**
     * @param launcher
     * @return The root view path
     */
    protected abstract FilePath getRootViewPath(ClearToolLauncher launcher);
    
    public Properties getViewData(String viewTag) throws IOException, InterruptedException {
        Properties resPrp = new Properties();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add("-l", viewTag);

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

    public boolean lock(String comment, String objectSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lock");
        cmd.add(objectSelector);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        launcher.run(cmd.toCommandArray(), null, baos, null);
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            return false;
        }
        baos.close();
        return true;
    }

    public void logRedundantCleartoolError(String[] cmd, Exception ex) {
        getLauncher().getListener().getLogger().println("Redundant Cleartool Error ");

        if (cmd != null)
            getLauncher().getListener().getLogger().println("command: " + getLauncher().getCmdString(cmd));

        getLauncher().getListener().getLogger().println(ex.getMessage());
    }

    public Reader lsactivity(String activity, String commandFormat, String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsactivity");
        cmd.add("-fmt", commandFormat);
        cmd.add(activity);

        // changed the path from workspace to getRootViewPath to make Dynamic UCM work
        FilePath filePath = getRootViewPath(launcher).child(viewPath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, filePath);
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }
    
    public String lsbl(String baselineName, String format) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsbl");
        if (StringUtils.isNotEmpty(format)) {
            cmd.add("-fmt");
            cmd.add(format);
        }
        cmd.add(baselineName);
        return runAndProcessOutput(cmd, null, null, false, null);
    }

    @Override
    public String lscurrentview(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview", "-cview", "-s");
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

    public Reader lshistory(String format, Date lastBuildDate, String viewPath, String branch, String[] pathsInView) throws IOException, InterruptedException {
        Validate.notNull(pathsInView);
        Validate.notNull(viewPath);
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

        FilePath filePath = getRootViewPath(launcher).child(viewPath);

        for (String path : pathsInView) {
            path = path.replace("\n", "").replace("\r", "");
            if (path.matches(".*\\s.*")) {
                cmd.addQuoted(path);
            } else {
                cmd.add(path);
            }
        }
        Reader returnReader = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            launcher.run(cmd.toCommandArray(), null, baos, filePath);
        } catch (IOException e) {
            // We don't care if Clearcase returns an error code, we will process it afterwards
        }
        returnReader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();

        return returnReader;
    }
    
    public String lsproject(String viewTag, String format) throws InterruptedException, IOException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsproject");
        cmd.add("-view");
        cmd.add(viewTag);
        if (StringUtils.isNotEmpty(format)) {
            cmd.add("-fmt");
            cmd.add(format);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            launcher.run(cmd.toCommandArray(), null, baos, null);
        } catch (IOException e) {
            // We don't care if Clearcase returns an error code, we will process it afterwards
        }
        String output = baos.toString();
        baos.close();

        return output;
    }
    
    public String lsstream(String stream, String viewTag, String format) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsstream");
        if (StringUtils.isNotEmpty(viewTag)) {
            cmd.add("-view", viewTag);
        }
        cmd.add("-fmt");
        cmd.add(format);
        if (StringUtils.isNotEmpty(stream)) {
            cmd.add(stream);
        }
        return runAndProcessOutput(cmd, null, null, false, null);
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
    
    public List<String> mkbl(String name, String viewTag, String comment, boolean fullBaseline, boolean identical, List<String> components, String dDependsOn, String aDependsOn) throws IOException, InterruptedException {
        Validate.notNull(viewTag);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkbl");
        if (identical) {
            cmd.add("-identical");
        }
        if (StringUtils.isNotBlank(comment)) {
            cmd.add("-comment", comment);
        }
        if (fullBaseline) {
            cmd.add("-full");
        } else {
            cmd.add("-incremental");
        }

        if (StringUtils.isNotEmpty(viewTag)) {
            cmd.add("-view", viewTag);
        }

        // Make baseline only for specified components
        cmd.add("-comp", StringUtils.join(components, ','));
        
        if (StringUtils.isNotEmpty(dDependsOn)) {
            cmd.add("-ddepends_on", dDependsOn);
        }
        if (StringUtils.isNotEmpty(aDependsOn)) {
            cmd.add("-adepends_on", aDependsOn);
        }
        
        cmd.add(name);

        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to make baseline, reason: " + output);
        }

        Pattern pattern = Pattern.compile("Created baseline \".+?\"");
        Matcher matcher = pattern.matcher(output);
        List<String> createdBaselinesList = new ArrayList<String>();
        while (matcher.find()) {
            String match = matcher.group();
            String newBaseline = match.substring(match.indexOf("\"") + 1, match.length() - 1);
            createdBaselinesList.add(newBaseline);
        }

        return createdBaselinesList;
    }

    public void mklabel(String viewName, String label) throws IOException, InterruptedException {
        throw new AbortException();
    }

    public void mkview(String viewPath, String viewTag, String streamSelector) throws IOException, InterruptedException {
        boolean isOptionalParamContainsHost = false;
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
        cmd.add("-snapshot");
        if (streamSelector != null) {
            cmd.add("-stream");
            cmd.add(streamSelector);
        }
        cmd.add("-tag");
        cmd.add(viewTag);

        if ((optionalMkviewParameters != null) && (optionalMkviewParameters.length() > 0)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");
        }

        if (!isOptionalParamContainsHost) {
            cmd.add(viewPath);
        }

        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    /**
     * for dynamic views : viewPath == viewTag
     */
    public void mkview(String viewPath, String viewTag, String streamSelector, String defaultStorageDir) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkview");
        if (streamSelector != null) {
            cmd.add("-stream");
            cmd.add(streamSelector);
        }
        cmd.add("-tag");
        cmd.add(viewTag);

        boolean isOptionalParamContainsHost = false;
        if (StringUtils.isNotEmpty(optionalMkviewParameters)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");
        }

        // add the default storage directory only if gpath/hpath are not set (only for windows)
        if (!isOptionalParamContainsHost && StringUtils.isNotEmpty(defaultStorageDir)) {
            String separator = PathUtil.fileSepForOS(getLauncher().getLauncher().isUnix());
            String viewStorageDir = defaultStorageDir + separator + viewTag;
            String base = viewStorageDir;
            FilePath fp = new FilePath(getLauncher().getLauncher().getChannel(), viewStorageDir);
            int i = 1;
            while (fp.exists()) {
                viewStorageDir = base + "." + i++;
                fp = new FilePath(getLauncher().getLauncher().getChannel(), viewStorageDir);
                if (i == Integer.MAX_VALUE) {
                    throw new IOException("Cannot determine a view storage dir.");
                }
            }
            cmd.add(viewStorageDir);
        }

        launcher.run(cmd.toCommandArray(), null, null, null);
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
    
    public void setBaselinePromotionLevel(String baselineName, DefaultPromotionLevel promotionLevel) throws IOException, InterruptedException {
        setBaselinePromotionLevel(baselineName, promotionLevel.toString());
    }
    
    public void setBaselinePromotionLevel(String baselineName, String promotionLevel) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chbl");
        cmd.add("-c");
        cmd.add("Hudson set baseline to promotion level " + promotionLevel);
        cmd.add("-level");
        cmd.add(promotionLevel);

        cmd.add(baselineName);

        runAndProcessOutput(cmd, null, null, false, null);
    }
    
    public String pwv(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("pwv");
        cmd.add("-root");
        FilePath vp = getRootViewPath(launcher).child(viewPath);
        if (vp.exists()) {
            return runAndProcessOutput(cmd, null, vp, false, null);
        } else {
            return null;
        }
    }
    
    public void rebaseDynamic(String viewTag, String baseline) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rebase");
        cmd.add("-baseline");
        cmd.add(baseline);
        cmd.add("-view");
        cmd.add(viewTag);
        cmd.add("-complete");
        launcher.run(cmd.toCommandArray(), null, null, null);
    }
    
    public void recommendBaseline(String streamSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("chstream");
        cmd.add("-rec");
        cmd.add("-def");
        cmd.add(streamSelector);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    public void rmview(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add(viewPath);

        FilePath workspace = launcher.getWorkspace();
        String output = runAndProcessOutput(cmd, null, workspace, false, null);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

        FilePath viewFilePath = workspace.child(viewPath);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println("Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    public void rmviewtag(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-tag");
        cmd.add(viewTag);

        String output = runAndProcessOutput(cmd, null, null, false, null);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + output);
        }

    }
    
    public void rmtag(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmtag");
        cmd.add("-view");
        cmd.add(viewTag);
        String output = runAndProcessOutput(cmd, null, null, false, null);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + output);
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

    protected String runAndProcessOutput(ArgumentListBuilder cmd, InputStream in, FilePath workFolder, boolean catchExceptions, List<IOException> exceptions)
            throws IOException, InterruptedException {
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

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    @Override
    public void setcs(String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        setcs(null, viewPath, option, configSpec);
    }

    private void setcs(String viewTag, String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        if (option == SetcsOption.CONFIGSPEC) {
            Validate.notNull(configSpec, "Using option CONFIGSPEC, you must provide a non-null config spec");
        } else {
            Validate.isTrue(configSpec == null, "Not using option CONFIGSPEC, you must provide a null config spec");
        }
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
        if (viewTag != null) {
            cmd.add("-tag");
            cmd.add(viewTag);
        }
        String optionStr = getOption(option);
        if (optionStr != null) {
            cmd.add(optionStr);
        }
        FilePath configSpecFile = null;
        if (option == SetcsOption.CONFIGSPEC) {
            configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);
            cmd.add(PathUtil.convertPathForOS(".." + File.separator + configSpecFile.getName(), launcher.getLauncher().isUnix()));
        }
        FilePath workingDirectory = null;
        if (viewPath != null) {
            workingDirectory = new FilePath(getRootViewPath(launcher), viewPath);
        }
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), workingDirectory, false, null);
        if (configSpecFile != null) {
            configSpecFile.delete();
        }
        
        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * 
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcsCurrent(String viewPath) throws IOException, InterruptedException {
        setcs(viewPath, SetcsOption.CURRENT, null);
    }
    
    /**
     * Synchronize the dynamic view with the latest recommended baseline for the stream. 1. Set the config spec on the
     * view (Removed call to chstream - based on
     * http://www.nabble.com/-clearcase-plugin--Use-of-chstream--generate-is-not-necessary-td25118511.html
     */
    public void setcsTag(String viewTag, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        setcs(viewTag, null, option, configSpec);
    }

    public void startView(String viewTags) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("startview");
        cmd.addTokenized(viewTags);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }
    
    public void unlock(String comment, String objectSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("unlock");
        cmd.add(objectSelector);

        launcher.run(cmd.toCommandArray(), null, null, null);
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

    @Override
    public void update(String viewPath, String[] loadRules) throws IOException, InterruptedException {
        FilePath filePath = getLauncher().getWorkspace().child(viewPath);
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

        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes".getBytes()), filePath, false, null);

        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
    }
}
