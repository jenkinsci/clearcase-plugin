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
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.util.ClearCaseUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ComponentService extends ClearcaseService {

    ComponentService(ClearTool clearTool) {
        super(clearTool);
    }

    /**
     * Returns the root directory for the given UCM Component. It may be empty in case the component doesn't have any root directory, like for a composite
     * component.
     */
    public String getRootDir(Component component) throws IOException, InterruptedException {
        if (component.getRootDir() == null) {
            String output = IOUtils.toString(clearTool.describe("%[root_dir]Xp", null, component.getSelector()));
            String rootDir = null;
            if (ClearCaseUtils.isCleartoolOutputValid(output)) {
                rootDir = output;
            }
            component.setRootDir(rootDir);
        }
        return component.getRootDir();
    }

    /**
     * <p>
     * Returns an array of root directories for the given array of UCM Components. Components with no root directory (composite) are filtered out, so the
     * resulting array :
     * 
     * <ul>
     * <li>contains at most n elements (where n is the size of the components array)
     * <li>doesn't contain empty elements
     * </p>
     */
    public String[] getRootDir(Component[] components) throws IOException, InterruptedException {
        List<String> rootDirs = new ArrayList<String>();
        for (int i = 0; i < components.length; i++) {
            String rootDir = getRootDir(components[i]);
            if (StringUtils.isNotEmpty(rootDir)) {
                rootDirs.add(rootDir);
            }
        }
        return rootDirs.toArray(new String[rootDirs.size()]);
    }

}
