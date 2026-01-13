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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.openide.filesystems.FileObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Tool to manage dependencies in the pom.xml file.
 * Allows adding, removing, updating, and listing dependencies in the project's pom.xml.
 *
 * Author: Assistant
 */
public class MavenTools extends AbstractBuildTool {

    public MavenTools(final String basedir) throws IOException {
        super(basedir, "pom.xml");
    }

    @Tool(
        name = "addMavenDependency",
        value = "Add a dependency to the pom.xml file"
    )
    public String addDependency(String groupId, String artifactId, String version)
    throws Exception {
        progress("Adding dependency: " + groupId + ":" + artifactId + ":" + version);
        return addDependency(groupId, artifactId, version, null);
    }

    @Tool(
        name = "addMavenDependencyWithScope",
        value = "Add a dependency with scope to the pom.xml file"
    )
    public String addDependencyWithScope(String groupId, String artifactId, String version, String scope)
    throws Exception {
        progress("Adding dependency with scope: " + groupId + ":" + artifactId + ":" + version + ":" + scope);
        return addDependency(groupId, artifactId, version, scope);
    }

    private String addDependency(String groupId, String artifactId, String version, String scope)
    throws Exception {
        try {
            final FileObject pomFile = buildFile();
            final Document pom = pomDocument(pomFile.getInputStream());

            final NodeList dependenciesNodes = pom.getElementsByTagName("dependencies");
            Element dependenciesElement;
            if (dependenciesNodes.getLength() == 0) {
                dependenciesElement = pom.createElement("dependencies");
                pom.getDocumentElement().appendChild(dependenciesElement);
            } else {
                dependenciesElement = (Element) dependenciesNodes.item(0);
            }

            // Check if dependency already exists
            NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyList.getLength(); i++) {
                Element dep = (Element) dependencyList.item(i);
                String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
                String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                if (g.equals(groupId) && a.equals(artifactId)) {
                    progress("Dependency already exists: " + groupId + ":" + artifactId);
                    return "Dependency already exists in pom.xml";
                }
            }

            Element dependency = pom.createElement("dependency");

            Element gId = pom.createElement("groupId");
            gId.setTextContent(groupId);
            Element aId = pom.createElement("artifactId");
            aId.setTextContent(artifactId);
            Element ver = pom.createElement("version");
            ver.setTextContent(version);

            dependency.appendChild(gId);
            dependency.appendChild(aId);
            dependency.appendChild(ver);

            if (scope != null && !scope.isEmpty()) {
                Element scopeElement = pom.createElement("scope");
                scopeElement.setTextContent(scope);
                dependency.appendChild(scopeElement);
            }

            dependenciesElement.appendChild(dependency);

            // Write changes back to pom.xml
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(pom);
            StreamResult result = new StreamResult(pomFile.getOutputStream());
            transformer.transform(source, result);

            progress("Dependency added successfully: " + groupId + ":" + artifactId + ":" + version);
            return "Dependency added successfully to pom.xml";
        } catch (Exception e) {
            progress("Failed to add dependency: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name = "removeMavenDependency",
        value = "Remove a dependency from the pom.xml file"
    )
    public String removeDependency(String groupId, String artifactId) throws Exception {
        progress("Removing dependency " + groupId + ": " + artifactId);
        try {
            final FileObject pomFile = buildFile();
            final Document pom = pomDocument(pomFile.getInputStream());
            final NodeList dependenciesNodes = expectedDependencies(pom);

            Element dependenciesElement = (Element) dependenciesNodes.item(0);
            NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyList.getLength(); i++) {
                Element dep = (Element) dependencyList.item(i);
                String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
                String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                if (g.equals(groupId) && a.equals(artifactId)) {
                    dependenciesElement.removeChild(dep);

                    // Write changes back to pom.xml
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    DOMSource source = new DOMSource(pom);
                    StreamResult result = new StreamResult(pomFile.getOutputStream());
                    transformer.transform(source, result);

                    progress("Dependency removed successfully " + groupId + ":" + artifactId);
                    return "Dependency removed successfully from pom.xml";
                }
            }

            throw new Exception("Dependency not found " + groupId + ":" + artifactId);
        } catch (Exception e) {
            progress("Failed to remove dependency: " + e.getMessage());
            throw e;
        }
    }

    @Tool(
        name = "MavenListDependenciesTool_listDependencies",
        value = "List all dependencies in the pom.xml file"
    )
    public String listDependencies() throws Exception {
        progress("Listing dependencies");
        try {
            FileObject pomFile = buildFile();
            Document doc = pomDocument(pomFile.getInputStream());

            NodeList dependenciesNodes = doc.getElementsByTagName("dependencies");
            if (dependenciesNodes.getLength() == 0) {
                progress("No dependencies section found in pom.xml");
                return "No dependencies section found in pom.xml";
            }

            Element dependenciesElement = (Element) dependenciesNodes.item(0);
            NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");

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

    @Tool(
        name = "updateMavenDependencyVersion",
        value = "Update the version of an existing dependency in the pom.xml file"
    )
    public String updateDependencyVersion(String groupId, String artifactId, String newVersion)
    throws Exception {
        progress("Updating dependency version: " + groupId + ":" + artifactId + ":" + newVersion);
        try {
            final FileObject pomFile = buildFile();
            final Document pom = pomDocument(pomFile.getInputStream());
            final NodeList dependenciesNodes = expectedDependencies(pom);

            Element dependenciesElement = (Element) dependenciesNodes.item(0);
            NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
            for (int i = 0; i < dependencyList.getLength(); i++) {
                Element dep = (Element) dependencyList.item(i);
                String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
                String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                if (g.equals(groupId) && a.equals(artifactId)) {
                    dep.getElementsByTagName("version").item(0).setTextContent(newVersion);

                    // Write changes back to pom.xml
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Transformer transformer = transformerFactory.newTransformer();
                    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                    DOMSource source = new DOMSource(pom);
                    StreamResult result = new StreamResult(pomFile.getOutputStream());
                    transformer.transform(source, result);

                    progress("Dependency version updated successfully: " + groupId + ":" + artifactId + ":" + newVersion);
                    return "Dependency version updated successfully in pom.xml";
                }
            }

            throw new Exception("Dependency " + groupId + ":" + artifactId + " not found");
        } catch (Exception e) {
            progress("Failed to update dependency: " + e.getMessage());

            throw e;
        }
    }

    @Tool(
        name= "mavenDependencyExists",
        value= "Check if a dependency exists in the pom.xml file"
    )
    public boolean dependencyExists(String groupId, String artifactId)
    throws Exception {
        progress("Checking dependency existence: " + groupId + ":" + artifactId);
        try {
            final FileObject pomFile = buildFile();
            final Document doc = pomDocument(pomFile.getInputStream());

            NodeList dependenciesNodes = doc.getElementsByTagName("dependencies");
            if (dependenciesNodes.getLength() > 0) {
                Element dependenciesElement = (Element) dependenciesNodes.item(0);
                NodeList dependencyList = dependenciesElement.getElementsByTagName("dependency");
                for (int i = 0; i < dependencyList.getLength(); i++) {
                    Element dep = (Element) dependencyList.item(i);
                    String g = dep.getElementsByTagName("groupId").item(0).getTextContent();
                    String a = dep.getElementsByTagName("artifactId").item(0).getTextContent();
                    if (g.equals(groupId) && a.equals(artifactId)) {
                        progress("Dependency exists: " + groupId + ":" + artifactId);
                        return true;
                    }
                }
            }

            progress("Dependency does not exist: " + groupId + ":" + artifactId);
            return false;
        } catch (Exception e) {
            progress("Failed to check dependency existence: " + e.getMessage());

            throw e;
        }
    }

    private org.w3c.dom.Document pomDocument(final InputStream is)
        throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        org.w3c.dom.Document doc = dBuilder.parse(is);
        doc.getDocumentElement().normalize();

        return doc;
    }

    /**
     * Checks if the <dependencies> section exists and returns the list of
     * dependencies. If the section does not exist, or no there dependencies are
     * there, an exception is thrown.
     *
     * @param pom the pom document
     *
     * @return the list of exceptions if any
     *
     * @throws Exception if the section does not exist
     */
    private NodeList expectedDependencies(final org.w3c.dom.Document pom) throws Exception {
        final NodeList dependencies = pom.getElementsByTagName("dependencies");
        if (dependencies.getLength() == 0) {
            throw new Exception("No dependencies section found in pom.xml");
        }

        return dependencies;
    }
}