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
package io.github.jeddict.ai.agent;

import io.github.jeddict.ai.test.TestBase;
import java.io.File;
import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.netbeans.modules.maven.NbMavenProjectFactory;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public class ProjectToolsTest extends TestBase {

    final NbMavenProjectFactory projectFactory = new NbMavenProjectFactory();

    @Test
    public void projectInfo_returns_project_metadata_as_text()
    throws Exception {
        ProjectTools tools = new ProjectTools(project("src/test/resources/projects/minimal"));
        then(tools.projectInfo()).isEmpty();

        tools = new ProjectTools(project("src/test/resources/projects/jdk"));
        then(tools.projectInfo()).isEqualTo("Java Version: 11");

        tools = new ProjectTools(project("src/test/resources/projects/jakarta"));
        then(tools.projectInfo()).isEqualTo("EE Version: jakarta\nEE Import Prefix: jakarta\nJava Version: 21");

        tools = new ProjectTools(project("src/test/resources/projects/javax"));
        then(tools.projectInfo()).isEqualTo("EE Version: javax\nEE Import Prefix: javax\nJava Version: 11");
    }

    // --------------------------------------------------------- private methods

    private Project project(final String pom) throws IOException {
        final FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(new File(pom)));
        return projectFactory.loadProject(
            fo,
            new ProjectState() {
                @Override
                public void markModified() {}

                @Override
                public void notifyDeleted() throws IllegalStateException {}
            }
        );
    }

}
