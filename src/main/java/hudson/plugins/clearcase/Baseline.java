/**
 * The MIT License
 *
 * Copyright (c) 2013 Vincent Latombe, Yosi Kalmanson
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

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

/**
 * @author kyosi
 */
@ExportedBean
public class Baseline {

    @Exported(visibility = 3)
    public String    baselineName;

    @Exported(visibility = 3)
    public Component componentDesc;

    @Exported(visibility = 3)
    public String    componentName;

    boolean          isNotLabeled;

    public Baseline(String componentName, boolean isNotLabeled) {
        super();
        this.componentName = componentName;
        this.isNotLabeled = isNotLabeled;
    }

    public Baseline(String baselineName, Component componentDesc) {
        super();
        this.baselineName = baselineName;
        this.componentDesc = componentDesc;
        this.componentName = componentDesc.getName();
    }

    public Baseline(String baselineName, Component componentDesc, boolean isNotLabeled) {
        super();
        this.baselineName = baselineName;
        this.componentDesc = componentDesc;
        this.componentName = componentDesc.getName();
        this.isNotLabeled = isNotLabeled;
    }

    public Baseline(String baselineName, String componentName) {
        super();
        this.baselineName = baselineName;
        this.componentName = componentName;
        this.componentDesc = null;
    }

    public String getBaselineName() {
        return baselineName;
    }

    public Component getComponentDesc() {
        return componentDesc;
    }

    public String getComponentName() {
        return (componentDesc != null ? componentDesc.getName() : componentName);
    }

    public boolean isNotLabeled() {
        return isNotLabeled;
    }

    public void setBaselineName(String baselineName) {
        this.baselineName = baselineName;
    }

    public void setComponentDesc(Component componentDesc) {
        this.componentDesc = componentDesc;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setNotLabeled(boolean isNotLabeled) {
        this.isNotLabeled = isNotLabeled;
    }
}