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

import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.spi.project.support.GenericSources;
import org.openide.filesystems.FileObject;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.project.JavaProjectConstants;

public class DummyProjectSources implements Sources {

    public static final String SOURCES_TYPE_JAVA = "java";
    public static final String SOURCES_TYPE_TEST = "test";

    private final Project project;

    public DummyProjectSources(Project project) {
        this.project = project;
    }

    @Override
    public SourceGroup[] getSourceGroups(String type) {
        final List<SourceGroup> groups = new ArrayList();
        // Intercept requests for Java sources
        FileObject projectDir = project.getProjectDirectory();

        // 1. Handle Main Java Files (e.g., src/main/java or src)
        if (JavaProjectConstants.SOURCES_TYPE_JAVA.equals(type)) {
            FileObject srcDir = projectDir.getFileObject("src/main/java");
            if (srcDir == null) {
                srcDir = projectDir.getFileObject("src"); // Fallback check
            }
            groups.addAll(List.of(createGroupArray(srcDir, "src", "Java Sources")));

            FileObject testDir = projectDir.getFileObject("src/test/java");
            if (testDir == null) {
                testDir = projectDir.getFileObject("src/test"); // Fallback check
            }
            groups.addAll(List.of(createGroupArray(testDir, "test", "Test Sources")));
        }

        return groups.toArray(SourceGroup[]::new);
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
    }

    // --------------------------------------------------------- private methods


    private SourceGroup[] createGroupArray(FileObject dir, String internalName, String displayName) {
        if (dir != null) {
            return new SourceGroup[] {
                GenericSources.group(project, dir, internalName, displayName, null, null)
            };
        }
        return new SourceGroup[0];
    }
}
