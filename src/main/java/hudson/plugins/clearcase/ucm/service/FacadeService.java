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
import hudson.plugins.clearcase.ucm.model.Baseline;
import hudson.plugins.clearcase.ucm.model.Component;
import hudson.plugins.clearcase.ucm.model.Stream;

import java.io.IOException;

/**
 * Facade service to UCM ClearCase services. This also contains reusable methods involving a combination of services.
 */
public class FacadeService extends ClearcaseService {

    private final ActivityService  activityService;
    private final BaselineService  baselineService;
    private final ComponentService componentService;
    private final ProjectService   projectService;
    private final StreamService    streamService;

    public FacadeService(ClearTool clearTool) {
        super(clearTool);
        this.activityService = new ActivityService(clearTool);
        this.baselineService = new BaselineService(clearTool);
        this.componentService = new ComponentService(clearTool);
        this.projectService = new ProjectService(clearTool);
        this.streamService = new StreamService(clearTool);
    }

    public ActivityService getActivityService() {
        return activityService;
    }

    public String[] getAllRootDirsFor(String streamSelector) throws IOException, InterruptedException {
        Stream stream = streamService.parse(streamSelector);
        Baseline[] foundationBaselines = streamService.getFoundationBaselines(stream);
        Baseline[] baselinesClosure = baselineService.getDependentBaselines(foundationBaselines);
        Component[] components = baselineService.getComponent(baselinesClosure);
        return componentService.getRootDir(components);
    }

    public BaselineService getBaselineService() {
        return baselineService;
    }

    public ComponentService getComponentService() {
        return componentService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public StreamService getStreamService() {
        return streamService;
    }
}
