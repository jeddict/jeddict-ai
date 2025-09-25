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
package io.github.jeddict.ai.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.text.Document;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.windows.TopComponent;
import java.net.FileNameMap;
import java.net.URLConnection;

/**
 *
 * @author Shiwani Gupta
 */
public class FileUtil {

    public static void saveOpenEditor() throws Exception {
        // Get the active TopComponent (the active editor window)
        TopComponent activatedComponent = TopComponent.getRegistry().getActivated();

        // Check if a document is open
        if (activatedComponent != null) {
            // Lookup the DataObject of the active editor
            DataObject dataObject = activatedComponent.getLookup().lookup(DataObject.class);

            if (dataObject != null) {
                // Get the SaveCookie from the DataObject
                SaveCookie saveCookie = dataObject.getLookup().lookup(SaveCookie.class);

                // If there are unsaved changes, save the file
                if (saveCookie != null) {
                    saveCookie.save();
                }
            }
        }
    }

    public static FileObject createTempFileObject(String name, String content) throws IOException {
        File tempFile = File.createTempFile("GenAI-" + name, ".java");
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }
        tempFile = org.openide.filesystems.FileUtil.normalizeFile(tempFile);
        FileObject fileObject = org.openide.filesystems.FileUtil.toFileObject(tempFile);
        return fileObject;
    }

    public static String getLatestContent(FileObject fileObject) {
        try {
            DataObject dataObject = DataObject.find(fileObject);
            EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
            if (editorCookie != null) {
                Document doc = editorCookie.getDocument(); // Do not load if not already open
                if (doc != null) {
                    return doc.getText(0, doc.getLength());
                }
            }
            return fileObject.asText();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Determines the MIME type of a file based on its filename.
     * This method handles both existing and non-existing files.
     * <p>
     * For existing files, it attempts to use NetBeans' {@code FileObject.getMIMEType()}
     * for accurate MIME type detection.
     * </p>
     * <p>
     * For non-existing files, or if {@code FileObject.getMIMEType()} cannot determine
     * the MIME type (e.g., for files not yet created or not recognized by NetBeans'
     * internal resolvers), it falls back to a custom logic:
     * <ul>
     *     <li>Specific programming language extensions (e.g., .java, .c, .cpp, .ruby, .php, .php5)
     *         are mapped to their respective {@code text/x-<extension>} MIME types.</li>
     *     <li>For other extensions, it uses {@code URLConnection.getFileNameMap()} as a general fallback.</li>
     *     <li>If no specific MIME type can be determined, it defaults to {@code application/octet-stream}.</li>
     * </ul>
     * This approach is necessary because NetBeans' primary MIME type resolution
     * relies on {@code FileObject}s (which require existing files), and its
     * {@code FileUtil.getMIMEType(String ext)} method is deprecated and can be problematic.
     * </p>
     *
     * @param filename The name of the file, including its extension.
     * @return The determined MIME type as a String.
     */
    public static String mimeType(final String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename can not be null or empty");
        }

        final FileObject fo = org.openide.filesystems.FileUtil.toFileObject(
            org.openide.filesystems.FileUtil.normalizeFile(new File(filename))
        );
        if (fo != null) {
            return fo.getMIMEType();
        }

        final String extension = org.openide.filesystems.FileUtil.getExtension(filename);

        switch (extension) {
            case "java", "c", "cpp", "ruby", "php", "php5" -> {
                return "text/x-" + extension;
            }
            default -> {
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                String mimeType = fileNameMap.getContentTypeFor(filename);
                if (mimeType == null) {
                    return "application/octet-stream";
                }
                return mimeType;
            }
        }
    }
}
