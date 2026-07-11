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

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import java.io.File;

public class MavenTools extends AbstractTool {

    private static final int TIMEOUT_MIN = 2;
    private final Project project;

    public MavenTools(final Project project) throws IOException {
        super(FileUtil.toFile(project.getProjectDirectory()).toString());
        this.project = project;
    }

    @Tool(name = "addMavenDependency", value = "Add a dependency to a maven project")
    @ToolPolicy(READWRITE)
    public String addDependency(
        @P("group id") final String groupId,
        @P("artifact id") final String artifactId,
        @P("version") final String version,
        @P("scope") final String scope,
        @P("type") final String type,
        @P("classifier") final String classifier
    ) throws Exception {
        progress("Adding dependency with scope: " + groupId + ":" + artifactId + ":" + version + ":" + scope);
        try {
            final FileObject pomFile = buildFile();
            Model model = readModel(pomFile);

            boolean exists = false;
            for (Dependency dep : model.getDependencies()) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                progress("Dependency already exists: " + groupId + ":" + artifactId);
                return "Dependency already exists in pom.xml";
            }

            Dependency dependency = new Dependency();
            dependency.setGroupId(groupId);
            dependency.setArtifactId(artifactId);

            if (version != null && !version.isBlank()) {
                dependency.setVersion(version);
            }
            if (scope != null && !scope.isBlank()) {
                dependency.setScope(scope);
            }
            if (type != null && !type.isBlank()) {
                dependency.setType(type);
            }
            if (classifier != null && !classifier.isBlank()) {
                dependency.setClassifier(classifier);
            }

            model.addDependency(dependency);
            writeModel(model, pomFile);
            refreshIdeState();

            progress("Dependency added successfully: " + groupId + ":" + artifactId + ":" + version);
            return "Dependency added successfully to pom.xml";
        } catch (Exception e) {
            progress("Failed to add dependency: " + e.getMessage());
            throw e;
        }
    }

    @Tool(name = "removeMavenDependency", value = "Remove a dependency from the pom.xml file")
    @ToolPolicy(READWRITE)
    public String removeDependency(
        @P("group id") final String groupId,
        @P("artifact id") final String artifactId
    ) throws Exception {
        progress("Removing dependency " + groupId + ": " + artifactId);
        try {
            final FileObject pomFile = buildFile();
            Model model = readModel(pomFile);

            Dependency target = null;
            for (Dependency dep : model.getDependencies()) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    target = dep;
                    break;
                }
            }

            if (target != null) {
                model.removeDependency(target);
                writeModel(model, pomFile);
                refreshIdeState();
                progress("Dependency removed successfully " + groupId + ":" + artifactId);
                return "Dependency removed successfully from pom.xml";
            }
            throw new Exception("Dependency not found " + groupId + ":" + artifactId);
        } catch (Exception e) {
            progress("Failed to remove dependency: " + e.getMessage());
            throw e;
        }
    }

    @Tool(name = "MavenListDependenciesTool_listDependencies", value = "List all dependencies in the pom.xml file")
    @ToolPolicy(READWRITE)
    public String listDependencies() throws Exception {
        progress("Listing dependencies");
        try {
            final FileObject pomFile = buildFile();
            Model model = readModel(pomFile);

            List<String> dependencies = new ArrayList<>();
            for (Dependency dep : model.getDependencies()) {
                String g = dep.getGroupId();
                String a = dep.getArtifactId();
                String v = dep.getVersion() != null ? dep.getVersion() : "";
                String scope = dep.getScope() != null ? dep.getScope() : "";

                dependencies.add(String.format("%s:%s:%s%s", g, a, v, scope.isEmpty() ? "" : ":" + scope));
            }
            return String.join("\n", dependencies);
        } catch (Exception e) {
            progress("Failed to list dependencies: " + e.getMessage());
            throw e;
        }
    }

    @Tool(name = "updateMavenDependencyVersion", value = "Update the version of an existing dependency in the pom.xml file")
    @ToolPolicy(READWRITE)
    public String updateDependencyVersion(
        @P("group id") final String groupId,
        @P("artifact id") final String artifactId,
        @P("new version") final String newVersion
    ) throws Exception {
        progress("Updating dependency version: " + groupId + ":" + artifactId + ":" + newVersion);
        try {
            final FileObject pomFile = buildFile();
            Model model = readModel(pomFile);

            Dependency target = null;
            for (Dependency dep : model.getDependencies()) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    target = dep;
                    break;
                }
            }

            if (target != null) {
                target.setVersion(newVersion);
                writeModel(model, pomFile);
                refreshIdeState();
                progress("Dependency version updated successfully: " + groupId + ":" + artifactId + ":" + newVersion);
                return "Dependency version updated successfully in pom.xml";
            }
            throw new Exception("Dependency " + groupId + ":" + artifactId + " not found");
        } catch (Exception e) {
            progress("Failed to update dependency: " + e.getMessage());
            throw e;
        }
    }

    @Tool(name = "mavenDependencyExists", value = "Check if a dependency exists in the pom.xml file")
    @ToolPolicy(READONLY)
    public boolean dependencyExists(
        @P("group id") final String groupId,
        @P("artifact id") final String artifactId
    ) throws Exception {
        progress("Checking dependency existence: " + groupId + ":" + artifactId);
        try {
            final FileObject pomFile = buildFile();
            Model model = readModel(pomFile);

            boolean exists = false;
            for (Dependency dep : model.getDependencies()) {
                if (groupId.equals(dep.getGroupId()) && artifactId.equals(dep.getArtifactId())) {
                    exists = true;
                    break;
                }
            }
            progress(exists ? "Dependency exists: " + groupId + ":" + artifactId : "Dependency does not exist: " + groupId + ":" + artifactId);
            return exists;
        } catch (Exception e) {
            progress("Failed to check dependency existence: " + e.getMessage());
            throw e;
        }
    }

    private void refreshIdeState() {
        NbMavenProject.fireMavenProjectReload(project);
    }

    private Model readModel(FileObject pomFile) throws Exception {
        try (InputStream is = pomFile.getInputStream()) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(is);
        }
    }

    private void writeModel(Model model, FileObject pomFile) throws Exception {
        try (OutputStream os = pomFile.getOutputStream()) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(os, model);
        }
    }


    private FileObject buildFile() throws Exception {
        final File file = ((java.nio.file.Path) basepath).toFile();

        final FileObject projectDir = FileUtil.toFileObject(file);
        final FileObject pomFile = projectDir.getFileObject("pom.xml");
        if (pomFile == null || !pomFile.isValid()) {
            throw new Exception("pom.xml not found in project directory");
        }
        return pomFile;
    }
}