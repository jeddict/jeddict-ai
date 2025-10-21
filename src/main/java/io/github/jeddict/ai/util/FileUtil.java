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

import static io.github.jeddict.ai.settings.PreferencesManager.JEDDICT_CONFIG;
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

    public static FileObject getFileObject(Project project, String path) {
        java.io.File file = resolvePath(project, path).toFile();
        return org.openide.filesystems.FileUtil.toFileObject(file);
    }

    public static String readContent(Project project, String path) throws IOException {
        return project.getProjectDirectory().getFileObject(path).asText();
    }

    public static Path resolvePath(Project project, String path) {
        Path p = Paths.get(path);
        if (!p.isAbsolute() && project != null) {
            FileObject projectDir = project.getProjectDirectory();
            if (projectDir != null) {
                return Paths.get(projectDir.getPath()).resolve(path)
                        .toAbsolutePath().normalize();
            }
        }
        return p.toAbsolutePath().normalize();
    }


    public static Path getConfigPath() {
        final String os = System.getProperty("os.name").toLowerCase();
        final Path userHome = Paths.get(System.getProperty("user.home"));

        if (os.contains("win")) {
            final String appData = System.getenv("APPDATA");
            final Path basePath;
            if (appData != null && !appData.isEmpty()) {
                basePath = Paths.get(appData);
            } else {
                basePath = userHome.resolve("AppData").resolve("Roaming");
            }

            return basePath.resolve("jeddict");
        } else if (os.contains("mac")) {
            return userHome.resolve("Library/Application Support").resolve("jeddict");
        } else if (os.contains("linux")) {
            return userHome.resolve(".config").resolve("jeddict");
        } else {
            return userHome.resolve(JEDDICT_CONFIG);
        }
    }

}
