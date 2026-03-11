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
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import java.io.IOException;

/**
 * Tool to return information about the project: jdk version, j2ee version,
 * and file tree structure.
 */
public class ProjectTools extends AbstractTool {

    private final Project project;

    public ProjectTools(final Project project) throws IOException  {
        super(project.getProjectDirectory().getPath());
        this.project = project;
    }

    @Tool(
        name = "projectInfo",
        value = "Return information about the project: jdk version, j2ee version"
    )
    @ToolPolicy(READONLY)
    public String projectInfo()
    throws Exception {
        progress("Gathering project info: " + project);
        return ProjectMetadataInfo.get(project);
    }

    @Tool(
        name = "projectFileTree",
        value = "Return the full file tree structure of the project directory, including all files and subdirectories"
    )
    @ToolPolicy(READONLY)
    public String projectFileTree()
    throws Exception {
        progress("Gathering project file tree: " + project);
        return ProjectMetadataInfo.getFileTree(project);
    }

    @Tool(
        name = "projectMinimalTree",
        value = "Return the minimal directory hierarchy of the project, showing only the package structure without individual files"
    )
    @ToolPolicy(READONLY)
    public String projectMinimalTree()
    throws Exception {
        progress("Gathering project minimal tree: " + project);
        return ProjectMetadataInfo.getMinimalTree(project);
    }

    @Tool(
        name = "projectSrcDir",
        value = "Return the path of the main Java sources directory (e.g. src/main/java)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcDir()
    throws Exception {
        progress("Gathering project source directory: " + project);
        return ProjectMetadataInfo.getSrcDir(project);
    }

    @Tool(
        name = "projectSrcResourceDir",
        value = "Return the path of the main resources directory (e.g. src/main/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectSrcResourceDir()
    throws Exception {
        progress("Gathering project source resources directory: " + project);
        return ProjectMetadataInfo.getSrcResourceDir(project);
    }

    @Tool(
        name = "projectTestDir",
        value = "Return the path of the test Java sources directory (e.g. src/test/java)"
    )
    @ToolPolicy(READONLY)
    public String projectTestDir()
    throws Exception {
        progress("Gathering project test directory: " + project);
        return ProjectMetadataInfo.getTestDir(project);
    }

    @Tool(
        name = "projectTestResourceDir",
        value = "Return the path of the test resources directory (e.g. src/test/resources)"
    )
    @ToolPolicy(READONLY)
    public String projectTestResourceDir()
    throws Exception {
        progress("Gathering project test resources directory: " + project);
        return ProjectMetadataInfo.getTestResourceDir(project);
    }
}