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

import hudson.AbortException;
import hudson.FilePath;
import hudson.plugins.clearcase.util.PathUtil;
import hudson.util.ArgumentListBuilder;
import hudson.util.VariableResolver;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ClearToolExec implements ClearTool {

    private transient Pattern viewListPattern;
    protected ClearToolLauncher launcher;
    protected VariableResolver variableResolver;

    public ClearToolExec(VariableResolver variableResolver,
                         ClearToolLauncher launcher) {
        this.variableResolver = variableResolver;
        this.launcher = launcher;
    }

    public ClearToolLauncher getLauncher() {
        return launcher;
    }

    protected abstract FilePath getRootViewPath(ClearToolLauncher launcher);

    public Reader lshistory(String format, Date lastBuildDate, String viewName,
                            String branch, String[] viewPaths) throws IOException,
                                                                      InterruptedException {
        SimpleDateFormat formatter = new SimpleDateFormat("d-MMM-yy.HH:mm:ss'UTC'Z", Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lshistory");
        cmd.add("-all");
        cmd.add("-since", formatter.format(lastBuildDate).toLowerCase());
        cmd.add("-fmt", format);
        //              cmd.addQuoted(format);
        if ((branch != null) && (branch.length() > 0)) {
            cmd.add("-branch", "brtype:" + branch);
        }
        cmd.add("-nco");
        
        FilePath viewPath = getRootViewPath(launcher).child(viewName);
        
        for (String path : viewPaths) {
            path = path.replace("\n","").replace("\r","");
            if (path.matches(".*\\s.*")) {
                cmd.addQuoted(path);
            }
            else {
                cmd.add(path);
            }
        }
        Reader returnReader = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, viewPath)) {
            returnReader = new InputStreamReader(new ByteArrayInputStream(baos
                                                                          .toByteArray()));
        }
        baos.close();
        
        return returnReader;
    }

    public Reader lsactivity(String activity, String commandFormat,
                             String viewname) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsactivity");
        cmd.add("-fmt", commandFormat);
        cmd.add(activity);
        
        // changed the path from workspace to getRootViewPath to make Dynamic UCM work
        FilePath viewPath = getRootViewPath(launcher).child(viewname);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        launcher.run(cmd.toCommandArray(), null, baos, viewPath);
        InputStreamReader reader = new InputStreamReader(
                                                         new ByteArrayInputStream(baos.toByteArray()));
        baos.close();
        return reader;
    }

    public void mklabel(String viewName, String label) throws IOException,
                                                              InterruptedException {
        throw new AbortException();
    }

    public List<String> lsview(boolean onlyActiveDynamicViews)
        throws IOException, InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(
                                                         new ByteArrayInputStream(baos.toByteArray())),
                                   onlyActiveDynamicViews);
        }
        return new ArrayList<String>();
    }

    public boolean doesViewExist(String viewName)
        throws IOException, InterruptedException {
        List<String> views = lsview(false);

        for (String v : views) {
            if (v.equals(viewName))
                return true;
        }

        return false;
    }

    
    public List<String> lsvob(boolean onlyMounted) throws IOException,
                                                          InterruptedException {
        viewListPattern = getListPattern();
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsvob");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            return parseListOutput(new InputStreamReader(
                                                         new ByteArrayInputStream(baos.toByteArray())), onlyMounted);
        }
        return new ArrayList<String>();
    }
    
    public String pwv(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("pwv");
        cmd.add("-root");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String retString = "";

        // changed the path from workspace to getRootViewPath to make Dynamic UCM work
        FilePath viewPath = getRootViewPath(launcher).child(viewName);
        
        if (launcher.run(cmd.toCommandArray(), null, baos, viewPath)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                                                             new ByteArrayInputStream(baos.toByteArray())));
            retString = reader.readLine();
            
            reader.close();
        }
        
        baos.close();

        return retString;
    }

    
    public String catcs(String viewName) throws IOException,
                                                InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("catcs");
        cmd.add("-tag", viewName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String retString = "";
        if (launcher.run(cmd.toCommandArray(), null, baos, null)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                                                             new ByteArrayInputStream(baos.toByteArray())));
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
            retString = builder.toString();
        }
        baos.close();
        return retString;
    }
    
    

    private List<String> parseListOutput(Reader consoleReader,
                                         boolean onlyStarMarked) throws IOException {
        List<String> views = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(consoleReader);
        String line = reader.readLine();
        while (line != null) {
            Matcher matcher = viewListPattern.matcher(line);
            if (matcher.find() && matcher.groupCount() == 3) {
                if ((!onlyStarMarked)
                    || (onlyStarMarked && matcher.group(1).equals("*"))) {
                    String vob = matcher.group(2);
                    int pos = Math.max(vob.lastIndexOf('\\'), vob
                                       .lastIndexOf('/'));
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

    public void mountVobs()  throws IOException, InterruptedException {
    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
    	ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mount");
        cmd.add("-all");
        
        try {
        	launcher.run(cmd.toCommandArray(), null, baos, null);	
        }
        catch (IOException ex) {		
        	logRedundantCleartoolError(cmd.toCommandArray(), ex);
        }
        finally {
        	baos.close();
        }
    }

    public String getViewUuid(String viewName) throws IOException,
                                                InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsview");
        cmd.add("-l", viewName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String retString = "";

        Pattern uuidPattern = Pattern.compile("View uuid: (.*)");
        boolean res = true;
        IOException exception = null;
        
        try {        	
        	res = launcher.run(cmd.toCommandArray(), null, baos, null);
        }
        catch (IOException ex) {
        	exception = ex;
        }
        
        // handle the use case in which view doesn't exist and therefore error is thrown
        String output = getOutputString(baos);
        baos.close();
        if (exception != null && ! output.contains("No matching entries found for view"))
        	throw exception;        
        
        if (res && exception == null) {
        	String [] lines = output.split("\n");
        	for (String line :lines) {
                Matcher matcher = uuidPattern.matcher(line);
                if (matcher.find() && matcher.groupCount() == 1) {
                    retString = matcher.group(1);
                    break;
                }        		
        	}
        }

        return retString;
    }

    public void rmviewtag(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmtag");
        cmd.add("-view");
        cmd.add(viewName);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        launcher.run(cmd.toCommandArray(), null, baos, null);
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
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

    public void unregisterView(String uuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("unregister");
        cmd.add("-view");
        cmd.add("-uuid");
        cmd.add(uuid);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        launcher.run(cmd.toCommandArray(), null, baos, null);
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
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
            throw new IOException("Failed to unregister view: " + builder.toString());
        }
        
    }

    public void rmviewUuid(String viewUuid) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
        cmd.add("-avobs");
        cmd.add("-uuid");
        cmd.add(viewUuid);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();  
        launcher.run(cmd.toCommandArray(), null, baos, null);
        BufferedReader reader = new BufferedReader( new InputStreamReader(new ByteArrayInputStream(baos.toByteArray())));
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
            throw new IOException("Failed to remove view: " + builder.toString());
        }
        
    }
    
    private String getOutputString(ByteArrayOutputStream baos) throws IOException {       	
    	BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(baos.toByteArray())));
    	StringBuilder builder = new StringBuilder();
    	String line;
    	while ((line = reader.readLine()) != null) { 
    		if (builder.length() > 0)
    			builder.append("\n");
    		
    		builder.append(line);
    	}
		reader.close();
    	
    	return builder.toString();
    }
    
    public void logRedundantCleartoolError(String [] cmd, Exception ex) {
    	getLauncher().getListener().getLogger().println("Redundant Cleartool Error ");
    	
    	if (cmd != null) 
    		getLauncher().getListener().getLogger().println("command: " + getLauncher().getCmdString(cmd));
       	
    	getLauncher().getListener().getLogger().println(ex.getMessage());
    }
}
