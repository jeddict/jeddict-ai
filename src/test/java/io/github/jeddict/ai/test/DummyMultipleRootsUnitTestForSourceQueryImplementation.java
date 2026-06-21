/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeddict.ai.test;

import java.net.MalformedURLException;
import java.net.URL;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.queries.MultipleRootsUnitTestForSourceQueryImplementation;
import org.openide.filesystems.FileObject;

public class DummyMultipleRootsUnitTestForSourceQueryImplementation
    implements MultipleRootsUnitTestForSourceQueryImplementation {

    private static final String MAIN_JAVA = "src/main/java/";
    private static final String TEST_JAVA = "src/test/java/";

    final Project project;

    public DummyMultipleRootsUnitTestForSourceQueryImplementation(final Project project) {
        this.project = project;
    }

    @Override
    public URL[] findUnitTests(FileObject fo) {
        return map(fo, MAIN_JAVA, TEST_JAVA, true);
    }

    @Override
    public URL[] findSources(FileObject fo) {
        return map(fo, TEST_JAVA, MAIN_JAVA, false);
    }

    private URL[] map(FileObject fo, String fromRoot, String toRoot, boolean toTest) {
        if (fo == null || project == null || project.getProjectDirectory() == null) {
            return new URL[0];
        }

        final String projectDir = project.getProjectDirectory().getPath().replace('\\', '/');
        final String filePath = normalizePath(fo);
        final int rootIndex = filePath.indexOf(fromRoot);
        if (rootIndex < 0) {
            return new URL[0];
        }

        final String relativePath = filePath.substring(rootIndex + fromRoot.length());
        final String mappedRelativePath = mapRelativePath(relativePath, toTest);
        final String mappedPath = projectDir + "/" + toRoot + mappedRelativePath;

        try {
            return new URL[]{new URL("file", "", mappedPath)};
        } catch (MalformedURLException ex) {
            return new URL[0];
        }
    }

    private String normalizePath(FileObject fo) {
        String path = fo.getPath().replace('\\', '/');
        if (fo.isFolder() && !path.endsWith("/")) {
            path += "/";
        }
        return path;
    }

    private String mapRelativePath(String relativePath, boolean toTest) {
        if (relativePath.isEmpty() || relativePath.endsWith("/")) {
            return relativePath;
        }
        if (toTest) {
            if (relativePath.endsWith(".java")) {
                final String baseName = relativePath.substring(0, relativePath.length() - ".java".length());
                return baseName + "Test.java";
            }
            return relativePath;
        }
        if (relativePath.endsWith("Test.java")) {
            return relativePath.substring(0, relativePath.length() - "Test.java".length()) + ".java";
        }
        return relativePath;
    }

}
