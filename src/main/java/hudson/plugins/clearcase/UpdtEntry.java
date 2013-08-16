/**
 * The MIT License
 *
 * Copyright (c) 2011, Andre Bossert
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

/**
 * Entry from ClearCase updt file
 */
public class UpdtEntry {

    public enum State {
        CHECKEDOUT("CheckedOut"), NEW("New"), UNKNOWN("Unknown"), UNLOAD("UnloadDeleted"), UPDATED("Updated");

        private String name;

        private State(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static State defaultLevel() {
            return State.UNKNOWN;
        }

        public static State fromString(String str) {
            for (State csl : values()) {
                if (csl.name.equals(str)) {
                    return csl;
                }
            }
            return State.defaultLevel();
        }
    }

    private String fileName   = null;
    private String newVersion = null;
    private String oldVersion = null;
    private State  state      = State.defaultLevel();

    public UpdtEntry() {
    }

    public UpdtEntry(State state, String fileName, String oldVersion, String newVersion) {
        this.state = state;
        this.fileName = fileName;
        this.oldVersion = oldVersion;
        this.newVersion = newVersion;
    }

    public UpdtEntry(String stateStr, String fileName, String oldVersion, String newVersion) {
        this(State.fromString(stateStr), fileName, oldVersion, newVersion);
    }

    public String getFileName() {
        return fileName;
    }

    public String getNewVersion() {
        return newVersion;
    }

    public String getObjectSelectorNewVersion() {
        return getObjectSelector(fileName, newVersion);
    }

    public String getObjectSelectorOldVersion() {
        return getObjectSelector(fileName, oldVersion);
    }

    public String getOldVersion() {
        return oldVersion;
    }

    public State getState() {
        return state;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setNewVersion(String newVersion) {
        this.newVersion = newVersion;
    }

    public void setOldVersion(String oldVersion) {
        this.oldVersion = oldVersion;
    }

    public void setState(State state) {
        this.state = state;
    }

    public static UpdtEntry getEntryFromLine(String line) {
        // get index for ":"
        int idx = line.indexOf(":");
        if (idx != -1) {
            String stStr = line.substring(0, idx);
            State st = State.fromString(stStr);
            if (st != State.UNKNOWN) {
                String[] splitted = line.substring(idx + 1).trim().split("\\s+");
                switch (st) {
                case UNLOAD:
                case CHECKEDOUT:
                    return new UpdtEntry(st, splitted[0], null, null);
                case NEW:
                    return new UpdtEntry(st, splitted[0], null, splitted[1]);
                case UPDATED:
                    return new UpdtEntry(st, splitted[0], splitted[1], splitted[2]);
                }
            }
        }
        return new UpdtEntry();
    }

    public static String getObjectSelector(String fileName, String version) {
        return fileName + "@@" + version;
    }
}
