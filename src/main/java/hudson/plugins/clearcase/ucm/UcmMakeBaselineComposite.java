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
package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.plugins.clearcase.ClearCaseUcmSCM;
import hudson.plugins.clearcase.HudsonClearToolLauncher;
import hudson.plugins.clearcase.PluginImpl;
import hudson.tasks.Publisher;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kohsuke.stapler.StaplerRequest;

/**
 * UcmMakeBaselineComposite creates a composite baseline and
 * extracting composite baseline information is a file.
 * 
 * @author Gregory BOISSINOT - Zenika
 */
public class UcmMakeBaselineComposite extends Publisher {


    public final static Descriptor<Publisher> DESCRIPTOR = new UcmMakeBaselineDescriptor();

    private final String compositeNamePattern;    
    private final String compositeStreamSelector;
    private final String compositeComponentName;
    private final boolean extractInfoFile;
    private final String fileName;
        

	public String getCompositeNamePattern(){
		return compositeNamePattern;
	}	
	public String getCompositeStreamSelector(){
		return compositeStreamSelector;
	}	
    public String getCompositeComponentName() {
        return this.compositeComponentName;
    }        
    public boolean isExtractInfoFile() {
        return this.extractInfoFile;
    }    
    public String getFileName() {
        return this.fileName;
    }	
	
    
    public static final class UcmMakeBaselineDescriptor extends
            Descriptor<Publisher> {

        public UcmMakeBaselineDescriptor() {
            super(UcmMakeBaselineComposite.class);
        }

        @Override
        public String getDisplayName() {
            return "ClearCase UCM Makebaseline Composite";
        }

        @Override
        public Publisher newInstance(StaplerRequest req) throws FormException {
            Publisher p = new UcmMakeBaselineComposite(
                    req.getParameter("mkbl.compositenamepattern"),
                    req.getParameter("mkbl.compositestreamselector"),
                    req.getParameter("mkbl.compositecomponentname"),
                    req.getParameter("mkbl.extractinfofile")!=null,
                    req.getParameter("mkbl.filename")
            );
            return p;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/clearcase/ucm/mkbl/composite/help.html";
        }
    }

    private UcmMakeBaselineComposite(
            final String compositeNamePattern,
            final String compositeStreamSelector,
            final String compositeComponentName,
            final boolean extractInfoFile,
            final String fileName) {

    	this.compositeNamePattern = compositeNamePattern.trim();
        this.compositeStreamSelector = compositeStreamSelector.trim();               
        this.compositeComponentName=compositeComponentName.trim();
        this.extractInfoFile=extractInfoFile;
        this.fileName=fileName.trim();
        
    }

    @Override
    public boolean needsToRunAfterFinalized() {
        return true;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {

        return true;
    }
        

    @SuppressWarnings("unchecked")
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
			BuildListener listener) throws InterruptedException, IOException {

		if (build.getProject().getScm() instanceof ClearCaseUcmSCM) {
			ClearCaseUcmSCM scm = (ClearCaseUcmSCM) build.getProject().getScm();
			FilePath filePath = build.getProject().getWorkspace().child(
					scm.getViewName());

			HudsonClearToolLauncher clearToolLauncher = new HudsonClearToolLauncher(
					PluginImpl.BASE_DESCRIPTOR.getCleartoolExe(),
					getDescriptor().getDisplayName(), listener, filePath,
					launcher);

			if (build.getResult().equals(Result.SUCCESS)) {

				try{
					
			        String compositeBaselineName = Util.replaceMacro(this.compositeNamePattern, build.getEnvVars());

			        String pvob = compositeStreamSelector;
			        if (compositeStreamSelector.contains("@"+ File.separator)) {
			        	pvob = compositeStreamSelector.substring(compositeStreamSelector.indexOf("@" + File.separator)+2, compositeStreamSelector.length());
			        }				        
			        
					this.makeCompositeBaseline(compositeBaselineName, this.compositeStreamSelector, this.compositeComponentName, pvob, clearToolLauncher, filePath);

				
					this.promoteCompositeBaselineToBuiltLevel(compositeBaselineName, pvob, clearToolLauncher, filePath);
				
					if (extractInfoFile){
						processExtractInfoFile(this.compositeComponentName,pvob,compositeBaselineName, this.fileName, clearToolLauncher, filePath);
					}
					
				}
				catch (Exception ex){
		    		listener.getLogger().println("Failed to create baseline: " + ex);
		    		return false;
				}
			} else {
			listener.getLogger().println(
					"Not a UCM clearcase SCM, cannot create baseline composite");
			}
			
		}
		return true;
	}


    public Descriptor<Publisher> getDescriptor() {
        return DESCRIPTOR;
    }



    /**
     * Retrieve all Clearcase UCM component (with pvob suffix) for a stream
     * 
     * @param stream the stream name like 'P_EngDesk_Product_3.2_int@\P_ORC'
     * @param clearToolLauncher the clearcase launcher
     * @param filePath the file path
     * @return component list attached to the stream like ['DocGen_PapeeteDoc@\P_ORC','DocMgt_Modulo@\P_ORC']
     * @throws IOException
     * @throws InterruptedException
     */
    private List<String> getComponentList(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws IOException, InterruptedException {
    	
    	List<String> result =  new ArrayList<String>();
    	
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsstream");       
        cmd.add("-fmt");       
        cmd.add("\"%[components]XCp\""); 
        cmd.add(stream); 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        String cleartoolResult = baos.toString();
        baos.close();
    	
		String comp []= cleartoolResult.split(",\\s");
		for (String c:comp){
			result.add(c.split("component:")[1]);
		}
            	
        return result;
    }
    
    
    /**
	 * Pick up a view from a stream
	 * 
	 * @return a view attached to the stream
	 */
    private String getOneViewFromStream(String stream,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws Exception {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsstream");       
        cmd.add("-fmt");       
        cmd.add("\"%[views]p\""); 
        cmd.add(stream); 

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        String cleartoolResult = baos.toString();
        baos.close();
        
        String resultLines[] = cleartoolResult.split("\n");
        if (resultLines.length == 0){
            throw new Exception("There is no view attached to the stream '" + stream + "'");
        }
        String viewsLines = resultLines[resultLines.length-1];
        return viewsLines.split(" ")[0];
        
        
    }

    
    /**
     * Make a composite baseline 
     * 
     * @param compositeBaselineName the composite baseline name
     * @param compositeStream the composite UCM Clearcase stream with Pvob like : 'P_EngDesk_Product_3.2_int@\P_ORC'
     * @param compositeComponent the composite UCM Clearcase component name like 'C_
Build_EngDesk'
     * @param clearToolLauncher the ClearCase launcher
     * @param filePath the filepath
     * @throws Exception
     */
    private void makeCompositeBaseline(
    		String compositeBaselineName,
    		String compositeStream,
    		String compositeComponent,
    		String pvob,
    		HudsonClearToolLauncher clearToolLauncher, FilePath filePath)
            throws Exception {
        
    	//Get a view containing the composite component
		String compositeView = getOneViewFromStream(this.compositeStreamSelector,clearToolLauncher,filePath);
		
		//Get the component list (with pvob suffix) for the stream
 		List<String> componentList = getComponentList(this.compositeStreamSelector,clearToolLauncher,filePath);
		    	
    	//Make baseline composite
        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("mkbl");       
        cmd.add("-nc");
        cmd.add("-view"); 
        cmd.add(compositeView); 
        cmd.add("-comp");
        cmd.add(compositeComponent + "@" + File.separator + pvob);
        cmd.add("-full");
        cmd.add("-ddepends_on");
        
		StringBuffer sb = new StringBuffer();
		for (String comp:componentList){
			//Exclude the composite component
			if (!comp.contains(compositeComponent))
				sb.append(",").append(comp);
		}
		sb.delete(0, 1);  		
        cmd.add(sb.toString());
        
        cmd.add("-adepends_on");
        cmd.add(sb.toString());
 
        cmd.add(compositeBaselineName);
    	
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            throw new Exception("Failed to make baseline, reason: "
                    + cleartoolResult);
        }    	

    }

    
    
    
    
    /**
     * Promote the composite baseline
     * 
     * @param compositeBaselineName the composite baseline name
     * @param pvob the vob name
     * @param clearToolLauncher the clearcase launcher
     * @param filePath the filepath
     * @throws InterruptedException
     * @throws IOException
     */
    private void promoteCompositeBaselineToBuiltLevel(String compositeBaselineName, String pvob,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath) 
    throws InterruptedException, IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("chbl");
        cmd.add("-level");
        cmd.add("BUILT");
        cmd.add(compositeBaselineName + "@" + File.separator + pvob);

        clearToolLauncher.run(cmd.toCommandArray(), null, null, filePath);
    }   
    
    
    /**
     * Retrieve the binding component for the current baseline
     * @param baseline the current baseline
     * @param clearToolLauncher
     * @param filePath
     * @return
     * @throws Exception
     */
    private String getComponent(String baseline,
    		HudsonClearToolLauncher clearToolLauncher, FilePath filePath) 
    		throws Exception {

    		ArgumentListBuilder cmd = new ArgumentListBuilder();
    		cmd.add("lsbl");
    		cmd.add("-fmt");
    		cmd.add("\"%[component]p\"");
    		cmd.add(baseline);

    		ByteArrayOutputStream baos = new ByteArrayOutputStream();
    		clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
    		baos.close();
    		String cleartoolResult = baos.toString();
    		if (cleartoolResult.contains("cleartool: Error")) 
    			throw new Exception("Failed to make baseline, reason: "+ cleartoolResult);
    			
    		return cleartoolResult;	
   }  


   
    /**
     * Extract Composite baseline information in an external file
     * @param compositeComponnentName
     * @param pvob
     * @param compositeBaselineName
     * @param fileName
     * @param clearToolLauncher
     * @param filePath
     * @throws Exception
     */
    private void processExtractInfoFile(String compositeComponnentName, 
    									String pvob, 
    									String compositeBaselineName,
    									String fileName,
            HudsonClearToolLauncher clearToolLauncher, FilePath filePath) 
    throws Exception {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        cmd.add("lsbl");
        cmd.add("-fmt");
        cmd.add("\"%[depends_on]p\"");
        cmd.add(compositeBaselineName  +"@" +  File.separator + pvob);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, filePath);
        baos.close();
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            throw new Exception("Failed to make baseline, reason: "
                    + cleartoolResult);
        }  
        
        
        String baselinesComp[] = cleartoolResult.split(" ");
        List<String> baselineList = Arrays.asList(baselinesComp);
        Collections.sort(baselineList);

        FileWriter fw = null;
        try{
        	File f = new File(filePath.getRemote() + File.separator + fileName);
        	if (!f.exists()){
        		f.createNewFile();
        	}
        	fw = new FileWriter(f);
        	fw.write("The composite baseline is '" + compositeBaselineName + "'");
        	for (String baseLine:baselineList){
        		fw.write("\nThe  baseline of component '"+ getComponent(baseLine, clearToolLauncher, filePath) + "' is :" + baseLine);
        	}
        }
        finally {
        	if (fw!= null)
        		fw.close();
        }
        
    }   

    
}
