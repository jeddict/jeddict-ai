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
public class NetBeansModuleProjectToolsTest extends TestBase {

    private static final String PROJECT_XML_CONTENT =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <project xmlns="http://www.netbeans.org/ns/project/1">
            <type>org.netbeans.modules.apisupport.project</type>
            <configuration>
                <data xmlns="http://www.netbeans.org/ns/nb-module-project/3">
                    <code-name-base>com.example.mymodule</code-name-base>
                    <module-dependencies>
                        <dependency>
                            <code-name-base>org.openide.util</code-name-base>
                        </dependency>
                        <dependency>
                            <code-name-base>org.netbeans.api.annotations.common</code-name-base>
                        </dependency>
                    </module-dependencies>
                </data>
            </configuration>
        </project>
        """;

    // -----------------------------------------------------------------------
    // BuildMetadataResolver
    // -----------------------------------------------------------------------

    @Test
    public void getProjectType_returns_netbeans_module()
    throws Exception {
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.getProjectType()).isEqualTo("netbeans-module");
    }

    @Test
    public void getBuildFileName_returns_nbproject_project_xml()
    throws Exception {
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.getBuildFileName()).isEqualTo("nbproject/project.xml");
    }

    @Test
    public void getProjectName_returns_code_name_base()
    throws Exception {
        final java.nio.file.Path nbproject = Files.createDirectories(
            projectPath.resolve("nbproject"));
        Files.writeString(nbproject.resolve("project.xml"), PROJECT_XML_CONTENT);
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.getProjectName()).isEqualTo("com.example.mymodule");
    }

    @Test
    public void getProjectName_returns_null_when_no_project_xml()
    throws Exception {
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.getProjectName()).isNull();
    }

    @Test
    public void getProjectMetadata_returns_empty_map()
    throws Exception {
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.getProjectMetadata()).isEmpty();
    }

    // -----------------------------------------------------------------------
    // projectDependencies
    // -----------------------------------------------------------------------

    @Test
    public void projectDependencies_lists_module_code_name_bases()
    throws Exception {
        final java.nio.file.Path nbproject = Files.createDirectories(
            projectPath.resolve("nbproject"));
        Files.writeString(nbproject.resolve("project.xml"), PROJECT_XML_CONTENT);
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        final String deps = tool.projectDependencies();
        then(deps).contains("org.openide.util");
        then(deps).contains("org.netbeans.api.annotations.common");
    }

    @Test
    public void projectDependencies_returns_message_when_no_project_xml()
    throws Exception {
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.projectDependencies()).contains("Unable to read nbproject/project.xml");
    }

    // -----------------------------------------------------------------------
    // projectInfo
    // -----------------------------------------------------------------------

    @Test
    public void projectInfo_includes_type_netbeans_module()
    throws Exception {
        final java.nio.file.Path nbproject = Files.createDirectories(
            projectPath.resolve("nbproject"));
        Files.writeString(nbproject.resolve("project.xml"), PROJECT_XML_CONTENT);
        final NetBeansModuleProjectTools tool = new NetBeansModuleProjectTools(project(projectDir));
        then(tool.projectInfo()).contains("- type: netbeans-module");
    }
}
