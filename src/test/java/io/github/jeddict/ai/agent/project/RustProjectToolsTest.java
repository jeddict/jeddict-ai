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
public class RustProjectToolsTest extends TestBase {

    private static final String CARGO_TOML_CONTENT =
        """
        [package]
        name = "my-crate"
        version = "0.1.0"
        edition = "2021"
        rust-version = "1.70"

        [dependencies]
        serde = "1.0"
        tokio = { version = "1", features = ["full"] }
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_rust()
    throws Exception {
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("rust");
    }

    @Test
    public void getBuildFileName_returns_cargo_toml()
    throws Exception {
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("Cargo.toml");
    }

    @Test
    public void getProjectName_returns_crate_name()
    throws Exception {
        Files.writeString(projectPath.resolve("Cargo.toml"), CARGO_TOML_CONTENT);
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("my-crate");
    }

    @Test
    public void getProjectName_returns_null_when_no_cargo_toml()
    throws Exception {
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_includes_edition_and_rust_version()
    throws Exception {
        Files.writeString(projectPath.resolve("Cargo.toml"), CARGO_TOML_CONTENT);
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        final java.util.Map<String, String> meta = tool.getProjectMetadata();
        then(meta).containsEntry("Edition", "2021");
        then(meta).containsEntry("Rust Version", "1.70");
    }

    @Test
    public void getProjectMetadata_returns_empty_map_when_no_cargo_toml()
    throws Exception {
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_returns_dependencies_section()
    throws Exception {
        Files.writeString(projectPath.resolve("Cargo.toml"), CARGO_TOML_CONTENT);
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("serde = \"1.0\"");
        then(deps).contains("tokio");
    }

    @Test
    public void projectDependencies_returns_message_when_no_dependencies_section()
    throws Exception {
        Files.writeString(projectPath.resolve("Cargo.toml"),
            "[package]\nname = \"myapp\"\nedition = \"2021\"\n");
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("No [dependencies] section found");
    }

    @Test
    public void projectDependencies_returns_message_when_no_cargo_toml()
    throws Exception {
        final RustProjectTools tool = new RustProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read Cargo.toml");
    }
}
