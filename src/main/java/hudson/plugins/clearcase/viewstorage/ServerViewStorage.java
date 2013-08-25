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
package hudson.plugins.clearcase.viewstorage;

import hudson.Extension;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Messages;
import hudson.model.TaskListener;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.util.FormValidation;
import hudson.util.VariableResolver;
import hudson.util.ListBoxModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.framework.io.ByteBuffer;

import antlr.ANTLRException;

public class ServerViewStorage extends ViewStorage {

    @Extension
    public static class DescriptorImpl extends ViewStorageDescriptor<ServerViewStorage> {

        static class AutoCompleteSeeder {
            private String source;

            AutoCompleteSeeder(String source) {
                this.source = source;
            }

            List<String> getSeeds() {
                ArrayList<String> terms = new ArrayList<String>();
                boolean trailingQuote = source.endsWith("\"");
                boolean leadingQuote = source.startsWith("\"");
                boolean trailingSpace = source.endsWith(" ");

                if (trailingQuote || (trailingSpace && !leadingQuote)) {
                    terms.add("");
                } else {
                    if (leadingQuote) {
                        int quote = source.lastIndexOf('"');
                        if (quote == 0) {
                            terms.add(source.substring(1));
                        } else {
                            terms.add("");
                        }
                    } else {
                        int space = source.lastIndexOf(' ');
                        if (space > -1) {
                            terms.add(source.substring(space + 1));
                        } else {
                            terms.add(source);
                        }
                    }
                }

                return terms;
            }
        }

        public AutoCompletionCandidates doAutoCompleteAssignedLabelString(@QueryParameter String value) {
            AutoCompletionCandidates c = new AutoCompletionCandidates();
            Set<Label> labels = Jenkins.getInstance().getLabels();
            List<String> queries = new AutoCompleteSeeder(value).getSeeds();

            for (String term : queries) {
                for (Label l : labels) {
                    if (l.getName().startsWith(term)) {
                        c.add(l.getName());
                    }
                }
            }
            return c;
        }

        public FormValidation doCheckAssignedLabelString(@QueryParameter String value) {
            if (Util.fixEmpty(value) == null)
                return FormValidation.ok(); // nothing typed yet
            try {
                Label.parseExpression(value);
            } catch (ANTLRException e) {
                return FormValidation.error(e, Messages.AbstractProject_AssignedLabelString_InvalidBooleanExpression(e.getMessage()));
            }
            Label l = Jenkins.getInstance().getLabel(value);
            if (l.isEmpty()) {
                for (LabelAtom a : l.listAtoms()) {
                    if (a.isEmpty()) {
                        LabelAtom nearest = LabelAtom.findNearest(a.getName());
                        return FormValidation.warning(Messages.AbstractProject_AssignedLabelString_NoMatch_DidYouMean(a.getName(), nearest.getDisplayName()));
                    }
                }
                return FormValidation.warning(Messages.AbstractProject_AssignedLabelString_NoMatch());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillServerItems(@QueryParameter("assignedLabelString") String assignedLabelString) {
            ByteBuffer baos = new ByteBuffer();
            ListBoxModel m = new ListBoxModel();
            Node n;
            if (assignedLabelString != null) {
                Label label = Jenkins.getInstance().getLabel(assignedLabelString);
                n = getRandomNode(label);
            } else {
                n = Jenkins.getInstance();
            }
            m.add("auto", "-auto");
            try {
                createLauncher(n).launch().cmds(getClearcaseDescriptor().getCleartoolExe(), "lsstgloc", "-view").stdout(baos).join();
                BufferedReader reader = new BufferedReader(new InputStreamReader(baos.newInputStream()));
                String line;
                Pattern pattern = Pattern.compile("(.*) (.*)");
                while ((line = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.matches()) {
                        String path = matcher.group(2).trim();
                        String id = matcher.group(1).trim();
                        m.add(id + " (" + path + ")", id);
                    }
                }
            } catch (IOException e) {
            } catch (InterruptedException e) {
            }
            return m;
        }

        @Override
        public String getDisplayName() {
            return "Use server storage location";
        }

        public String getServer() {
            ViewStorage defaultViewStorage = getDefaultViewStorage();
            if (defaultViewStorage instanceof ServerViewStorage) {
                return ((ServerViewStorage) defaultViewStorage).server;
            }
            return null;
        }

        private Launcher createLauncher(Node node) {
            return node.createLauncher(TaskListener.NULL);
        }

        private Node getRandomNode(Label label) {
            Set<Node> nodes = label.getNodes();
            int nodeIndex = (int) (Math.random() * (nodes.size() - 1));
            int i = 0;
            for (Node node : nodes) {
                if (i == nodeIndex) {
                    return node;
                }
                i++;
            }
            return Jenkins.getInstance();
        }

    }

    private String assignedLabelString;

    private String server;

    public ServerViewStorage(String server) {
        this(null, server);
    }

    @DataBoundConstructor
    public ServerViewStorage(String assignedLabelString, String server) {
        this.assignedLabelString = assignedLabelString;
        this.server = server;
    }

    @Override
    public ViewStorage decorate(VariableResolver<String> resolver) {
        return new ServerViewStorage(Util.replaceMacro(server, resolver));
    }

    public String getAssignedLabelString() {
        return assignedLabelString;
    }

    @Override
    public String[] getCommandArguments(boolean unix, String viewTag) {
        return new String[] { "-stgloc", server };
    }

    public String getServer() {
        return server;
    }

    public void setAssignedLabelString(String assignedLabelString) {
        this.assignedLabelString = assignedLabelString;
    }

    public void setServer(String server) {
        this.server = server;
    }

}
