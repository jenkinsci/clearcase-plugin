/**
 * The MIT License
 *
 * Copyright (c) 2007-2010, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
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
package hudson.plugins.clearcase.ucm;

import hudson.FilePath;
import hudson.plugins.clearcase.Baseline;
import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.DiffBlOptions;
import hudson.plugins.clearcase.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * @author kyosi
 */
public class UcmCommon {
    
    /**
     * Takes a list of baselines as argument, and return the load rules for all components matching these baselines
     * @param clearTool
     * @param stream
     * @param baselines
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static String[] generateLoadRulesFromBaselines(ClearTool clearTool, String stream, List<Baseline> baselines) throws IOException, InterruptedException {
        if (baselines == null) {
            return null;
        }
        List<String> loadRules = new ArrayList<String>();
        int i = 0;
        for(Baseline bl : baselines) {
            Reader reader = clearTool.describe("%[root_dir]p\\n", "component:" + bl.getComponentName());
            BufferedReader br = new BufferedReader(reader);
            StringBuilder sb = new StringBuilder();
            for(String line = br.readLine(); line != null; line = br.readLine()){
                if (StringUtils.isNotBlank(line)) {
                    sb.append(line.substring(1)); // Remove leading separator
                }
            }
            String loadRule = sb.toString();
            if (StringUtils.isNotBlank(loadRule)) {
                loadRules.add(loadRule);
            }
        }
        return loadRules.toArray(new String[loadRules.size()]);
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
    public static List<String> getLatestBaselineNames(ClearTool clearTool, boolean isUseDynamicView, String viewName, FilePath filePath,
            List<String> readWriteComponents) throws IOException, InterruptedException {
        String output = clearTool.lsstream(null, viewName, "%[latest_bls]Xp");
        String prefix = "baseline:";
        List<String> baselineNames = new ArrayList<String>();
        if (StringUtils.startsWith(output, prefix)) {
            String[] baselineNamesSplit = output.split("baseline:");
            for (String baselineName : baselineNamesSplit) {
                if (StringUtils.isNotBlank(baselineName)) {
                    String baselineNameTrimmed = StringUtils.trim(baselineName);
                    // Retrict to baseline bind to read/write component
                    String blComp = getDataforBaseline(clearTool, filePath, baselineNameTrimmed).getBaselineName();
                    if (readWriteComponents == null || readWriteComponents.contains(blComp)) {
                        baselineNames.add(baselineNameTrimmed);
                    }
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
    public static List<Baseline> getComponentsForBaselines(ClearTool clearTool, List<Component> componentsList, boolean isUseDynamicView, String viewName,
            FilePath filePath, List<String> baselinesNames) throws InterruptedException, IOException {
        List<Baseline> baselinesList = new ArrayList<Baseline>();

        // loop through baselines
        for (String blName : baselinesNames) {
            Baseline baseline = getDataforBaseline(clearTool, filePath, blName);
            Component matchComponentDesc = null;

            // find the equivalent componentDesc element
            for (Component componentDesc : componentsList) {
                if (getNoVob(componentDesc.getName()).equals(getNoVob(baseline.getComponentName()))) {
                    matchComponentDesc = componentDesc;
                    baselinesList.add(new Baseline(blName, matchComponentDesc, baseline.isNotLabeled()));
                    break;
                }
            }
            if (matchComponentDesc == null) {
                clearTool.getLauncher().getListener().error("Could not find a component matching baseline " + blName);
            }
        }

        return baselinesList;
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
    public static Baseline getDataforBaseline(ClearTool clearTool, FilePath filePath, String blName) throws InterruptedException, IOException {
        String cleartoolResult = clearTool.lsbl(blName, "%[label_status]p|%[component]Xp");
        String[] arr = cleartoolResult.split("\\|");
        boolean isNotLabeled = arr[0].contains("Not Labeled");

        String prefix = "component:";
        String componentName = arr[1].substring(cleartoolResult.indexOf(cleartoolResult) + prefix.length());

        return new Baseline(componentName, isNotLabeled);
    }
    
    public static List<Baseline> getLatestBaselines(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        return getBaselinesDesc(clearTool, stream, "%[latest_bls]p\\n");
    }
    
    public static List<Baseline> getFoundationBaselines(ClearTool clearTool, String stream) throws IOException, InterruptedException {
        return getBaselinesDesc(clearTool, stream, "%[found_bls]p\\n");
    }
    
    private static List<Baseline> getBaselinesDesc(ClearTool clearTool, String stream, String format) throws IOException,
            InterruptedException {
        BufferedReader rd = new BufferedReader(clearTool.describe(format, "stream:" + stream));
        List<String> baselines = new ArrayList<String>();
        try {
            for (String line = rd.readLine(); line != null; line = rd.readLine()) {
                String[] bl = line.split(" ");
                for (String b : bl) {
                    if (StringUtils.isNotBlank(b)) {
                        baselines.add(b);
                    }
                }
            }
        } finally {
            rd.close();
        }
        List<Baseline> foundationBaselines = new ArrayList<Baseline>();
        String pvob = UcmCommon.getVob(stream);
        for (String baseline : baselines) {
            String qualifiedBaseline = baseline + "@" + pvob;
            BufferedReader br = new BufferedReader(clearTool.describe("%[component]p\\n", "baseline:" + qualifiedBaseline));
            try {
                foundationBaselines.add(new Baseline(qualifiedBaseline, br.readLine() + "@" + pvob));
            } finally {
                br.close();
            }
        }
        return foundationBaselines;
    }
    
    /**
     * @param clearToolLauncher
     * @param streamName
     * @return list of components - name and isModifiable
     * @throws IOException
     * @throws InterruptedException
     */
    public static List<Component> getStreamComponentsDesc(ClearTool clearTool, String streamName) throws IOException, InterruptedException {
        List<Component> componentsDescList = new ArrayList<Component>();
        Reader reader = clearTool.describe(null, "stream:" + streamName);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        while (bufferedReader.ready()) {
            sb.append(bufferedReader.readLine());
        }
        String output = sb.toString();

        // searching in the result for the pattern (<component-name> (modifiable | non-modifiable)
        int idx = 0;
        int idx1;
        int idx2;
        String searchFor = "modifiable)";
        while (idx >= 0) {
            idx = output.indexOf(searchFor, idx + 1);

            if (idx > 0) {
                // get the component state part: modifiable or non-modifiable
                idx1 = output.lastIndexOf("(", idx - 1);
                idx2 = output.indexOf(")", idx1);
                String componentState = output.substring(idx1 + 1, idx2);

                // get the component name
                idx1 = output.lastIndexOf("(", idx1 - 1);
                idx2 = output.indexOf(")", idx1);

                // add to the result
                Component componentDesc = new Component(output.substring(idx1 + 1, idx2), componentState.equals("modifiable"));
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
    public static List<Baseline> getLatestBlsWithCompOnStream(ClearTool clearTool, String stream, String view) throws IOException, InterruptedException {
        // get the components on the build stream
        List<Component> componentsList = getStreamComponentsDesc(clearTool, stream);

        // get latest baselines on the stream (name only)
        List<String> latestBlsOnBuildStream = getLatestBaselineNames(clearTool, true, view, null, null);

        // add component information to baselines
        List<Baseline> latestBlsWithComp = getComponentsForBaselines(clearTool, componentsList, true, view, null, latestBlsOnBuildStream);

        return latestBlsWithComp;
    }

    /**
     * @param componentsDesc
     * @return list of read-write components out of components list (removing read-only components)
     */
    public static List<String> getReadWriteComponents(List<Component> components) {
        List<String> res = new ArrayList<String>();

        for (Component comp : components) {
            if (comp.isModifiable()) {
                res.add(comp.getName());
            }
        }

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
    public static List<String> getDiffBlVersions(ClearTool clearTool, String viewRootDirectory, String bl1, String bl2) throws IOException,
            InterruptedException {
        Reader rd = clearTool.diffbl(EnumSet.of(DiffBlOptions.VERSIONS), bl1, bl2, viewRootDirectory);

        BufferedReader br = new BufferedReader(rd);

        List<String> versionList = new ArrayList<String>();
        // remove ">>" from result
        for (String line = br.readLine(); br.ready(); line = br.readLine()) {
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
    public static String getVersionDescription(ClearTool clearTool, String version, String format) throws IOException, InterruptedException {
        Reader rd = clearTool.describe(format, version);
        BufferedReader bufferedReader = new BufferedReader(rd);
        StringBuilder sb = new StringBuilder();
        while (bufferedReader.ready()) {
            sb.append(bufferedReader.readLine());
        }
        return sb.toString();
    }

    /**
     * @param clearToolLauncher
     * @param stream
     * @param baselines
     * @throws IOException
     * @throws InterruptedException
     */
    public static void rebase(ClearTool clearTool, String viewName, List<Baseline> baselines) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        for (Baseline bl : baselines) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(bl.getBaselineName());
        }
        clearTool.rebaseDynamic(viewName, sb.toString());
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

}
