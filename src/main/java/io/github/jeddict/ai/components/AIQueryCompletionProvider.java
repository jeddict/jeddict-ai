/**
 * Copyright 2025 the original author or authors from the Jeddict project (https://jeddict.github.io/).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jeddict.ai.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.editor.mimelookup.MimeRegistration;

import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.java.source.ClassIndex;
import org.netbeans.api.java.source.ClasspathInfo;
import org.netbeans.api.java.source.ElementHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ui.OpenProjects;

import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 * AI Query editor completion (packages + classes)
 * 
 * @author Gaurav Gupta
 */
@MimeRegistration(
        mimeType = "text/x-java",
        service = CompletionProvider.class
)
public final class AIQueryCompletionProvider implements CompletionProvider {

    private static boolean isAIEditor(JTextComponent c) {
        return Boolean.TRUE.equals(
                c.getClientProperty("AI_QUERY_EDITOR"));
    }

    @Override
    public CompletionTask createTask(int queryType, JTextComponent component) {
        if (!isAIEditor(component)) {
            return null;
        }
        return new AsyncCompletionTask(new Query(), component);
    }

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        if (!isAIEditor(component)) {
            return 0;
        }
        if (typedText == null || typedText.isEmpty()) {
            return 0;
        }
        char c = typedText.charAt(typedText.length() - 1);
        return (Character.isJavaIdentifierPart(c) || c == '.')
                ? COMPLETION_QUERY_TYPE
                : 0;
    }

    // ----------------------------------------------------------------------
    private static final class Query extends AsyncCompletionQuery {

        private static final Set<ClassIndex.SearchScope> SCOPES
                = EnumSet.of(
                        ClassIndex.SearchScope.SOURCE,
                        ClassIndex.SearchScope.DEPENDENCIES
                );

        @Override
        protected void query(CompletionResultSet rs,
                Document doc,
                int caretOffset) {

            try {
                String prefix = getPrefix(doc, caretOffset);
                if (prefix.isEmpty()) {
                    return;
                }

                Project project = getOpenProject();
                if (project == null) {
                    return;
                }

                ClassIndex index = getClassIndex(project);
                if (index == null) {
                    return;
                }

                Set<String> results = new LinkedHashSet<>();

                // Packages
                results.addAll(
                        index.getPackageNames(prefix, true, SCOPES)
                );

                // Classes
                for (ElementHandle<TypeElement> h
                        : index.getDeclaredTypes(
                                prefix,
                                ClassIndex.NameKind.PREFIX,
                                SCOPES)) {

                    results.add(h.getQualifiedName());
                }

                for (String s : results) {
                    rs.addItem(new Item(
                            s,
                            caretOffset - prefix.length(),
                            prefix.length()));
                }

            } finally {
                rs.finish();
            }
        }

        private static String getPrefix(Document doc, int caretOffset) {
            try {
                int start = caretOffset;
                String text = doc.getText(0, caretOffset);
                while (start > 0) {
                    char c = text.charAt(start - 1);
                    if (Character.isJavaIdentifierPart(c) || c == '.') {
                        start--;
                    } else {
                        break;
                    }
                }
                return text.substring(start, caretOffset);
            } catch (BadLocationException ex) {
                return "";
            }
        }

        private static Project getOpenProject() {
            Project[] open = OpenProjects.getDefault().getOpenProjects();
            return open.length > 0 ? open[0] : null;
        }

        private static ClassIndex getClassIndex(Project project) {
            SourceGroup[] groups = ProjectUtils.getSources(project)
                    .getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);

            if (groups.length == 0) {
                return null;
            }

            ClassPath sourceCp = ClassPath.getClassPath(
                    groups[0].getRootFolder(), ClassPath.SOURCE);
            ClassPath compileCp = ClassPath.getClassPath(
                    groups[0].getRootFolder(), ClassPath.COMPILE);
            ClassPath bootCp = ClassPath.getClassPath(
                    groups[0].getRootFolder(), ClassPath.BOOT);

            if (sourceCp == null) {
                return null;
            }

            return ClasspathInfo
                    .create(bootCp, compileCp, sourceCp)
                    .getClassIndex();
        }
    }

    // ----------------------------------------------------------------------
    private static final class Item implements CompletionItem {

        private final String text;
        private final int offset;
        private final int length;

        Item(String text, int offset, int length) {
            this.text = text;
            this.offset = offset;
            this.length = length;
        }

        @Override
        public void defaultAction(JTextComponent component) {
            try {
                Document doc = component.getDocument();
                doc.remove(offset, length);
                doc.insertString(offset, text, null);
            } catch (BadLocationException ignored) {
            }
            Completion.get().hideAll();
        }

        @Override
        public void processKeyEvent(KeyEvent evt) {
            // required by interface, not used
        }

        @Override
        public void render(Graphics g, Font f, Color fg,
                Color bg, int width, int height,
                boolean selected) {

            CompletionUtilities.renderHtml(
                    null,
                    text,
                    null,
                    g,
                    f,
                    selected ? Color.white : fg,
                    width,
                    height,
                    selected
            );
        }

        @Override
        public int getPreferredWidth(Graphics g, Font f) {
            return CompletionUtilities.getPreferredWidth(text, null, g, f);
        }

        @Override
        public CompletionTask createDocumentationTask() {
            return null;
        }

        @Override
        public CompletionTask createToolTipTask() {
            return null;
        }

        @Override
        public boolean instantSubstitution(JTextComponent c) {
            return false;
        }

        @Override
        public int getSortPriority() {
            return 100;
        }

        @Override
        public CharSequence getSortText() {
            return text;
        }

        @Override
        public CharSequence getInsertPrefix() {
            return text;
        }
    }
}
