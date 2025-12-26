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
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.openide.filesystems.FileObject;

/**
 * Tool to manage dependencies in the build.gradle file.
 * Allows adding, removing, updating, and listing dependencies in the project's build.gradle.
 *
 * Author: Assistant
 */
public class GradleTools extends AbstractBuildTool {

    public GradleTools(final String basedir) {
        super(basedir, "build.gradle");
    }

    @Tool(
        name = "addGradleDependency",
        value = "Add a dependency to the build.gradle file"
    )
    @ToolPolicy(READWRITE)
    public String addDependency(String configuration, String dependencyNotation)
    throws Exception {
        progress("Adding dependency: " + configuration + ":" + dependencyNotation);
        try {
            final FileObject gradleFile = buildFile();

            // Read the existing content
            String content = gradleFile.asText();

            String dependencyString = configuration + " '" + dependencyNotation + "'";

            if (content.contains(dependencyString)) {
                progress("Dependency already exists: " + dependencyString);
                return "Dependency already exists in build.gradle";
            }

            // Find dependencies block and add the dependency inside it
            String updatedContent;
            int dependenciesIndex = content.indexOf("dependencies {");
            if (dependenciesIndex == -1) {
                // No dependencies block, add one at the end
                updatedContent = content + "\n\ndependencies {\n    " + dependencyString + "\n}\n";
            } else {
                int insertIndex = content.indexOf("}", dependenciesIndex);
                if (insertIndex == -1) {
                    // Malformed file? Just append
                    updatedContent = content + "\n    " + dependencyString + "\n";
                } else {
                    updatedContent = content.substring(0, insertIndex) + "    " + dependencyString + "\n" + content.substring(insertIndex);
                }
            }

            // Write changes back to build.gradle
            try (OutputStream os = gradleFile.getOutputStream()) {
                os.write(updatedContent.getBytes());
            }

            progress("Dependency added successfully: " + dependencyString);
            return "Dependency added successfully to build.gradle";
        } catch (Exception e) {
            progress("Failed to add dependency: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name = "removeGradleDependency",
        value = "Remove a dependency from the build.gradle file"
    )
    @ToolPolicy(READWRITE)
    public String removeDependency(String configuration, String dependencyNotation)
    throws Exception {
        progress("Removing dependency: " + configuration + ":" + dependencyNotation);
        try {
            final FileObject gradleFile = buildFile();

            // Read the existing content
            String content = new String(gradleFile.asBytes());

            String dependencyString = configuration + " '" + dependencyNotation + "'";

            if (!content.contains(dependencyString)) {
                progress("Dependency not found: " + dependencyString);
                return "Dependency not found in build.gradle";
            }

            // Remove the dependency string
            String updatedContent = content.replace(dependencyString + "\n", "");
            updatedContent = updatedContent.replace(dependencyString, "");

            // Write changes back to build.gradle
            try (OutputStream os = gradleFile.getOutputStream()) {
                os.write(updatedContent.getBytes());
            }

            progress("Dependency removed successfully: " + dependencyString);
            return "Dependency removed successfully from build.gradle";
        } catch (Exception e) {
            progress("Failed to remove dependency: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name = "listGradleDependencies",
        value = "List all dependencies in the build.gradle file"
    )
    @ToolPolicy(READONLY)
    public String listDependencies() throws Exception {
        progress("Listing dependencies");
        try {
            final FileObject gradleFile = buildFile();

            // Read the existing content
            String content = new String(gradleFile.asBytes());

            // Extract dependencies block
            int dependenciesIndex = content.indexOf("dependencies {");
            if (dependenciesIndex == -1) {
                progress("No dependencies section found in build.gradle");
                return "No dependencies section found in build.gradle";
            }
            int endIndex = content.indexOf("}", dependenciesIndex);
            if (endIndex == -1) {
                progress("Malformed dependencies section in build.gradle");
                return "Malformed dependencies section in build.gradle";
            }

            String dependenciesBlock = content.substring(dependenciesIndex, endIndex + 1);
            String[] lines = dependenciesBlock.split("\n");
            List<String> dependencies = new ArrayList<>();
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith("dependencies") && !line.equals("{") && !line.equals("}") && !line.isEmpty()) {
                    dependencies.add(line);
                }
            }

            progress("Dependencies listed");
            if (dependencies.isEmpty()) {
                return "No dependencies found in build.gradle";
            }
            return String.join("\n", dependencies);
        } catch (Exception e) {
            progress("Failed to list dependencies: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name = "updateGradleDependency",
        value = "Update a dependency in the build.gradle file"
    )
    @ToolPolicy(READWRITE)
    public String updateDependency(String configuration, String oldDependencyNotation, String newDependencyNotation)
    throws Exception {
        progress("Updating dependency: " + configuration + ":" + oldDependencyNotation + " -> " + newDependencyNotation);
        try {
            final FileObject gradleFile = buildFile();

            // Read the existing content
            String content = new String(gradleFile.asBytes());

            String oldDependencyString = configuration + " '" + oldDependencyNotation + "'";
            String newDependencyString = configuration + " '" + newDependencyNotation + "'";

            if (!content.contains(oldDependencyString)) {
                progress("Dependency not found: " + oldDependencyString);
                return "Dependency not found in build.gradle";
            }

            String updatedContent = content.replace(oldDependencyString, newDependencyString);

            // Write changes back to build.gradle
            try (OutputStream os = gradleFile.getOutputStream()) {
                os.write(updatedContent.getBytes());
            }

            progress("Dependency updated successfully: " + newDependencyString);
            return "Dependency updated successfully in build.gradle";
        } catch (Exception e) {
            progress("Failed to update dependency: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name = "gradleDependencyExists",
        value = "Check if a dependency exists in the build.gradle file"
    )
    @ToolPolicy(READONLY)
    public boolean dependencyExists(String configuration, String dependencyNotation)
    throws Exception {
        progress("Checking dependency existence: " + configuration + ":" + dependencyNotation);
        try {
            final FileObject gradleFile = buildFile();

            // Read the existing content
            String content = new String(gradleFile.asBytes());

            String dependencyString = configuration + " '" + dependencyNotation + "'";

            boolean exists = content.contains(dependencyString);
            progress((exists ? "Dependency exists" : "Dependency does not exist") + ": " + dependencyString);
            return exists;
        } catch (Exception e) {
            progress("Failed to check dependency existence: " + e.getMessage());

            throw e;
        }
    }

}