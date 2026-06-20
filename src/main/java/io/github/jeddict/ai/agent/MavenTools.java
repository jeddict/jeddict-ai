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
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;

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
            final Document pom = pomDocument(pomFile.getInputStream());
            final Element projectElement = pom.getDocumentElement();
            final Element dependenciesElement = ensureDependenciesElement(pom, projectElement);

            if (findDependency(dependenciesElement, groupId, artifactId) != null) {
                progress("Dependency already exists: " + groupId + ":" + artifactId);
                return "Dependency already exists in pom.xml";
            }

            final Element dependency = pom.createElement("dependency");
            appendTextElement(pom, dependency, "groupId", groupId);
            appendTextElement(pom, dependency, "artifactId", artifactId);
            appendTextElement(pom, dependency, "version", version);
            if (scope != null && !scope.isBlank()) {
                appendTextElement(pom, dependency, "scope", scope);
            }
            if (type != null && !type.isBlank()) {
                appendTextElement(pom, dependency, "type", type);
            }
            if (classifier != null && !classifier.isBlank()) {
                appendTextElement(pom, dependency, "classifier", classifier);
            }

            dependenciesElement.appendChild(dependency);
            writeDocument(pom, pomFile);
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
            final Document pom = pomDocument(pomFile.getInputStream());
            final Element dependenciesElement = getDependenciesElement(pom);
            final Element dependency = findDependency(dependenciesElement, groupId, artifactId);
            if (dependency != null) {
                dependenciesElement.removeChild(dependency);
                writeDocument(pom, pomFile);
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
            FileObject pomFile = buildFile();
            Document doc = pomDocument(pomFile.getInputStream());
            NodeList dependencyList = getDependenciesElement(doc).getElementsByTagName("dependency");
            List<String> dependencies = new ArrayList<>();
            for (int i = 0; i < dependencyList.getLength(); i++) {
                Element dep = (Element) dependencyList.item(i);
                String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
                String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                String v = dep.getElementsByTagName("version").item(0).getTextContent();
                String scope = "";
                NodeList scopeNodes = dep.getElementsByTagName("scope");
                if (scopeNodes.getLength() > 0) {
                    scope = scopeNodes.item(0).getTextContent();
                }
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
            final Document pom = pomDocument(pomFile.getInputStream());
            final Element dependenciesElement = getDependenciesElement(pom);
            final Element dependency = findDependency(dependenciesElement, groupId, artifactId);
            if (dependency != null) {
                final Node versionNode = dependency.getElementsByTagName("version").item(0);
                if (versionNode == null) {
                    appendTextElement(pom, dependency, "version", newVersion);
                } else {
                    versionNode.setTextContent(newVersion);
                }
                writeDocument(pom, pomFile);
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
            final Document doc = pomDocument(pomFile.getInputStream());
            final Element dependenciesElement = getDependenciesElement(doc);
            final boolean exists = findDependency(dependenciesElement, groupId, artifactId) != null;
            progress(exists ? "Dependency exists: " + groupId + ":" + artifactId : "Dependency does not exist: " + groupId + ":" + artifactId);
            return exists;
        } catch (Exception e) {
            progress("Failed to check dependency existence: " + e.getMessage());
            throw e;
        }
    }

    private void refreshIdeState() {
        try {
            NbMavenProject.fireMavenProjectReload(project);
        } catch (Exception ex) {
            progress("IDE refresh after pom.xml update failed: " + ex.getMessage());
        }
    }

    private Document pomDocument(final InputStream is)
        throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private Element ensureDependenciesElement(final Document pom, final Element projectElement) {
        final NodeList dependencies = pom.getElementsByTagName("dependencies");
        if (dependencies.getLength() > 0) {
            return (Element) dependencies.item(0);
        }
        final Element dependenciesElement = pom.createElement("dependencies");
        projectElement.appendChild(dependenciesElement);
        return dependenciesElement;
    }

    private Element getDependenciesElement(final Document pom) throws Exception {
        final NodeList dependencies = pom.getElementsByTagName("dependencies");
        if (dependencies.getLength() == 0) {
            throw new Exception("No dependencies section found in pom.xml");
        }
        return (Element) dependencies.item(0);
    }

    private Element findDependency(final Element dependenciesElement, final String groupId, final String artifactId) {
        final NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
        for (int i = 0; i < dependencyList.getLength(); i++) {
            final Element dep = (Element) dependencyList.item(i);
            final String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
            final String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
            if (g.equals(groupId) && a.equals(artifactId)) {
                return dep;
            }
        }
        return null;
    }

    private void appendTextElement(final Document doc, final Element parent, final String name, final String value) {
        final Element element = doc.createElement(name);
        element.setTextContent(value);
        parent.appendChild(element);
    }

    private void writeDocument(final Document doc, final FileObject pomFile) throws Exception {
        javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
        javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(pomFile.getOutputStream());
        transformer.transform(source, result);
    }

    private FileObject buildFile() throws Exception {
        final FileObject projectDir = FileUtil.toFileObject(basepath);
        final FileObject pomFile = projectDir.getFileObject("pom.xml");
        if (pomFile == null || !pomFile.isValid()) {
            throw new Exception("pom.xml not found in project directory");
        }
        return pomFile;
    }
}
