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
package io.github.jeddict.ai.classpath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

public class JeddictQueryCompletionQuery extends AsyncCompletionQuery {

    private static List<String> cachedClassNames = null;
    private static long lastScanTimestamp = 0;
    private static final long CACHE_EXPIRY_MS = 60 * 1000;
    public static final String JEDDICT_EDITOR_CALLBACK = "jeddict-editor-callback";

    @Override
    protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
        Consumer<FileObject> callback = (Consumer<FileObject>) doc.getProperty(JEDDICT_EDITOR_CALLBACK);
        if (callback == null) {
            resultSet.finish();
            return;
        }
        String prefix = extractPrefix(doc, caretOffset);
        if (prefix == null) {
            resultSet.finish();
            return;
        }
        if (System.currentTimeMillis() - lastScanTimestamp > CACHE_EXPIRY_MS) {
            cachedClassNames = null;
        }
        if (cachedClassNames == null) {
            cachedClassNames = new ArrayList<>();
            scanAllProjects(cachedClassNames);
            lastScanTimestamp = System.currentTimeMillis();
        }

        for (String fqcn : cachedClassNames) {
            if (prefix.isEmpty() || fqcn.toLowerCase().contains(prefix.toLowerCase())) {
                resultSet.addItem(new JeddictQueryCompletionItem(fqcn, caretOffset, prefix, callback));
            }
        }

        resultSet.finish();
    }

    private void collectClassNames(FileObject file, String pkg, List<String> classNames) {
        if (file.isFolder()) {
            for (FileObject child : file.getChildren()) {
                collectClassNames(child, pkg + file.getName() + ".", classNames);
            }
        } else if ("java".equals(file.getExt())) {
            String className = file.getName();
            String fqcn = pkg + className;
            classNames.add(fqcn);
        }
    }

    private boolean scanClassPathEntries(ClassPath classPath, List<String> classNames) {
        boolean found = false;
        for (ClassPath.Entry entry : classPath.entries()) {
            FileObject root = entry.getRoot();
            if (root != null) {
                for (FileObject child : root.getChildren()) {
                    collectClassNames(child, "", classNames);
                }
                found = true;
            }
        }
        return found;
    }

    private void scanProject(Project project, List<String> classNames) {
        for (var sourceGroup : ProjectUtils.getSources(project).getSourceGroups("java")) {
            ClassPath classPath = ClassPath.getClassPath(sourceGroup.getRootFolder(), ClassPath.SOURCE);
            if (classPath != null) {
                scanClassPathEntries(classPath, classNames);
            }
        }
    }

    private void scanAllProjects(List<String> classNames) {
        for (Project project : OpenProjects.getDefault().getOpenProjects()) {
            scanProject(project, classNames);
        }
    }

    private String extractPrefix(Document doc, int caretOffset) {
        try {
            int start = caretOffset - 1;
            boolean hasAt = false;
            while (start >= 0) {
                char ch = doc.getText(start, 1).charAt(0);
                if (ch == '@') {
                    hasAt = true;
                    break;
                }
                if (!Character.isJavaIdentifierPart(ch)) {
                    break;
                }
                start--;
            }
            return hasAt ? doc.getText(start + 1, caretOffset - (start + 1)) : null;
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }
}
