package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.plugins.clearcase.ClearToolLauncher;
import hudson.util.ArgumentListBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author kyosi
 */
public class UcmCommon {

    /**
     * @param clearToolLauncher
     * @param isUseDynamicView
     * @param viewName
     * @param filePath
     * @param baselineName
     * @param baselineComment
     * @param identical
     * @param fullBaseline
     * @param readWriteComponents
     * @return list of created baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<BaselineDesc> makeBaseline(ClearToolLauncher clearToolLauncher, 
                                      boolean isUseDynamicView,
                                      String viewName,
                                      FilePath filePath, 
                                      String baselineName, 
                                      String baselineComment,                                      
                                      boolean identical, 
                                      boolean fullBaseline,
                                      List<String> readWriteComponents) throws IOException, InterruptedException  {

        List<BaselineDesc> createdBaselinesList = new ArrayList<BaselineDesc>();

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkbl");
        if (identical) {
            cmd.add("-identical");
        }
        cmd.add("-comment");
        cmd.add(baselineComment);
        if (fullBaseline) {
            cmd.add("-full");
        } else {
            cmd.add("-incremental");
        }

        FilePath clearToolLauncherPath = filePath;
        if (isUseDynamicView) {
            cmd.add("-view");
            cmd.add(viewName);
            clearToolLauncherPath = clearToolLauncher.getWorkspace();
        }

        // Make baseline only for read/write components (identical or not)
        if (readWriteComponents != null) {
            cmd.add("-comp");
            StringBuffer lstComp = new StringBuffer();
            for (String comp : readWriteComponents) {
                lstComp.append(",");
                lstComp.append(comp);
            }
            lstComp.delete(0, 1);
            cmd.add(lstComp.toString());
        }

        cmd.add(baselineName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();
        if (cleartoolResult.contains("cleartool: Error")) {
            throw new IOException("Failed to make baseline, reason: " + cleartoolResult);
        }

        Pattern pattern = Pattern.compile("Created baseline \".+?\" .+? \".+?\"");
        Matcher matcher = pattern.matcher(cleartoolResult);
        while (matcher.find()) {
            String match = matcher.group();
            String[] parts = match.split("\"");
            String newBaseline = parts[1];
            String componentName = parts[3];

            createdBaselinesList.add(new BaselineDesc(newBaseline, componentName));
        }

        return createdBaselinesList;
    }

    /**
     * @param clearToolLauncher
     * @param isUseDynamicView
     * @param viewName
     * @param filePath
     * @param readWriteComponents . if null both baselines on read and read-write components will be returned
     * @return List of latest baselines on read write components (only)
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    public static List<String> getLatestBaselineNames(ClearToolLauncher clearToolLauncher, boolean isUseDynamicView, String viewName, FilePath filePath,
            List<String> readWriteComponents) throws IOException, InterruptedException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        FilePath clearToolLauncherPath = filePath;
        List<String> baselineNames = new ArrayList<String>();

        cmd.add("lsstream");
        if (isUseDynamicView) {
            cmd.add("-view");
            cmd.add(viewName);
            clearToolLauncherPath = clearToolLauncher.getWorkspace();
        }
        cmd.add("-fmt");
        cmd.add("%[latest_bls]Xp");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();
        String prefix = "baseline:";
        if (cleartoolResult != null && cleartoolResult.startsWith(prefix)) {
            String[] baselineNamesSplit = cleartoolResult.split("baseline:");
            for (String baselineName : baselineNamesSplit) {
                String baselineNameTrimmed = baselineName.trim();
                if (!baselineNameTrimmed.equals("")) {
                    // Retrict to baseline bind to read/write component
                    String blComp = getDataforBaseline(clearToolLauncher, filePath, baselineNameTrimmed).getBaselineName();
                    if (readWriteComponents == null || readWriteComponents.contains(blComp))
                        baselineNames.add(baselineNameTrimmed);
                }
            }

        }

        return baselineNames;
    }

    /**
     * @param clearToolLauncher
     * @param isUseDynamicView
     * @param viewName
     * @param filePath
     * @param baselinesNames
     * @return list of BaselineDesc (baseline name and component name)
     * @throws InterruptedException
     * @throws IOException
     */
    public static List<BaselineDesc> getComponentsForBaselines(ClearToolLauncher clearToolLauncher, List<ComponentDesc> componentsList,
            boolean isUseDynamicView, String viewName, FilePath filePath, List<String> baselinesNames) throws InterruptedException, IOException {
        List<BaselineDesc> baselinesDescList = new ArrayList<BaselineDesc>();

        // loop through baselines
        for (String blName : baselinesNames) {
            BaselineDesc baseLineDesc = getDataforBaseline(clearToolLauncher, filePath, blName);
            ComponentDesc matchComponentDesc = null;

            // find the equivalent componentDesc element
            for (ComponentDesc componentDesc : componentsList) {
                if (getNoVob(componentDesc.getName()).equals(getNoVob(baseLineDesc.getComponentName()))) {
                    matchComponentDesc = componentDesc;
                    break;
                }

            }

            baselinesDescList.add(new BaselineDesc(blName, matchComponentDesc, baseLineDesc.isNotLabeled));
        }

        return baselinesDescList;
    }

    /**
     * Get the component binding to the baseline
     * 
     * @param clearToolLauncher
     * @param filePath
     * @param blName the baseline name like 'deskCore_3.2-146_2008-11-14_18-07-22.3543@\P_ORC'
     * @return the component name like 'Desk_Core@\P_ORC'
     * @throws InterruptedException
     * @throws IOException
     */
    public static BaselineDesc getDataforBaseline(ClearToolLauncher clearToolLauncher, FilePath filePath, String blName) throws InterruptedException,
            IOException {

        ArgumentListBuilder cmd = new ArgumentListBuilder();

        FilePath clearToolLauncherPath = filePath;

        cmd.add("lsbl");
        cmd.add("-fmt");
        cmd.add("%[label_status]p|%[component]Xp");
        cmd.add(blName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();
        String[] arr = cleartoolResult.split("\\|");
        boolean isNotLabeled = arr[0].contains("Not Labeled");

        String prefix = "component:";
        String componentName = arr[1].substring(cleartoolResult.indexOf(cleartoolResult) + prefix.length());

        return new BaselineDesc(componentName, isNotLabeled);
    }

    /**
     * @param clearToolLauncher
     * @param parentStream
     * @param childStream
     * @throws IOException
     * @throws InterruptedException
     */
    public static void mkstream(ClearToolLauncher clearToolLauncher, String parentStream, String childStream) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("mkstream");
        cmd.add("-in");
        cmd.add(parentStream);
        cmd.add(childStream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
    }

    /**
     * @param clearToolLauncher
     * @param stream
     * @return boolean indicating if the given stream exists
     * @throws IOException
     * @throws InterruptedException
     */
    public static boolean isStreamExists(ClearToolLauncher clearToolLauncher, String stream) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("lsstream");
        cmd.add("-short");
        cmd.add(stream);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        } catch (Exception e) {
            // empty by design
        }
        baos.close();
        String cleartoolResult = baos.toString();
        return !(cleartoolResult.contains("stream not found"));
    }

    /**
     * @param clearToolLauncher
     * @param streamName
     * @return list of components - name and isModifiable
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<ComponentDesc> getStreamComponentsDesc(ClearToolLauncher clearToolLauncher, String streamName) throws IOException, InterruptedException {
        List<ComponentDesc> componentsDescList = new ArrayList<ComponentDesc>();
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("desc");
        cmd.add("stream:" + streamName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        String cleartoolResult = baos.toString();

        // searching in the result for the pattern (<component-name> (modifiable | non-modifiable)
        int idx = 0;
        int idx1;
        int idx2;
        String searchFor = "modifiable)";
        while (idx >= 0) {
            idx = cleartoolResult.indexOf(searchFor, idx + 1);

            if (idx > 0) {
                // get the component state part: modifiable or non-modifiable
                idx1 = cleartoolResult.lastIndexOf("(", idx - 1);
                idx2 = cleartoolResult.indexOf(")", idx1);
                String componentState = cleartoolResult.substring(idx1 + 1, idx2);

                // get the component name
                idx1 = cleartoolResult.lastIndexOf("(", idx1 - 1);
                idx2 = cleartoolResult.indexOf(")", idx1);

                // add to the result
                ComponentDesc componentDesc = new ComponentDesc(cleartoolResult.substring(idx1 + 1, idx2), componentState.equals("modifiable"));
                componentsDescList.add(componentDesc);
            }
        }

        return componentsDescList;
    }

    /**
     * @return List of latest BaseLineDesc (baseline + component) for stream. Only baselines on read-write components
     *         are returned
     * @throws InterruptedException
     * @throws IOException
     * @throws Exception
     */
    public static List<BaselineDesc> getLatestBlsWithCompOnStream(ClearToolLauncher clearToolLauncher, String stream, String view) throws IOException,
            InterruptedException {
        // get the components on the build stream
        List<ComponentDesc> componentsList = getStreamComponentsDesc(clearToolLauncher, stream);

        // get latest baselines on the stream (name only)
        List<String> latestBlsOnBuildStream = getLatestBaselineNames(clearToolLauncher, true, view, null, null);

        // add component information to baselines
        List<BaselineDesc> latestBlsWithComp = getComponentsForBaselines(clearToolLauncher, componentsList, true, view, null, latestBlsOnBuildStream);

        return latestBlsWithComp;
    }

    /**
     * @param componentsDesc
     * @return list of read-write components out of components list (removing read-only components)
     */
    public static List<String> getReadWriteComponents(List<ComponentDesc> componentsDesc) {
        List<String> res = new ArrayList<String>();

        for (ComponentDesc comp : componentsDesc)
            res.add(comp.getName());

        return res;
    }

    /**
     * @param clearToolLauncher
     * @param viewRootDirectory
     * @param bl1
     * @param bl2
     * @return list of versions that were changed between two baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<String> getDiffBlVersions(ClearToolLauncher clearToolLauncher, String viewRootDirectory, String bl1, String bl2) throws IOException,
            InterruptedException {
        List<String> versionList = new ArrayList<String>();

        ArgumentListBuilder cmd = new ArgumentListBuilder();
        FilePath clearToolLauncherPath = new FilePath(clearToolLauncher.getLauncher().getChannel(), viewRootDirectory);

        cmd.add("diffbl");
        cmd.add("-versions");
        cmd.add(bl1);
        cmd.add(bl2);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, clearToolLauncherPath);
        baos.close();
        String cleartoolResult = baos.toString();

        // remove ">>" from result
        String[] arr = cleartoolResult.split("\n");
        for (String line : arr) {
            if (line.startsWith(">>")) {
                line = line.replaceAll(">>", "");
                versionList.add(line.trim());
            }
        }

        return versionList;
    }

    /**
     * @param clearToolLauncher
     * @param version
     * @return version description
     * @throws IOException
     * @throws InterruptedException
     */
    public static String getVersionDescription(ClearToolLauncher clearToolLauncher, String version, String format) throws IOException, InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("desc");
        cmd.add("-fmt");
        cmd.add(format);
        cmd.add(version);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
        String cleartoolResult = baos.toString();

        return cleartoolResult;
    }

    /**
     * @param clearToolLauncher
     * @param stream
     * @param baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static void rebase(ClearToolLauncher clearToolLauncher, String viewName, List<UcmCommon.BaselineDesc> baselines) throws IOException,
            InterruptedException {
        ArgumentListBuilder cmd = new ArgumentListBuilder();

        cmd.add("rebase");

        cmd.add("-base");
        String baselineStr = "";
        for (UcmCommon.BaselineDesc bl : baselines) {
            if (baselineStr.length() > 0)
                baselineStr += ",";
            baselineStr += bl.getBaselineName();
        }
        cmd.add(baselineStr);

        cmd.add("-view");
        cmd.add(viewName);

        cmd.add("-complete");
        cmd.add("-force");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        clearToolLauncher.run(cmd.toCommandArray(), null, baos, null);
        baos.close();
    }

    /**
     * @param clearToolLauncher
     * @param element
     * @return
     */
    public static String getNoVob(String element) {
        return element.split("@")[0];
    }

    public static String getVob(String element) {
        return element.split("@")[1];
    }

    /**
     * @author kyosi
     */
    @ExportedBean
    public static class BaselineDesc {

        @Exported(visibility = 3)
        public String baselineName;

        @Exported(visibility = 3)
        public String componentName;

        @Exported(visibility = 3)
        public ComponentDesc componentDesc;

        private boolean isNotLabeled;

        public BaselineDesc(String componentName, boolean isNotLabeled) {
            super();
            this.componentName = componentName;
            this.isNotLabeled = isNotLabeled;
        }

        public BaselineDesc(String baselineName, String componentName) {
            super();
            this.baselineName = baselineName;
            this.componentName = componentName;
            this.componentDesc = null;
        }

        public BaselineDesc(String baselineName, ComponentDesc componentDesc) {
            super();
            this.baselineName = baselineName;
            this.componentDesc = componentDesc;
            this.componentName = componentDesc.getName();
        }

        public BaselineDesc(String baselineName, ComponentDesc componentDesc, boolean isNotLabeled) {
            super();
            this.baselineName = baselineName;
            this.componentDesc = componentDesc;
            this.componentName = componentDesc.getName();
            this.isNotLabeled = isNotLabeled;
        }

        public String getBaselineName() {
            return baselineName;
        }

        public void setBaselineName(String baselineName) {
            this.baselineName = baselineName;
        }

        public String getComponentName() {
            return (componentDesc != null ? componentDesc.getName() : componentName);
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public ComponentDesc getComponentDesc() {
            return componentDesc;
        }

        public void setComponentDesc(ComponentDesc componentDesc) {
            this.componentDesc = componentDesc;
        }

        public boolean isNotLabeled() {
            return isNotLabeled;
        }

        public void setNotLabeled(boolean isNotLabeled) {
            this.isNotLabeled = isNotLabeled;
        }
    }

    /**
     * @author kyosi
     */
    @ExportedBean
    public static class ComponentDesc {

        @Exported(visibility = 3)
        public String name;

        @Exported(visibility = 3)
        public boolean isModifiable;

        public ComponentDesc(String name, boolean isModifiable) {
            this.name = name;
            this.isModifiable = isModifiable;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isModifiable() {
            return isModifiable;
        }

        public void setModifiable(boolean isModifiable) {
            this.isModifiable = isModifiable;
        }
    }

}
