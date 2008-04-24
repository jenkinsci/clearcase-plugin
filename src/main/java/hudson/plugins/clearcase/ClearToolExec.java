package hudson.plugins.clearcase;

import hudson.AbortException;
import hudson.FilePath;
import hudson.util.ArgumentListBuilder;
import hudson.util.IOException2;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ClearToolExec implements ClearTool {

    private transient Pattern viewListPattern;
    protected transient String clearToolExec;
    protected ClearToolLauncher launcher;

    public ClearToolExec(ClearToolLauncher launcher, String clearToolExec) {
        this.launcher = launcher;
        this.clearToolExec = clearToolExec;
    }

    protected abstract FilePath getRootViewPath(ClearToolLauncher launcher);

    public List<ClearCaseChangeLogEntry> lshistory(Date lastBuildDate, String viewName,
            String branch, String vobPaths) throws IOException, InterruptedException {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM.HH:mm:ss");
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("lshistory");
        cmd.add("-r");
        cmd.add("-since", formatter.format(lastBuildDate).toLowerCase());
        cmd.add("-fmt", ClearToolHistoryParser.getLogFormat());
        if ((branch != null) && (branch.length() > 0)) {
            cmd.add("-branch", "brtype:" + branch);
        }
        cmd.add("-nco");

        FilePath viewPath = getRootViewPath(launcher).child(viewName);

        if (viewPath.exists()) {
            String[] vobNameArray = getVobNames(viewPath, vobPaths);
            for (String vob : vobNameArray) {
                cmd.add(vob);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            if (launcher.run(cmd.toCommandArray(), null, baos, viewPath)) {
                try {
                    ClearToolHistoryParser parser = new ClearToolHistoryParser();
                    return parser.parse(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
                } catch (ParseException pe) {
                    throw new IOException2("There was a problem parsing the history log.", pe);
                }
            }
        } else {
            launcher.getListener().fatalError(
                    "No view found at '" + viewPath + "'. Create the view by initiating a build manually.");
            throw new AbortException();
        }
        return new ArrayList<ClearCaseChangeLogEntry>();
    }

    private String[] getVobNames(FilePath viewPath, String vobPaths) throws IOException, InterruptedException {
        String[] vobNameArray;
        if ((vobPaths == null) || (vobPaths.trim().length() == 0)) {
            List<String> vobList = new ArrayList<String>();
            List<FilePath> subFilePaths = viewPath.list((FileFilter) null);
            if ((subFilePaths != null) && (subFilePaths.size() > 0)) {

                for (int i = 0; i < subFilePaths.size(); i++) {
                    if (subFilePaths.get(i).isDirectory()) {
                        vobList.add(subFilePaths.get(i).getName());
                    }
                }
            }
            vobNameArray = vobList.toArray(new String[0]);
        } else {
            // split by whitespace, except "\ "
            vobNameArray = vobPaths.split("(?<!\\\\)[ \\r\\n]+");
            // now replace "\ " to " ".
            for (int i = 0; i < vobNameArray.length; i++)
                vobNameArray[i] = vobNameArray[i].replaceAll("\\\\ ", " ");
        }
        return vobNameArray;
    }

    public void mklabel(String viewName, String label) throws IOException,
            InterruptedException {
        throw new AbortException();
    }

    public List<String> lsview(boolean onlyActiveDynamicViews) throws IOException,
            InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("lsview");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())),
                    onlyActiveDynamicViews);
        }
        return new ArrayList<String>();
    }

    public List<String> lsvob(boolean onlyMOunted) throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("lsvob");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())), onlyMOunted);
        }
        return new ArrayList<String>();
    }

    public String catcs(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add(clearToolExec);
        cmd.add("catcs");
        cmd.add("-tag", viewName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
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
        return "";
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
}
