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
public class AntProjectToolsTest extends TestBase {

    private static final String BUILD_XML_CONTENT =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <project name="my-ant-app" default="build" basedir=".">
            <target name="build">
                <javac srcdir="src" destdir="build/classes"/>
            </target>
        </project>
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_ant()
    throws Exception {
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("ant");
    }

    @Test
    public void getBuildFileName_returns_build_xml()
    throws Exception {
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("build.xml");
    }

    @Test
    public void getProjectName_returns_name_attribute_from_build_xml()
    throws Exception {
        Files.writeString(projectPath.resolve("build.xml"), BUILD_XML_CONTENT);
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("my-ant-app");
    }

    @Test
    public void getProjectName_returns_null_when_no_build_xml()
    throws Exception {
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_returns_empty_map()
    throws Exception {
        Files.writeString(projectPath.resolve("build.xml"), BUILD_XML_CONTENT);
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectInfo
    // -----------------------------------------------------------------------

    @Test
    public void projectInfo_includes_type_ant_and_project_name()
    throws Exception {
        Files.writeString(projectPath.resolve("build.xml"), BUILD_XML_CONTENT);
        final AntProjectTools tool = new AntProjectTools(project(projectDir));
        final String info = tool.projectInfo();
        then(info).contains("- type: ant");
        then(info).contains("- name: my-ant-app");
    }
}
