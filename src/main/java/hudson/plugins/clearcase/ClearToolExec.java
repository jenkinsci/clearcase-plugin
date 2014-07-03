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
import hudson.plugins.clearcase.command.CleartoolOutput;
import hudson.plugins.clearcase.command.LsHistoryCommand;
import hudson.plugins.clearcase.util.DeleteOnCloseFileInputStream;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

public abstract class ClearToolExec implements ClearTool {

    private static final CleartoolVersion CLEARTOOL_VERSION_7                          = new CleartoolVersion("7");
    @SuppressWarnings("unused")
    private static final Logger           LOGGER                                       = Logger.getLogger(ClearToolExec.class.getName());
    private static final Pattern          PATTERN_UNABLE_TO_REMOVE_DIRECTORY_NOT_EMPTY = Pattern
                                                                                               .compile("cleartool: Error: Unable to remove \"(.*)\": Directory not empty.");
    private static final Pattern          PATTERN_VIEW_ACCESS_PATH                     = Pattern.compile("View server access path: (.*)");
    private static final Pattern          PATTERN_VIEW_UUID                            = Pattern.compile("View uuid: (.*)");

    protected ClearToolLauncher           launcher;
    protected String                      optionalMkviewParameters;
    protected int                         endViewDelay;

    protected String                      updtFileName;
    protected VariableResolver<String>    variableResolver;
    private transient CleartoolVersion    version;
    private transient Pattern             viewListPattern;

    public ClearToolExec(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String optionalMkviewParameters) {
        this(variableResolver, launcher, optionalMkviewParameters, 0);
    }

    public ClearToolExec(VariableResolver<String> variableResolver, ClearToolLauncher launcher, String optionalMkviewParameters, int endViewDelay) {
        this.variableResolver = variableResolver;
        this.launcher = launcher;
        this.optionalMkviewParameters = optionalMkviewParameters;
        this.endViewDelay = endViewDelay;
    }

    @Override
    public String catcs(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("catcs");
        cmd.add("-tag", viewTag);
        return runAndProcessOutput(cmd, null, null, false, null, true);
    }

    @Override
    public Reader describe(String format, String objectSelector) throws IOException, InterruptedException {
        return describe(format, null, objectSelector);
    }

    @Override
    public Reader describe(String format, String viewPath, String objectSelector) throws IOException, InterruptedException {
        Validate.notNull(objectSelector);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("desc");
        if (StringUtils.isNotBlank(format)) {
            cmd.add("-fmt", format);
        }
        cmd.add(objectSelector);
        FilePath workingDirectory = null;
        if (viewPath != null) {
            workingDirectory = new FilePath(getRootViewPath(launcher), viewPath);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, workingDirectory, true);
        Reader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    @Override
    public Reader describe(String format, String[] objectSelectors) throws IOException, InterruptedException {
        Validate.notNull(objectSelectors);
        Validate.isTrue(objectSelectors.length > 0);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("desc");
        if (StringUtils.isNotBlank(format)) {
            cmd.add("-fmt", format);
        }
        for (String selector : objectSelectors) {
            cmd.add(selector);
        }
        FilePath workingDirectory = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, workingDirectory, true);
        Reader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    @Override
    public Reader diffbl(EnumSet<DiffBlOptions> type, String baseline1, String baseline2, String viewPath) throws IOException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("diffbl");
        if (type != null) {
            for (DiffBlOptions t : type) {
                cmd.add(getOption(t));
            }
        }
        cmd.add(baseline1);
        cmd.add(baseline2);

        // Output to a temporary file since the output can become quite large
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("cleartool-diffbl", null);
        } catch (IOException e) {
            throw new IOException("Couldn't create a temporary file", e);
        }
        OutputStream out = new FileOutputStream(tmpFile);

        FilePath workingDirectory = launcher.getWorkspace();
        if (viewPath != null) {
            workingDirectory = workingDirectory.child(viewPath);
        }
        try {
            launcher.run(cmd.toCommandArray(), null, out, workingDirectory, true);
        } catch (IOException e) {
        } catch (InterruptedException e) {
        }
        out.close();
        return new InputStreamReader(new DeleteOnCloseFileInputStream(tmpFile));
    }

    @Override
    public boolean doesSetcsSupportOverride() throws IOException, InterruptedException {
        try {
            return CLEARTOOL_VERSION_7.compareTo(version()) <= 0;
        } catch (CleartoolVersionParsingException e) {
            return false;
        }
    }

    @Override
    public boolean doesStreamExist(String streamSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lsstream");
        cmd.add("-short");
        cmd.add(streamSelector);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            launcher.run(cmd.toCommandArray(), null, baos, null, true);
        }
        catch (IOException e) {
            String cleartoolResult = baos.toString();
            if (cleartoolResult.contains("stream not found")) {
                return false;
            }
            throw e;
        }
        finally {
            baos.close();
        }
        return true;
    }

    @Override
    public boolean doesViewExist(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add(viewTag);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            return launcher.run(cmd.toCommandArray(), null, baos, null, true);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void endView(String viewTag) throws IOException, InterruptedException {
        endView(viewTag, false);
    }

    @Override
    public void endViewServer(String viewTag) throws IOException, InterruptedException {
        endView(viewTag, true);
    }

    private void endView(String viewTag, boolean server) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("endview");
        if (server) {
            cmd.add("-server");
        }
        cmd.add(viewTag);

        String output = runAndProcessOutput(cmd, null, null, false, null, true);
        if (endViewDelay > 0) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(endViewDelay));
        }
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to end view tag: " + output);
        }
    }

    @Override
    public ClearToolLauncher getLauncher() {
        return launcher;
    }

    @Override
    public Properties getViewData(String viewTag) throws IOException, InterruptedException {
        Properties resPrp = new Properties();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add("-l", viewTag);

        List<IOException> exceptions = new ArrayList<IOException>();

        String output = runAndProcessOutput(cmd, null, null, true, exceptions, true);
        // handle the use case in which view doesn't exist and therefore error is thrown
        if (!exceptions.isEmpty()) {
            if (!output.contains("No matching entries found for view")) {
                throw exceptions.get(0);
            }
            String[] lines = output.split("\n");
            for (String line : lines) {
                Matcher matcher = PATTERN_VIEW_UUID.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1)
                    resPrp.put("UUID", matcher.group(1));

                matcher = PATTERN_VIEW_ACCESS_PATH.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1)
                    resPrp.put("STORAGE_DIR", matcher.group(1));
            }
        }

        return resPrp;
    }

    @Override
    public boolean lock(String comment, String objectSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lock");
        cmd.add(objectSelector);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        launcher.run(cmd.toCommandArray(), null, baos, null, true);
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            return false;
        }
        baos.close();
        return true;
    }

    @Override
    public void logRedundantCleartoolError(String[] cmd, Exception ex) {
        getLauncher().getListener().getLogger().println("Redundant Cleartool Error ");

        if (cmd != null)
            getLauncher().getListener().getLogger().println("command: " + getLauncher().getCmdString(cmd));

        getLauncher().getListener().getLogger().println(ex.getMessage());
    }

    @Override
    public Reader lsactivity(String activity, String commandFormat, String viewPath) throws IOException, InterruptedException {
        return lsactivity(viewPath, "-fmt", commandFormat, activity);
    }

    @Override
    public Reader lsactivityIn(String streamSelector, String commandFormat, String viewPath) throws IOException, InterruptedException {
        return lsactivity(viewPath, "-fmt", commandFormat, "-in", streamSelector);
    }

    private Reader lsactivity(String viewPath, String... args) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsactivity");
        cmd.add(args);

        // changed the path from workspace to getRootViewPath to make Dynamic UCM work
        FilePath filePath = getRootViewPath(launcher).child(viewPath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, filePath, true);
        InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    @Override
    public String lsbl(String baselineName, String format) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsbl");
        if (StringUtils.isNotEmpty(format)) {
            cmd.add("-fmt");
            cmd.add(format);
        }
        cmd.add(baselineName);
        return runAndProcessOutput(cmd, null, null, false, null, true);
    }

    @Override
    public String lscurrentview(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview", "-cview", "-s");
        List<IOException> exceptions = new ArrayList<IOException>();
        String output = runAndProcessOutput(cmd, null, getLauncher().getWorkspace().child(viewPath), true, exceptions, true);
        if (!exceptions.isEmpty()) {
            if (output.contains("cleartool: Error: Cannot get view info for current view: not a ClearCase object.")) {
                output = null;
            } else {
                throw exceptions.get(0);
            }
        }
        return output;
    }

    @Override
    @Deprecated
    public Reader lshistory(String format, Date lastBuildDate, String viewPath, String branch, String[] pathsInView, boolean getMinor) throws IOException,
            InterruptedException {
        return lshistory(format, lastBuildDate, viewPath, branch, pathsInView, getMinor, false);
    }

    @Override
    public LsHistoryCommand lshistory() {
        return new LsHistoryCommand();
    }

    @Override
    @Deprecated
    public Reader lshistory(String format, Date lastBuildDate, String viewPath, String branch, String[] pathsInView, boolean getMinor, boolean useRecurse)
            throws IOException, InterruptedException {
        LsHistoryCommand lsHistory = lshistory();
        lsHistory.format(format).since(lastBuildDate).viewPath(getRootViewPath(launcher).child(viewPath)).branch(branch).pathsInView(pathsInView);
        lsHistory.setConsiderMinorEvents(getMinor);
        lsHistory.setUseRecurse(useRecurse);
        CleartoolOutput output = lsHistory.execute(launcher, launcher.getListener());
        return new InputStreamReader(output.getInputStream());
    }

    @Override
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
            launcher.run(cmd.toCommandArray(), null, baos, null, true);
        } catch (IOException e) {
            // We don't care if Clearcase returns an error code, we will process it afterwards
        }
        String output = baos.toString();
        baos.close();

        return output;
    }

    @Override
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
        return runAndProcessOutput(cmd, null, null, false, null, true);
    }

    @Override
    public List<String> lsview(boolean onlyActiveDynamicViews) throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null, true)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyActiveDynamicViews);
        }
        return new ArrayList<String>();
    }

    @Override
    public List<String> lsvob(boolean onlyMounted) throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsvob");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null, true)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyMounted);
        }
        return new ArrayList<String>();
    }

    @Override
    public List<Baseline> mkbl(String name, String viewTag, String comment, boolean fullBaseline, boolean identical, List<String> components,
            String dDependsOn, String aDependsOn) throws IOException, InterruptedException {
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
        if (CollectionUtils.isNotEmpty(components)) {
            cmd.add("-comp", StringUtils.join(components, ','));
        }

        if (StringUtils.isNotEmpty(dDependsOn)) {
            cmd.add("-ddepends_on", dDependsOn);
        }
        if (StringUtils.isNotEmpty(aDependsOn)) {
            cmd.add("-adepends_on", aDependsOn);
        }

        cmd.add(name);

        String output = runAndProcessOutput(cmd, null, null, false, null, true);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to make baseline, reason: " + output);
        }

        Pattern pattern = Pattern.compile("Created baseline \"(.+?)\" in component \"(.+?)\"");
        Matcher matcher = pattern.matcher(output);
        List<Baseline> createdBaselinesList = new ArrayList<Baseline>();
        while (matcher.find() && matcher.groupCount() == 2) {
            String baseline = matcher.group(1);
            String component = matcher.group(2);
            createdBaselinesList.add(new Baseline(baseline, component));
        }

        return createdBaselinesList;
    }

    @Override
    public void mklabel(String viewName, String label) throws IOException, InterruptedException {
        throw new AbortException();
    }

    @Override
    public void mkstream(String parentStream, String stream) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkstream");
        cmd.add("-in");
        cmd.add(parentStream);
        cmd.add(stream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        launcher.run(cmd.toCommandArray(), null, baos, null, true);
        baos.close();
    }

    @Override
    public void mkview(MkViewParameters parameters) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkview");
        if (parameters.getType() == ViewType.Snapshot) {
            cmd.add("-snapshot");
        }
        if (parameters.getStreamSelector() != null) {
            cmd.add("-stream");
            cmd.add(parameters.getStreamSelector());
        }
        cmd.add("-tag");
        cmd.add(parameters.getViewTag());

        boolean isMetadataLocationDefinedInAdditionalParameters = false;
        if (StringUtils.isNotEmpty(optionalMkviewParameters)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isMetadataLocationDefinedInAdditionalParameters = variabledResolvedParams.contains("-host") || variabledResolvedParams.contains("-vws");
        }

        if (!isMetadataLocationDefinedInAdditionalParameters) {
            cmd.add(parameters.getViewStorage().getCommandArguments(launcher.isUnix(), parameters.getViewTag()));
        }
        if (ViewType.Snapshot.equals(parameters.getType())) {
            cmd.add(parameters.getViewPath());
        }
        launcher.run(cmd.toCommandArray(), null, null, null, true);
    }

    /**
     * @see Use ClearToolExec#mkview(MkViewParameters) instead
     */
    @Override
    @Deprecated
    public void mkview(String viewPath, String viewTag, String streamSelector) throws IOException, InterruptedException {
        Validate.notEmpty(viewPath);
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

        if (StringUtils.isNotEmpty(optionalMkviewParameters)) {
            String variabledResolvedParams = Util.replaceMacro(optionalMkviewParameters, this.variableResolver);
            cmd.addTokenized(variabledResolvedParams);
            isOptionalParamContainsHost = optionalMkviewParameters.contains("-host");
        }

        if (!isOptionalParamContainsHost) {
            cmd.add(viewPath);
        }

        launcher.run(cmd.toCommandArray(), null, null, null, false);
    }

    /**
     * for dynamic views : viewPath == viewTag
     */
    @Override
    @Deprecated
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
            String separator = PathUtil.fileSepForOS(getLauncher().isUnix());
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

        launcher.run(cmd.toCommandArray(), null, null, null, false);
    }

    @Override
    public void mountVobs() throws IOException, InterruptedException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mount");
        cmd.add("-all");

        try {
            launcher.run(cmd.toCommandArray(), null, baos, null, true);
        } catch (IOException ex) {
            logRedundantCleartoolError(cmd.toCommandArray(), ex);
        } finally {
            baos.close();
        }
    }

    @Override
    public String pwv(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("pwv");
        cmd.add("-root");
        FilePath vp = getRootViewPath(launcher).child(viewPath);
        if (vp.exists()) {
            return runAndProcessOutput(cmd, null, vp, false, null, true);
        }
        return null;
    }

    @Override
    public void rebaseDynamic(String viewTag, String baseline) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rebase");
        cmd.add("-baseline", baseline);
        cmd.add("-view", viewTag);
        cmd.add("-complete");
        cmd.add("-force");
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    @Override
    public void recommendBaseline(String streamSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("chstream");
        cmd.add("-rec");
        cmd.add("-def");
        cmd.add(streamSelector);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    @Override
    public void rmtag(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmtag");
        cmd.add("-view");
        cmd.add(viewTag);
        String output = runAndProcessOutput(cmd, null, null, false, null, true);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + output);
        }
    }

    @Override
    public void rmview(String viewPath) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add(viewPath);

        FilePath workspace = launcher.getWorkspace();
        String output = runAndProcessOutput(cmd, null, workspace, false, null, true);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

        FilePath viewFilePath = workspace.child(viewPath);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println("Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    @Override
    public void rmviewtag(String viewTag) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-tag");
        cmd.add(viewTag);

        String output = runAndProcessOutput(cmd, null, null, false, null, true);

        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view tag: " + output);
        }

    }

    @Override
    public void rmviewUuid(String viewUuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-avobs");
        cmd.add("-uuid");
        cmd.add(viewUuid);

        String output = runAndProcessOutput(cmd, null, null, false, null, true);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to remove view: " + output);
        }

    }

    @Override
    public void setBaselinePromotionLevel(String baselineName, DefaultPromotionLevel promotionLevel) throws IOException, InterruptedException {
        setBaselinePromotionLevel(baselineName, promotionLevel.toString());
    }

    @Override
    public void setBaselinePromotionLevel(String baselineName, String promotionLevel) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("chbl");
        cmd.add("-c");
        cmd.add("Hudson set baseline to promotion level " + promotionLevel);
        cmd.add("-level");
        cmd.add(promotionLevel);

        cmd.add(baselineName);

        runAndProcessOutput(cmd, null, null, false, null, true);
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     *
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    @Override
    @Deprecated
    public void setcs(String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        setcs(null, viewPath, option, configSpec);
    }

    @Override
    public CleartoolUpdateResult setcs2(String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        return setcs(null, viewPath, option, configSpec);
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     *
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcsCurrent(String viewPath) throws IOException, InterruptedException {
        setcs2(viewPath, SetcsOption.CURRENT, null);
    }

    /**
     * Synchronize the dynamic view with the latest recommended baseline for the stream. 1. Set the config spec on the view (Removed call to chstream - based on
     * http://www.nabble.com/-clearcase-plugin--Use-of-chstream--generate-is-not-necessary-td25118511.html
     */
    @Override
    public void setcsTag(String viewTag, SetcsOption option, String configSpec) throws IOException, InterruptedException {
        setcs(viewTag, null, option, configSpec);
    }

    @Override
    public void startView(String viewTags) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("startview");
        cmd.addTokenized(viewTags);
        launcher.run(cmd.toCommandArray(), null, null, null, true);
    }

    @Override
    public void unlock(String comment, String objectSelector) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("unlock");
        cmd.add(objectSelector);

        launcher.run(cmd.toCommandArray(), null, null, null, true);
    }

    @Override
    public void unregisterView(String uuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("unregister");
        cmd.add("-view");
        cmd.add("-uuid");
        cmd.add(uuid);

        String output = runAndProcessOutput(cmd, null, null, false, null, true);
        if (output.contains("cleartool: Error")) {
            throw new IOException("Failed to unregister view: " + output);
        }

    }

    @Deprecated
    @Override
    public void update(String viewPath, String[] loadRules) throws IOException, InterruptedException {
        update2(viewPath, loadRules);
    }

    @Override
    public CleartoolUpdateResult update2(String viewPath, String[] loadRules) throws IOException, InterruptedException {
        FilePath workspace = getLauncher().getWorkspace();
        FilePath filePath = workspace.child(viewPath);
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("update");
        cmd.add("-force");
        cmd.add("-overwrite");
        if (!ArrayUtils.isEmpty(loadRules)) {
            cmd.add("-add_loadrules");
            for (String loadRule : loadRules) {
                cmd.add(fixLoadRule(loadRule));
            }
        }
        List<IOException> exceptions = new ArrayList<IOException>();
        PrintStream logger = getLauncher().getListener().getLogger();
        logger.println("Running cleartool update, this operation may take a while");
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes\nyes\n".getBytes()), filePath, true, exceptions, false);
        FilePath logFile = extractUpdtFile(workspace, output);
        displayLogFile(logger, logFile);
        if (!exceptions.isEmpty()) {
            handleHijackedDirectoryCCBug(viewPath, filePath, exceptions, output);
        }
        return new CleartoolUpdateResult(logFile);
    }

    @Override
    public CleartoolVersion version() throws IOException, InterruptedException, CleartoolVersionParsingException {
        if (version == null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            InputStreamReader reader = null;
            ByteArrayInputStream is = null;
            ArgumentListBuilder cmd = new ArgumentListBuilder();
            cmd.add("-version");
            try {
                launcher.run(cmd.toCommandArray(), null, baos, null, true);
                is = new ByteArrayInputStream(baos.toByteArray());
                reader = new InputStreamReader(is);
                version = CleartoolVersion.parseCmdOutput(reader);
            } finally {
                org.apache.commons.io.IOUtils.closeQuietly(reader);
                org.apache.commons.io.IOUtils.closeQuietly(is);
                org.apache.commons.io.IOUtils.closeQuietly(baos);
            }
        }
        return version;
    }

    private static final String FILE_DESCRIPTOR_LEAK = "Process leaked file descriptors. See http://wiki.jenkins-ci.org/display/JENKINS/Spawning+processes+from+build for more information";

    /**
     * @param launcher
     * @return The root view path
     */
    protected abstract FilePath getRootViewPath(ClearToolLauncher launcher);

    protected String runAndProcessOutput(ArgumentListBuilder cmd, InputStream in, FilePath workFolder, boolean catchExceptions, List<IOException> exceptions,
            boolean log) throws IOException, InterruptedException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            launcher.run(cmd.toCommandArray(), in, baos, workFolder, log);
        } catch (IOException e) {
            if (!catchExceptions) {
                throw e;
            }
            exceptions.add(e);
        }
        byte[] byteArray = baos.toByteArray();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        baos.close();
        String line = reader.readLine();
        StringBuilder builder = new StringBuilder();
        while (line != null) {
            line = StringUtils.removeEnd(line, FILE_DESCRIPTOR_LEAK);
            if (StringUtils.isNotEmpty(line)) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(line);
            }
            line = reader.readLine();
        }
        reader.close();
        return builder.toString();
    }

    private void displayLogFile(PrintStream logger, FilePath logFile) throws IOException, InterruptedException {
        if (logFile != null && logFile.exists()) {
            InputStream stream = logFile.read();
            try {
                LineIterator it = org.apache.commons.io.IOUtils.lineIterator(stream, "UTF-8");
                while (it.hasNext()) {
                    logger.println(it.nextLine());
                }
            } finally {
                org.apache.commons.io.IOUtils.closeQuietly(stream);
            }

        }
    }

    private FilePath extractUpdtFile(FilePath workspace, String output) {
        Pattern updtPattern = Pattern.compile("Log has been written to \"(.*)\".*");
        String[] lines = output.split("\n");
        String fileName = null;
        for (String line : lines) {
            Matcher matcher = updtPattern.matcher(line);
            if (matcher.find() && matcher.groupCount() == 1) {
                fileName = matcher.group(1);
            }
        }
        if (fileName == null) {
            return null;
        }
        return new FilePath(workspace, fileName);
    }

    private String fixLoadRule(String loadRule) {
        if (StringUtils.isBlank(loadRule)) {
            return loadRule;
        }
        // Remove leading file separator, we don't need it when using add_loadrules
        String quotedLR = ConfigSpec.cleanLoadRule(loadRule, getLauncher().isUnix());
        if (isQuoted(quotedLR)) {
            return "\"" + quotedLR.substring(2);
        }
        return quotedLR.substring(1);
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
     * Work around for a CCase bug with hijacked directories: in the case where a directory was hijacked, cleartool is not able to remove it when it is not
     * empty, we detect this and remove the hijacked directories explicitly, then we relaunch the update.
     *
     * @param viewPath
     * @param filePath
     * @param exceptions
     * @param output
     * @throws IOException
     * @throws InterruptedException
     */
    private void handleHijackedDirectoryCCBug(String viewPath, FilePath filePath, List<IOException> exceptions, String output) throws IOException,
            InterruptedException {
        String[] lines = output.split("\n");
        int nbRemovedDirectories = 0;
        PrintStream logger = getLauncher().getListener().getLogger();
        for (String line : lines) {
            Matcher matcher = PATTERN_UNABLE_TO_REMOVE_DIRECTORY_NOT_EMPTY.matcher(line);
            if (matcher.find() && matcher.groupCount() == 1) {
                String directory = matcher.group(1);
                logger.println("Forcing removal of hijacked directory: " + directory);
                filePath.child(directory).deleteRecursive();
                nbRemovedDirectories++;
            }
        }
        if (nbRemovedDirectories == 0) {
            // Exception was unrelated to hijacked directories, throw it
            throw exceptions.get(0);
        }
        // We forced some hijacked directory removal, relaunch update
        logger.println("Relaunching update after removal of hijacked directories");
        update2(viewPath, null);
    }

    private boolean isQuoted(String quotedLR) {
        return quotedLR.charAt(0) == '"' && quotedLR.endsWith("\"");
    }

    private List<String> parseListOutput(Reader consoleReader, boolean onlyStarMarked) throws IOException {
        List<String> views = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(consoleReader);
        try {
            String line = reader.readLine();
            while (line != null) {
                line = StringUtils.removeEnd(line, FILE_DESCRIPTOR_LEAK);
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
        } finally {
            org.apache.commons.io.IOUtils.closeQuietly(reader);
        }
        return views;
    }

    private CleartoolUpdateResult setcs(String viewTag, String viewPath, SetcsOption option, String configSpec) throws IOException, InterruptedException {
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
        if (doesSetcsSupportOverride()) {
            cmd.add("-overwrite");
        }
        FilePath configSpecFile = null;
        if (option == SetcsOption.CONFIGSPEC) {
            configSpecFile = launcher.getWorkspace().createTextTempFile("configspec", ".txt", configSpec);
            cmd.add(PathUtil.convertPathForOS(configSpecFile.absolutize().getRemote(), launcher.isUnix()));
        }
        FilePath workingDirectory = null;
        if (viewPath != null) {
            workingDirectory = new FilePath(getRootViewPath(launcher), viewPath);
        }
        PrintStream logger = getLauncher().getListener().getLogger();
        logger.println("Running cleartool setcs, this operation may take a while");
        String output = runAndProcessOutput(cmd, new ByteArrayInputStream("yes\nyes\n".getBytes()), workingDirectory, false, null, false);
        if (configSpecFile != null) {
            configSpecFile.delete();
        }
        if (output.contains("cleartool: Warning: An update is already in progress for view")) {
            throw new IOException("View update failed: " + output);
        }
        FilePath logFile = extractUpdtFile(launcher.getWorkspace(), output);
        displayLogFile(logger, logFile);
        return new CleartoolUpdateResult(logFile);
    }
}
