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
package io.github.jeddict.ai.agent;

import io.github.jeddict.ai.util.DocAction;
import io.github.jeddict.ai.util.ThrowingFunction;
import java.io.IOException;
import java.nio.file.Path;
import javax.swing.text.Document;
import org.netbeans.api.java.source.JavaSource;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;

public abstract class AbstractCodeTool extends AbstractTool {

    public AbstractCodeTool(final String basedir) throws IOException {
        super(basedir);
    }

    /**
     * Executes an action on a JavaSource obtained from a file in the project.
     *
     * @param project  the NetBeans project
     * @param fullpath     relative path of the Java file
     * @param action   function to run with JavaSource, returns a result
     * @param modify   if true, will open the document in modification mode
     * @param <T>      result type
     * @return the result of the action, or error message if failed
     */
    protected <T> T withJavaSource(
        String path,
        ThrowingFunction<JavaSource, T> action,
        boolean modify
    ) throws Exception {
        Path fullPath = fullPath(path);
        FileObject fo = org.openide.filesystems.FileUtil.toFileObject(fullPath);
        if (fo == null) {
            return (T) ("File not found: " + path);
        }

        JavaSource javaSource = JavaSource.forFileObject(fo);
        if (javaSource == null) {
            return (T) ("Not a Java source file: " + path);
        }

        return action.apply(javaSource);
    }

    /**
     * Utility method that opens a NetBeans document for a given path and
     * applies the specified action.
     * <p>
     * This centralizes error handling and ensures that the document is saved
     * after the action is applied.
     *
     * @param path the relative or absolute path to the file
     * @param action the action to perform on the opened {@link Document}
     * @param save save the doc after post action
     * @return the result of the action, or an error message
     */
    protected String withDocument(String path, DocAction action, boolean save)
    throws Exception {
        FileObject fo = org.openide.filesystems.FileUtil.toFileObject(fullPath(path));
        if (fo == null) {
            return "File not found: " + path;
        }

        DataObject dobj = DataObject.find(fo);
        EditorCookie cookie = dobj.getLookup().lookup(EditorCookie.class);
        if (cookie == null) {
            return "No editor available for: " + path;
        }

        Document doc = cookie.openDocument();
        String result = action.apply(doc);
        if (save) {
            cookie.saveDocument();
        }
        return result;
    }
}