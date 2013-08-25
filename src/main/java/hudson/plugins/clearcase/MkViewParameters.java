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
package hudson.plugins.clearcase;

import hudson.plugins.clearcase.viewstorage.ViewStorage;

public class MkViewParameters {

    private String      additionalParameters;
    private String      streamSelector;
    private ViewType    type = ViewType.Snapshot;
    private String      viewPath;
    private ViewStorage viewStorage;
    private String      viewTag;

    public String getAdditionalParameters() {
        return additionalParameters;
    }

    public String getStreamSelector() {
        return streamSelector;
    }

    public ViewType getType() {
        return type;
    }

    public String getViewPath() {
        return viewPath;
    }

    public ViewStorage getViewStorage() {
        if (viewStorage == null) {
            viewStorage = ViewStorage.createDefault();
        }
        return viewStorage;
    }

    public String getViewTag() {
        return viewTag;
    }

    public void setAdditionalParameters(String additionalParameters) {
        this.additionalParameters = additionalParameters;
    }

    public void setStreamSelector(String streamSelector) {
        this.streamSelector = streamSelector;
    }

    public void setType(ViewType type) {
        this.type = type;
    }

    public void setViewPath(String viewPath) {
        this.viewPath = viewPath;
    }

    public void setViewStorage(ViewStorage viewStorage) {
        this.viewStorage = viewStorage;
    }

    public void setViewTag(String viewTag) {
        this.viewTag = viewTag;
    }

}
