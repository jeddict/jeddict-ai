/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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
package io.github.jeddict.ai.scanner;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.agent.MavenProjectTools;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Paths;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;

/**
 *
 */
@CacioTest
public class ProjectMetadataInfoTest extends TestBase {

    @Test
    public void get_returns_basic_info_for_maven_project() throws Exception {
        final Project project = project(projectDir);

        // Generic path: no BuildMetadataResolver → name comes from the project
        // directory; EE/JDK fields are absent (they require MavenProjectTools).
        final String info = ProjectMetadataInfo.get(project);
        then(info)
            .contains("- folder: " + Paths.get(project.getProjectDirectory().getPath()))
            .contains("- type: maven")
            .contains("- Source Directory: src/main/java")
            .contains("- Test Source Directory: src/test/java")
            .doesNotContain("- EE Version:")
            .doesNotContain("- Java Version:");
    }

    @Test
    public void get_with_resolver_returns_maven_metadata() throws Exception {
        final Project project = project(projectDir);

        // MavenProjectTools acts as the BuildMetadataResolver: reads pom.xml.
        final MavenProjectTools resolver = new MavenProjectTools(project);
        final String info = ProjectMetadataInfo.get(project, resolver);
        then(info)
            .contains("- name: name")        // <name> from pom.xml
            .contains("- folder: " + Paths.get(project.getProjectDirectory().getPath()))
            .contains("- type: maven")
            .contains("- Source Directory: src/main/java")
            .contains("- Test Source Directory: src/test/java");
    }

    @Test
    public void getFileTree_returns_file_tree_for_maven_project() throws Exception {
        final Project project = project(projectDir);

        final String tree = ProjectMetadataInfo.getFileTree(project);
        then(tree)
            .contains("pom.xml")
            .contains("folder/")
            .contains("  testfile.txt");
    }

    @Test
    public void getMinimalTree_returns_directory_hierarchy_for_maven_project() throws Exception {
        final Project project = project(projectDir);

        final String tree = ProjectMetadataInfo.getMinimalTree(project);
        then(tree)
            .contains("folder/")
            .doesNotContain("pom.xml")
            .doesNotContain("testfile.txt");
    }

    @Test
    public void getMinimalTree_returns_empty_string_for_null_project() {
        then(ProjectMetadataInfo.getMinimalTree(null)).isEmpty();
    }

    @Test
    public void getSrcDir_returns_src_main_java_for_maven_project() throws Exception {
        final Project project = project(projectDir);
        then(ProjectMetadataInfo.getSrcDir(project)).isEqualTo("src/main/java");
    }

    @Test
    public void getSrcResourceDir_returns_src_main_resources_for_maven_project() throws Exception {
        final Project project = project(projectDir);
        then(ProjectMetadataInfo.getSrcResourceDir(project)).isEqualTo("src/main/resources");
    }

    @Test
    public void getTestDir_returns_src_test_java_for_maven_project() throws Exception {
        final Project project = project(projectDir);
        then(ProjectMetadataInfo.getTestDir(project)).isEqualTo("src/test/java");
    }

    @Test
    public void getTestResourceDir_returns_src_test_resources_for_maven_project() throws Exception {
        final Project project = project(projectDir);
        then(ProjectMetadataInfo.getTestResourceDir(project)).isEqualTo("src/test/resources");
    }

    @Test
    public void getSrcDir_returns_empty_string_for_null_project() {
        then(ProjectMetadataInfo.getSrcDir(null)).isEmpty();
    }

    @Test
    public void getTestDir_returns_empty_string_for_null_project() {
        then(ProjectMetadataInfo.getTestDir(null)).isEmpty();
    }

}
