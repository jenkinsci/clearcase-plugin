/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe
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
package hudson.plugins.clearcase.ucm.service;

import hudson.plugins.clearcase.ClearTool;
import hudson.plugins.clearcase.ClearTool.DiffBlOptions;
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.ucm.model.ActivitiesDelta;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.ucm.model.UcmSelector;
import hudson.plugins.clearcase.util.ClearCaseUtils;
import hudson.plugins.clearcase.util.PathUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class BaselineService extends ClearcaseService {

    @SuppressWarnings("unused")
    private static final Logger   LOG          = Logger.getLogger(BaselineService.class.getName());

    private Map<String, Baseline> baselinePool = new HashMap<String, Baseline>();

    BaselineService(ClearTool clearTool) {
        super(clearTool);
    }

    public ActivitiesDelta compare(Baseline from, Baseline to) throws IOException {
        return compare(from, to, EnumSet.of(DiffBlOptions.ACTIVITIES), null);
    }

    public ActivitiesDelta compareWithVersions(Baseline from, Baseline to, String viewPath) throws IOException {
        return compare(from, to, EnumSet.of(DiffBlOptions.VERSIONS), viewPath);
    }

    public ConfigSpec generateConfigSpec(Baseline[] baselines) throws IOException, InterruptedException {
        StringBuilder sb = new StringBuilder();
        Set<Baseline> baselineSet = new HashSet<Baseline>();
        for (Baseline baseline : baselines) {
            baselineSet.addAll(Arrays.asList(getDependentBaselines(baseline)));
        }
        boolean unix = clearTool.getLauncher().isUnix();
        String fileSep = PathUtil.fileSepForOS(unix);
        String newLine = PathUtil.newLineForOS(unix);
        sb.append("element * CHECKEDOUT").append(newLine);
        ComponentService componentService = new ComponentService(clearTool);
        for (Baseline baseline : baselineSet) {
            String rootDir = componentService.getRootDir(getComponent(baseline));
            if (StringUtils.isNotBlank(rootDir)) {
                sb.append("element \"").append(rootDir).append(fileSep).append("...\" ").append(baseline.getName()).append(" -nocheckout").append(newLine);
            }
        }
        sb.append("element * /main/0 -ucm -nocheckout").append(newLine);
        return new ConfigSpec(sb.toString(), unix);
    }

    public Component getComponent(Baseline baseline) throws IOException, InterruptedException {
        if (baseline.getComponent() == null) {
            String output = IOUtils.toString(clearTool.describe("%[component]Xp", null, baseline.getSelector()));
            Component component = null;
            if (ClearCaseUtils.isCleartoolOutputValid(output)) {
                component = UcmSelector.parse(output, Component.class);
            }
            baseline.setComponent(component);
        }
        return baseline.getComponent();
    }

    public Component[] getComponent(Baseline... baselines) throws IOException, InterruptedException {
        Component[] result = new Component[baselines.length];
        for (int i = 0; i < baselines.length; i++) {
            result[i] = getComponent(baselines[i]);
        }
        return result;
    }

    /**
     * For the given baseline, returns the list of baselines it depends on.
     * 
     * @param baseline
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public Baseline[] getDependentBaselines(Baseline baseline) throws IOException, InterruptedException {
        if (baseline.getDependentBaselines() == null) {
            String output = clearTool.lsbl(baseline.getSelector(), "%[depends_on_closure]Xp");
            Baseline[] result = null;
            if (ClearCaseUtils.isCleartoolOutputValid(output)) {
                String[] splitOutput = StringUtils.split(output);
                result = new Baseline[splitOutput.length];
                int i = 0;
                for (String s : splitOutput) {
                    result[i++] = UcmSelector.parse("baseline:" + s, Baseline.class);
                }
                baseline.setDependentBaselines(result);
            }
        }
        return baseline.getDependentBaselines();
    }

    public Baseline[] getDependentBaselines(Baseline... baselines) throws IOException, InterruptedException {
        Collection<Baseline> result = new ArrayList<Baseline>();
        for (Baseline baseline : baselines) {
            Baseline[] dependentBaselines = getDependentBaselines(baseline);
            result.addAll(Arrays.asList(dependentBaselines));
        }
        return result.toArray(new Baseline[result.size()]);
    }

    public Baseline parse(String selector) {
        String baselineSelector = stripPrefix(selector);
        if (!baselinePool.containsKey(baselineSelector)) {
            baselinePool.put(selector, UcmSelector.parse(baselineSelector, Baseline.class));
        }
        return baselinePool.get(selector);
    }

    private ActivitiesDelta compare(Baseline from, Baseline to, EnumSet<DiffBlOptions> diffBlOptions, String viewPath) throws IOException {
        String fromSelector = from.getSelector();
        String toSelector = to.getSelector();
        if (StringUtils.equals(fromSelector, toSelector)) {
            return ActivitiesDelta.EMPTY;
        }
        Reader reader = clearTool.diffbl(diffBlOptions, fromSelector, toSelector, viewPath);
        return ActivitiesDelta.parse(reader);
    }

    private String stripPrefix(String selector) {
        return StringUtils.removeStart(selector, Baseline.PREFIX);
    }
}
