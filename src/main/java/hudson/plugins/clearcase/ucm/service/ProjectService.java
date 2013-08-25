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
import hudson.plugins.clearcase.ucm.model.Project;
import hudson.plugins.clearcase.ucm.model.UcmSelector;
import hudson.plugins.clearcase.util.ClearCaseUtils;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class ProjectService extends ClearcaseService {
    ProjectService(ClearTool clearTool) {
        super(clearTool);
    }

    public Component[] getModifiableComponents(Project project) throws IOException, InterruptedException {
        Reader reader = clearTool.describe("%[mod_comps]Xp", null, project.getSelector());
        String output = IOUtils.toString(reader);
        if (ClearCaseUtils.isCleartoolOutputValid(output)) {
            String[] split = StringUtils.split(output, ' ');
            Component[] components = new Component[split.length];
            for (int i = 0; i < split.length; i++) {
                components[i] = UcmSelector.parse(split[i], Component.class);
            }
            return components;
        }
        throw new IOException(output);
    }
}
