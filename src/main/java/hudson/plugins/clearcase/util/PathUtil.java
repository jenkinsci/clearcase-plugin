/**
 * The MIT License
 *
 * Copyright (c) 2007-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Erik Ramfelt,
 *                          Henrik Lynggaard, Peter Liljenberg, Andrew Bayer
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
package hudson.plugins.clearcase.util;

import hudson.Launcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public abstract class PathUtil {

    public static String convertPathForOS(String path, boolean isUnix) {
        if (path == null) {
            return null;
        }
        String tempPath = path;
        if (isUnix) {
            tempPath = tempPath.replaceAll("\r\n", "\n");
        } else {
            tempPath = tempPath.replaceAll("\n", "\r\n");
            tempPath = tempPath.replaceAll("\r\r\n", "\r\n");
        }
        if (isUnix) {
            tempPath = tempPath.replaceAll("\\\\", "/");
        } else {
            tempPath = tempPath.replaceAll("/", "\\\\");
        }
        return tempPath;
    }

    public static String convertPathForOS(String path, Launcher launcher) {
        return convertPathForOS(path, launcher.isUnix());
    }

    public static String fileSepForOS(boolean isUnix) {
        if (isUnix) {
            return "/";
        }
        return "\\";
    }

    public static String newLineForOS(boolean isUnix) {
        if (isUnix) {
            return "\n";
        }
        return "\r\n";
    }

    public static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (IOException ignored) {
                    // no op
                }
            }
        }
        return new String(buffer);
    }
}
