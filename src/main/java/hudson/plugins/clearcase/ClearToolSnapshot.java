package hudson.plugins.clearcase;

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
import java.io.InputStreamReader;

public class ClearToolSnapshot extends ClearToolExec {

    private String optionalMkviewParameters;
    
    public ClearToolSnapshot(VariableResolver variableResolver, ClearToolLauncher launcher) {
        super(variableResolver, launcher);
    }

    public ClearToolSnapshot(VariableResolver variableResolver, ClearToolLauncher launcher, String optionalParameters) {
        this(variableResolver, launcher);
        this.optionalMkviewParameters = optionalParameters;
    }

    /**
     * To set the config spec of a snapshot view, you must be in or under the snapshot view root directory.
     * @see http://www.ipnom.com/ClearCase-Commands/setcs.html
     */
    public void setcs(String viewName, String configSpec) throws IOException,
            InterruptedException {
        FilePath workspace = launcher.getWorkspace();
        FilePath configSpecFile = workspace.createTextTempFile("configspec", ".txt", configSpec);
        String csLocation = ".." + File.separatorChar + configSpecFile.getName();
        csLocation = PathUtil.convertPathsBetweenUnixAndWindows(csLocation, launcher.getLauncher());
	
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("setcs");
     	cmd.add(csLocation);
        launcher.run(cmd.toCommandArray(), null, null, workspace.child(viewName));

        configSpecFile.delete();
    }

    public void mkview(String viewName, String streamSelector) throws IOException, InterruptedException {
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
        }
        cmd.add(viewName);
        launcher.run(cmd.toCommandArray(), null, null, null);
    }

    public void rmview(String viewName) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("rmview");
        cmd.add("-force");
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
        	throw new IOException("Failed to remove view: " + builder.toString());
        }
       
        
        FilePath viewFilePath = launcher.getWorkspace().child(viewName);
        if (viewFilePath.exists()) {
            launcher.getListener().getLogger().println(
                    "Removing view folder as it was not removed when the view was removed.");
            viewFilePath.deleteRecursive();
        }
    }

    public void update(String viewName, String loadRules) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("update");
        cmd.add("-force");
        cmd.add("-log", "NUL");
        if (loadRules == null) {
            cmd.add(viewName);
        } else {
            cmd.add("-add_loadrules");
            String loadRulesLocation = viewName + File.separator + loadRules;
			cmd.add(PathUtil.convertPathsBetweenUnixAndWindows(loadRulesLocation, getLauncher().getLauncher()));
        }
        launcher.run(cmd.toCommandArray(), null, null, null);
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
