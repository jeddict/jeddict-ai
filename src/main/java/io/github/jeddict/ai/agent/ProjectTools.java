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

import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import org.netbeans.api.project.Project;

/**
 * Tool to return information about the project: jdk version, j2ee version
 */
public class ProjectTools extends AbstractTool {

    private final Project project;

    public ProjectTools(final Project project) {
        super(project.getProjectDirectory().getPath());
        this.project = project;
    }

    @Tool(
        name = "projectInfo",
        value = "Return information about the project: jdk version, j2ee version"
    )
    public String projectInfo()
    throws Exception {
        progress("Gathering project info: " + project);
        return ProjectMetadataInfo.get(project);
    }
}