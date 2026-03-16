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
public class DockerProjectToolsTest extends TestBase {

    private static final String DOCKERFILE_CONTENT =
        """
        FROM eclipse-temurin:21-jre
        WORKDIR /app
        COPY target/*.jar app.jar
        ENTRYPOINT ["java", "-jar", "app.jar"]
        """;

    private static final String DOCKERFILE_MULTI_STAGE =
        """
        FROM maven:3.9-eclipse-temurin-21 AS build
        WORKDIR /src
        COPY . .
        RUN mvn package

        FROM eclipse-temurin:21-jre
        COPY --from=build /src/target/*.jar app.jar
        ENTRYPOINT ["java", "-jar", "app.jar"]
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_docker()
    throws Exception {
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("docker");
    }

    @Test
    public void getProjectName_returns_null()
    throws Exception {
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getBuildFileName_returns_dockerfile_when_present()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_CONTENT);
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("Dockerfile");
    }

    @Test
    public void getBuildFileName_returns_compose_file_when_no_dockerfile()
    throws Exception {
        Files.writeString(projectPath.resolve("docker-compose.yml"),
            "version: '3'\nservices:\n  app:\n    image: myapp\n");
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("docker-compose.yml");
    }

    @Test
    public void getBuildFileName_returns_null_when_no_docker_files()
    throws Exception {
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_base_image_from_dockerfile()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_CONTENT);
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Base Images", "eclipse-temurin:21-jre");
    }

    @Test
    public void getProjectMetadata_includes_all_from_images_in_multi_stage()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_MULTI_STAGE);
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        final String baseImages = tool.getProjectMetadata().get("Base Images");
        then(baseImages).contains("maven:3.9-eclipse-temurin-21");
        then(baseImages).contains("eclipse-temurin:21-jre");
    }

    @Test
    public void getProjectMetadata_includes_compose_file_when_present()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_CONTENT);
        Files.writeString(projectPath.resolve("docker-compose.yml"),
            "version: '3'\nservices:\n  app:\n    image: myapp\n");
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).containsEntry("Compose File", "docker-compose.yml");
    }

    @Test
    public void getProjectMetadata_returns_empty_when_no_docker_files()
    throws Exception {
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_returns_from_images()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_CONTENT);
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("eclipse-temurin:21-jre");
    }

    @Test
    public void projectDependencies_returns_all_from_images_in_multi_stage()
    throws Exception {
        Files.writeString(projectPath.resolve("Dockerfile"), DOCKERFILE_MULTI_STAGE);
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("maven:3.9-eclipse-temurin-21");
        then(deps).contains("eclipse-temurin:21-jre");
    }

    @Test
    public void projectDependencies_returns_message_when_no_dockerfile()
    throws Exception {
        final DockerProjectTools tool = new DockerProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No Dockerfile found");
    }
}
