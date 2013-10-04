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
package hudson.plugins.clearcase.ucm.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Represents a delta between two UCM entities (Stream or Baseline). Some activities are only present on the left side, while some other are only on the right
 * side.
 * 
 */
public class ActivitiesDelta {
    public final static ActivitiesDelta EMPTY            = new ActivitiesDelta();

    private final static Pattern        ACTIVITY_PATTERN = Pattern.compile("^([^ ]*) \"(.*)\"$");
    private static final String         LEFT_PREFIX      = "<< ";
    private final static Logger         LOG              = Logger.getLogger(ActivitiesDelta.class.getName());
    private static final String         RIGHT_PREFIX     = ">> ";
    private Collection<Activity>        left             = new ArrayList<Activity>();
    private Collection<Activity>        right            = new ArrayList<Activity>();

    private ActivitiesDelta() {
    }

    private ActivitiesDelta(Collection<Activity> left, Collection<Activity> right) {
        this.left.addAll(left);
        this.right.addAll(right);
    }

    /**
     * @return an unmodifiable collection of activities present on left side.
     */
    public Collection<Activity> getLeft() {
        return Collections.unmodifiableCollection(left);
    }

    /**
     * @return an unmodifiable collection of activities present on right side.
     */
    public Collection<Activity> getRight() {
        return Collections.unmodifiableCollection(right);
    }

    /**
     * Builds a delta from the given reader, which is expected to be the output of a cleartool diffbl command
     */
    public static ActivitiesDelta parse(Reader reader) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(reader);
            Collection<Activity> left = new ArrayList<Activity>();
            Collection<Activity> right = new ArrayList<Activity>();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    left = parseLeft(line, left);
                    right = parseRight(line, right);
                } catch (ActivityParsingException e) {
                    LOG.warning(e.getMessage());
                }
            }
            return new ActivitiesDelta(left, right);
        } finally {
            IOUtils.closeQuietly(br);
            IOUtils.closeQuietly(reader);
        }
    }

    private static Activity buildActivity(String activityLine) {
        Matcher matcher = ACTIVITY_PATTERN.matcher(activityLine);
        if (!matcher.matches()) {
            throw new ActivityParsingException(activityLine);
        }
        String selector = matcher.group(1);
        String headline = matcher.group(2);
        Activity activity = new Activity(selector);
        activity.setHeadline(headline);
        return activity;
    }

    private static boolean isLeftLine(String line) {
        return line.startsWith(LEFT_PREFIX);
    }

    private static boolean isRightLine(String line) {
        return line.startsWith(RIGHT_PREFIX);
    }

    private static Collection<Activity> parseLeft(String line, Collection<Activity> left) {
        if (isLeftLine(line)) {
            left.add(parseLeftLine(line));
        }
        return left;
    }

    private static Activity parseLeftLine(String line) {
        String activityLine = StringUtils.removeStart(line, LEFT_PREFIX);
        return buildActivity(activityLine);
    }

    private static Collection<Activity> parseRight(String line, Collection<Activity> right) {
        if (isRightLine(line)) {
            right.add(parseRightLine(line));
        }
        return right;
    }

    private static Activity parseRightLine(String line) {
        String activityLine = StringUtils.removeStart(line, RIGHT_PREFIX);
        return buildActivity(activityLine);
    }
}
