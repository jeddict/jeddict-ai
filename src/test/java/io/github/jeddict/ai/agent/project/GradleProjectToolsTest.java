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
package io.github.jeddict.ai.agent.project;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.TestBase;
import java.nio.file.Files;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

@CacioTest
public class GradleProjectToolsTest extends TestBase {

    private static final String BUILD_GRADLE_CONTENT =
        """
        plugins {
            id 'java'
        }
        sourceCompatibility = '17'
        dependencies {
            implementation 'com.google.guava:guava:32.0.0-jre'
            testImplementation 'junit:junit:4.13.2'
        }
        """;

    private static final String BUILD_GRADLE_KTS_CONTENT =
        """
        plugins {
            kotlin("jvm") version "1.9.0"
        }
        kotlin {
            jvmTarget = "17"
        }
        dependencies {
            implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
        }
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_gradle()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("gradle");
    }

    @Test
    public void getProjectName_returns_null()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getBuildFileName_returns_build_gradle_when_present()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle"), BUILD_GRADLE_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("build.gradle");
    }

    @Test
    public void getBuildFileName_returns_null_when_no_build_file()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_java_version_from_source_compatibility()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle"), BUILD_GRADLE_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Java Version", "17");
    }

    @Test
    public void getProjectMetadata_includes_java_version_from_jvm_target_in_kts()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle.kts"), BUILD_GRADLE_KTS_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Java Version", "17");
    }

    @Test
    public void getProjectMetadata_returns_empty_when_no_build_file()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // jdkVersion
    // -----------------------------------------------------------------------

    @Test
    public void jdkVersion_returns_version_from_build_gradle()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle"), BUILD_GRADLE_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.jdkVersion()).isEqualTo("17");
    }

    @Test
    public void jdkVersion_returns_message_when_no_build_file()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.jdkVersion()).contains("No sourceCompatibility");
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_returns_groovy_dsl_dependencies()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle"), BUILD_GRADLE_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("com.google.guava:guava:32.0.0-jre");
        then(deps).contains("junit:junit:4.13.2");
    }

    @Test
    public void projectDependencies_returns_kotlin_dsl_dependencies()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle.kts"), BUILD_GRADLE_KTS_CONTENT);
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("org.jetbrains.kotlin:kotlin-stdlib:1.9.0");
    }

    @Test
    public void projectDependencies_returns_message_when_no_build_file()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read Gradle build file");
    }

    @Test
    public void projectDependencies_returns_message_when_no_deps_declared()
    throws Exception {
        Files.writeString(projectPath.resolve("build.gradle"),
            "plugins { id 'java' }\n");
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No dependencies declared");
    }

    // -----------------------------------------------------------------------
    // Wrapper / command resolution
    // -----------------------------------------------------------------------

    @Test
    public void resolveRunCommand_uses_gradle_when_no_wrapper_present()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveRunCommand("com.example.Main"))
            .isEqualTo("gradle run --main-class=com.example.Main");
    }

    @Test
    public void resolveRunCommand_uses_gradlew_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("gradlew"));
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveRunCommand("com.example.App"))
            .isEqualTo("./gradlew run --main-class=com.example.App");
    }

    @Test
    public void resolveBuildCommand_uses_gradle_when_no_wrapper_present()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveBuildCommand()).isEqualTo("gradle build");
    }

    @Test
    public void resolveBuildCommand_uses_gradlew_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("gradlew"));
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveBuildCommand()).isEqualTo("./gradlew build");
    }

    @Test
    public void resolveTestCommand_uses_gradle_when_no_wrapper_present()
    throws Exception {
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveTestCommand()).isEqualTo("gradle test");
    }

    @Test
    public void resolveTestCommand_uses_gradlew_when_wrapper_present()
    throws Exception {
        Files.createFile(projectPath.resolve("gradlew"));
        final GradleProjectTools tool = new GradleProjectTools(project(projectDir));
        then(tool.resolveTestCommand()).isEqualTo("./gradlew test");
    }

}
