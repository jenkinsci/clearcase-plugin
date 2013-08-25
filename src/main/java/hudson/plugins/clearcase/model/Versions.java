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
package hudson.plugins.clearcase.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;

public class Versions implements Iterable<Version> {

    private Collection<Version> versions = new ArrayList<Version>();

    private Versions() {
    }

    public Collection<Version> getVersions() {
        return Collections.unmodifiableCollection(versions);
    }

    @Override
    public Iterator<Version> iterator() {
        return versions.iterator();
    }

    public static Versions parse(Reader reader, String viewPath) throws IOException {
        Versions instance = new Versions();
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            instance.versions.add(Version.parse(line, viewPath));
        }
        return instance;
    }

    public static Versions parse(Reader reader, String viewPath, String separator) throws IOException {
        Versions instance = new Versions();
        BufferedReader br = new BufferedReader(reader);
        String line;
        while ((line = br.readLine()) != null) {
            for (String extendedViewPath : StringUtils.split(line, separator)) {
                instance.versions.add(Version.parse(extendedViewPath, viewPath));
            }
        }
        return instance;
    }

    public static Versions parse(String output, String viewPath, String separator) {
        Versions instance = new Versions();
        for (String extendedViewPath : StringUtils.split(output, separator)) {
            instance.versions.add(Version.parse(extendedViewPath, viewPath));
        }
        return instance;
    }

}
