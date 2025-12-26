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
package io.github.jeddict.ai.scanner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.netbeans.api.project.Project;
import org.netbeans.modules.maven.api.NbMavenProject;

/**
 *
 * @author Gaurav Gupta
 *
 * TODO: add more info about the project like:
 *       - base directory
 *       - build system (maven, ant, gradle)
 *       - project name
 */
public class ProjectMetadataInfo {

    private static final Map<Project, CachedResult> cache = new HashMap<>();

    public static String get(Project project) {
        CachedResult cachedResult = getCachedResult(project);

        // Check if cachedResult is null and handle accordingly
        if (cachedResult == null) {
            return "Project Metadata: Unable to retrieve metadata for the specified project.";
        }

        StringBuilder sb = new StringBuilder();

        // Append EE Version with appropriate label if importPrefix is "jakarta"
        if (cachedResult.getEeVersion() != null) {
            if ("jakarta".equals(cachedResult.getImportPrefix())) {
                sb.append("EE Version: ").append(cachedResult.getEeVersion()).append("\n");
            } else {
                sb.append("EE Version: ").append(cachedResult.getEeVersion()).append("\n");
            }
        }
        if (cachedResult.getImportPrefix() != null) {
            sb.append("EE Import Prefix: ").append(cachedResult.getImportPrefix()).append("\n");
        }
        if (cachedResult.getJdkVersion() != null) {
            sb.append("Java Version: ").append(cachedResult.getJdkVersion()).append("\n");
        }

        return sb.toString().trim();
    }

    public static CachedResult getCachedResult(Project project) {
        try {
            // Check if the project is cached
            CachedResult cachedResult = cache.get(project);

            // Get project modification timestamp
            NbMavenProject nbMavenProject = project.getLookup().lookup(NbMavenProject.class);
            if (nbMavenProject == null) {
                return null; // Not a Maven project
            }

            // Get the pom.xml FileObject
            File pomFile = nbMavenProject.getMavenProject().getFile();
            long lastModified = pomFile.lastModified();

            // Invalidate cache if timestamp has changed
            if (cachedResult == null || cachedResult.timestamp < lastModified) {
                // Get the MavenProject
                MavenProject mavenProject = nbMavenProject.getMavenProject();

                // Determine Jakarta EE version
                String eeVersion = getEEVersionFromDependencies(mavenProject);

                // Determine JDK version
                String jdkVersion = getJdkVersionFromPom(mavenProject);

                // Determine the import prefix
                String importPrefix = null;
                if (eeVersion != null) {
                    if (eeVersion.startsWith("jakarta")) {
                        if (eeVersion.equals("jakarta-8.0.0")) {
                            importPrefix = "javax"; // Special case for Jakarta EE 8
                        } else {
                            importPrefix = "jakarta";
                        }
                    } else if (eeVersion.startsWith("javax")) {
                        importPrefix = "javax";
                    }
                }

                // Cache the result
                CachedResult result = new CachedResult(importPrefix, eeVersion, jdkVersion, lastModified);
                cache.put(project, result);

                return result;
            }

            // Return cached result
            return cachedResult;

        } catch (Exception e) {
            e.printStackTrace();
            return null; // In case of errors
        }
    }

    private static String getEEVersionFromDependencies(MavenProject mavenProject) {
        // Look for Jakarta EE or Java EE dependencies in the pom.xml
        for (org.apache.maven.model.Dependency dependency : mavenProject.getDependencies()) {
            if (dependency.getGroupId().equals("jakarta.platform")) {
                if (dependency.getVersion().startsWith("8.0")) {
                    return "jakarta-8.0.0"; // Special case for Jakarta EE 8
                }
                return "jakarta"; // Other versions of Jakarta EE
            }
            if (dependency.getGroupId().startsWith("javax.")) {
                return "javax"; // Java EE dependencies
            }
        }
        return null; // Return null if no matching dependencies are found
    }

    private static String getJdkVersionFromPom(MavenProject mavenProject) {
        // Check for JDK version in Maven properties
        String source = mavenProject.getProperties().getProperty("maven.compiler.source");
        String target = mavenProject.getProperties().getProperty("maven.compiler.target");

        // Return source version if available; fallback to target
        if (source != null) {
            return source;
        }
        if (target != null) {
            return target;
        }
        for (Plugin plugin : mavenProject.getBuildPlugins()) {
            if ("maven-compiler-plugin".equals(plugin.getArtifactId())) {
                Object configuration = plugin.getConfiguration();
                if (configuration instanceof Xpp3Dom dom) {
                    Xpp3Dom sourceNode = dom.getChild("source");
                    if (sourceNode != null) {
                        return sourceNode.getValue();
                    }
                }
            }
        }

        // Return null if no JDK version is found
        return null;
    }

    // Helper class to store cached result and its timestamp
    public static class CachedResult {

        String importPrefix; // "javax" or "jakarta"
        String eeVersion; // Jakarta EE or Java EE version
        String jdkVersion; // JDK version used
        long timestamp; // Timestamp of cache creation

        CachedResult(String importPrefix, String eeVersion, String jdkVersion, long timestamp) {
            this.importPrefix = importPrefix;
            this.eeVersion = eeVersion;
            this.jdkVersion = jdkVersion;
            this.timestamp = timestamp;
        }

        public String getImportPrefix() {
            return importPrefix;
        }

        public String getEeVersion() {
            return eeVersion;
        }

        public String getJdkVersion() {
            return jdkVersion;
        }
    }
}
