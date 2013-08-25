package hudson.plugins.clearcase.command;

import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.google.common.annotations.VisibleForTesting;

/**
 * Executes a cleartool lshistory command
 *
 */
public class LsHistoryCommand implements CleartoolCommand {

    private static final Logger LOGGER = Logger.getLogger(LsHistoryCommand.class.getName());
    /**
     * the name of the branch to get history events for; if null then history events for all branches are listed
     */
    private String              branch;
    /**
     * get minor changes like labeling etc.
     */
    private boolean             considerMinorEvents;
    /**
     * format that should be used by the lshistory command
     */
    private String              format;
    /**
     * Lists the specified number of events, starting with the most recent. This option is mutually exclusive with useRecurse = true
     */
    private int                 numberOfLastEvents;
    /**
     * view paths that should be added to the lshistory command. The view paths must be relative.
     */
    private String[]            pathsInView;
    /**
     * lists events recorded since (that is, at or after) the specified date-time
     */
    private Date                since;
    /**
     * if true use -recurse command, else use -all command (default) this option is mutually exclusive with numberOfLastEvents (see
     * http://publib.boulder.ibm.com/infocenter/cchelp/v7r1m2/index.jsp?topic=%2Fcom.ibm.rational.clearcase.cc_ref.doc%2Ftopics%2Fct_lshistory.htm)
     */
    private boolean             useRecurse;
    /**
     * a path to the view where to execute the lshistory command
     */
    private FilePath            viewPath;

    public LsHistoryCommand branch(String branch) {
        setBranch(branch);
        return this;
    }

    public LsHistoryCommand considerMinorEvents() {
        setConsiderMinorEvents(true);
        return this;
    }

    @Override
    public CleartoolOutput execute(ClearToolLauncher launcher, TaskListener listener) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = generateCommandLine();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean successful = false;
        try {
            successful = launcher.run(cmd.toCommandArray(), null, baos, viewPath, true);
        } catch (IOException e) {
            LOGGER.log(Level.FINE, null, e);
            // We don't care if Clearcase returns an error code, we will process it afterwards
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "cmd={0} output={1}", new Object[] { cmd.toStringWithQuote(), baos });
        }
        return new CleartoolOutput(new ByteArrayInputStream(baos.toByteArray()), successful);
    }

    @VisibleForTesting
    void validate() {
        if (useRecurse && numberOfLastEvents != 0) {
            throw new IllegalArgumentException("useRecurse and numberOfLastEvents != 0 are mutually exclusive");
        }
        if (numberOfLastEvents < 0) {
            throw new IllegalArgumentException("numberOfLastEvents must be positive");
        }
        if (pathsInView == null) {
            pathsInView = new String[0];
        }
        Validate.notNull(viewPath, "You must provide a valid view path.");
    }
    
    
    @VisibleForTesting
    ArgumentListBuilder generateCommandLine() {
        validate();
        SimpleDateFormat formatter = getFormatter();

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lshistory");
        if (useRecurse) {
            cmd.add("-recurse");
        } else {
            cmd.add("-all");
        }
        if (since != null) {
            cmd.add("-since", formatter.format(since).toLowerCase());
        }

        if (numberOfLastEvents > 0) {
            cmd.add("-last", Integer.toString(numberOfLastEvents));
        }
        if (StringUtils.isNotEmpty(format)) {
            cmd.add("-fmt", format);
        }
        if (StringUtils.isNotEmpty(branch)) {
            cmd.add("-branch", "brtype:" + branch);
        }
        if (considerMinorEvents) {
            cmd.add("-minor");
        }
        cmd.add("-nco");
        for (String path : pathsInView) {
            path = path.replace("\n", "").replace("\r", "");
            if (path.matches(".*\\s.*")) {
                cmd.addQuoted(path);
            } else {
                cmd.add(path);
            }
        }
        return cmd;
    }

    @VisibleForTesting
    SimpleDateFormat getFormatter() {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'Z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter;
    }

    public LsHistoryCommand format(String format) {
        setFormat(format);
        return this;
    }

    public String getBranch() {
        return branch;
    }

    public String getFormat() {
        return format;
    }

    public int getNumberOfLastEvents() {
        return numberOfLastEvents;
    }

    public String[] getPathsInView() {
        return pathsInView;
    }

    public Date getSince() {
        return since;
    }

    public FilePath getViewPath() {
        return viewPath;
    }

    public boolean isConsiderMinorEvents() {
        return considerMinorEvents;
    }

    public boolean isUseRecurse() {
        return useRecurse;
    }

    public LsHistoryCommand numberOfLastEvents(int numberOfLastEvents) {
        setNumberOfLastEvents(numberOfLastEvents);
        return this;
    }

    public LsHistoryCommand pathsInView(String... pathsInView) {
        setPathsInView(pathsInView);
        return this;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public void setConsiderMinorEvents(boolean considerMinorEvents) {
        this.considerMinorEvents = considerMinorEvents;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setNumberOfLastEvents(int numberOfLastEvents) {
        this.numberOfLastEvents = numberOfLastEvents;
    }

    public void setPathsInView(String[] pathsInView) {
        this.pathsInView = pathsInView;
    }

    public void setSince(Date since) {
        this.since = since;
    }

    public void setUseRecurse(boolean useRecurse) {
        this.useRecurse = useRecurse;
    }

    public void setViewPath(FilePath viewPath) {
        this.viewPath = viewPath;
    }

    public LsHistoryCommand since(Date since) {
        setSince(since);
        return this;
    }

    public LsHistoryCommand useRecurse() {
        setUseRecurse(true);
        return this;
    }

    public LsHistoryCommand viewPath(FilePath viewPath) {
        setViewPath(viewPath);
        return this;
    }
}
