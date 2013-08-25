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
import hudson.plugins.clearcase.ConfigSpec;
import hudson.plugins.clearcase.model.Versions;
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Stream;
import hudson.plugins.clearcase.ucm.model.UcmSelector;
import hudson.plugins.clearcase.util.ClearCaseUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class StreamService extends ClearcaseService {
    private Cache<String, Stream> streamPool = CacheBuilder.newBuilder().maximumSize(10).build();

    StreamService(ClearTool clearTool) {
        super(clearTool);
    }

    public boolean exists(Stream stream) throws IOException, InterruptedException {
        return clearTool.doesStreamExist(stream.getSelector());
    }

    /**
     * @return the current config spec for the given stream.
     * @throws IOException
     *             in case the config spec cannot be determined.
     */
    public ConfigSpec getConfigSpec(Stream stream) throws IOException, InterruptedException {
        Reader reader = clearTool.describe("%[config_spec]Xp", null, stream.getSelector());
        String output = IOUtils.toString(reader);
        if (ClearCaseUtils.isCleartoolOutputValid(output)) {
            return new ConfigSpec(output, clearTool.getLauncher().isUnix());
        }
        throw new IOException("Invalid output, cannot determine the config spec for " + stream + ". Output : " + output);
    }

    /**
     * @returns the current foundation baselines for the given stream.
     */
    public Baseline[] getFoundationBaselines(Stream stream) throws IOException, InterruptedException {
        Stream streamFromPool = getFromPool(stream);
        if (streamFromPool.getFoundationBaselines() == null) {
            streamFromPool.setFoundationBaselines(describeToBaselines(streamFromPool, "%[found_bls]Xp"));
        }
        return streamFromPool.getFoundationBaselines();
    }

    /**
     * @return the latest baselines available on the given stream
     */
    public Baseline[] getLatestBaselines(Stream stream) throws IOException, InterruptedException {
        Stream streamFromPool = getFromPool(stream);
        if (streamFromPool.getLatestBaselines() == null) {
            streamFromPool.setLatestBaselines(describeToBaselines(streamFromPool, "%[latest_bls]Xp"));
        }
        return streamFromPool.getLatestBaselines();
    }

    /**
     * Returns the view tags attached to the given stream
     */
    public String[] getViews(Stream stream) throws IOException, InterruptedException {
        Collection<String> viewTags = new ArrayList<String>();
        Reader reader = clearTool.describe("%[views]p", null, stream.getSelector());
        BufferedReader br = new BufferedReader(reader);
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (ClearCaseUtils.isCleartoolOutputValid(line)) {
                    String[] entry = line.split(" ");
                    for (String s : entry) {
                        viewTags.add(s);
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(reader);
        }
        return viewTags.toArray(new String[viewTags.size()]);
    }

    public boolean isViewAttachedTo(String viewTag, Stream stream) throws IOException, InterruptedException {
        return ArrayUtils.contains(getViews(stream), viewTag);
    }

    /**
     * Parse a stream selector into a valid Stream object
     * 
     * @param selector
     * @return
     */
    public Stream parse(String selector) {
        String formattedSelector = selector.startsWith(Stream.PREFIX) ? selector : Stream.PREFIX + selector;
        Stream candidate = streamPool.getIfPresent(formattedSelector);
        if (candidate == null) {
            Stream stream = UcmSelector.parse(selector, Stream.class);
            streamPool.put(stream.getSelector(), stream);
            return stream;
        }
        return candidate;
    }
    
    public Versions getVersions(Stream stream, String viewPath) throws IOException, InterruptedException{
        Reader reader = clearTool.lsactivityIn(stream.getSelector(), "%[versions]p\\n", viewPath);
        return Versions.parse(reader, viewPath, null);
    }

    private Baseline[] describeToBaselines(Stream stream, String format) throws IOException, InterruptedException {
        Reader reader = clearTool.describe(format, null, stream.getSelector());
        BufferedReader br = new BufferedReader(reader);
        List<Baseline> baselines = new ArrayList<Baseline>();
        try {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if (ClearCaseUtils.isCleartoolOutputValid(line)) {
                    String[] bl = line.split(" ");
                    for (String b : bl) {
                        if (StringUtils.isNotBlank(b)) {
                            baselines.add(UcmSelector.parse(b, Baseline.class));
                        }
                    }
                }
            }
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(reader);
        }
        if (baselines.isEmpty()) {
            throw new IOException("Unexpected output for command \"cleartool describe -fmt " + format + " " + stream.getSelector()
                    + "\" or no available baseline found");
        }
        return baselines.toArray(new Baseline[baselines.size()]);
    }

    private Stream getFromPool(Stream stream) {
        Stream streamFromPool = streamPool.getIfPresent(stream.getSelector());
        if (streamFromPool == null) {
            streamPool.put(stream.getSelector(), stream);
            streamFromPool = stream;
        }
        return streamFromPool;
    }

}
