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
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;

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

    private static final Logger LOG = Logger.getLogger(ProjectMetadataInfo.class.getName());

    private static final Map<Project, CachedResult> cache = new HashMap<>();

    /**
     * Optional hook that project-type-specific tools (e.g. MavenProjectTools,
     * GradleProjectTools) implement so that {@link ProjectMetadataInfo} can
     * obtain metadata without parsing build files directly.
     *
     * <p>This interface lives here (scanner package) so that the agent package
     * can depend on scanner — not the other way round.</p>
     */
    public interface BuildMetadataResolver {
        /** Returns the project display name, or {@code null} to keep the default. */
        String getProjectName();

        /**
         * Returns the build-system type label for this project (e.g. "maven",
         * "gradle", "ant", "nodejs", "python"). {@code null} means unknown.
         */
        String getProjectType();

        /**
         * Returns the name of the primary build file relative to the project
         * root (e.g. "pom.xml", "build.gradle", "build.xml"). Used for
         * cache-invalidation: when the file changes the cached metadata is
         * discarded. {@code null} means use the project directory mtime.
         */
        String getBuildFileName();

        /**
         * Returns an ordered map of project metadata key-value pairs to include
         * in the project info output (e.g. "Java Version" → "11").
         * Implementations should return an empty map when no metadata is
         * available; returning {@code null} is treated the same as an empty map.
         */
        Map<String, String> getProjectMetadata();
    }

    public static String get(Project project) {
        return get(project, null);
    }

    public static String get(Project project, BuildMetadataResolver resolver) {
        if (project == null) {
            return "";
        }

        final CachedResult cachedResult = getCachedResult(project, resolver);

        // Check if cachedResult is null and handle accordingly
        if (cachedResult == null) {
            return "Project Metadata: Unable to retrieve metadata for the specified project.";
        }

        final StringBuilder sb = new StringBuilder();

        sb.append("- name: ").append(StringUtils.defaultString(cachedResult.name)).append('\n')
          .append("- folder: ").append(StringUtils.defaultString(cachedResult.folder)).append('\n')
        ;

        if (cachedResult.type() != null) {
            sb.append("- type: ").append(cachedResult.type()).append('\n');
        }

        for (final Map.Entry<String, String> entry : cachedResult.metadata().entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }

        return sb.toString().trim();
    }

    public static CachedResult getCachedResult(Project project) {
        return getCachedResult(project, null);
    }

    /**
     * Returns (and caches) a {@link CachedResult} for the given project.
     *
     * <p>When a {@link BuildMetadataResolver} is supplied, the project type,
     * build file name, extra metadata key-value pairs, and project name are
     * all obtained from it. When {@code resolver} is {@code null} type and
     * metadata are absent — the generic path does not attempt to parse any
     * build file.</p>
     */
    public static CachedResult getCachedResult(Project project, BuildMetadataResolver resolver) {
        try {
            // Check if the project is cached
            CachedResult cachedResult = cache.get(project);

            final FileObject projectDirectory = project.getProjectDirectory();
            final String folder = FilenameUtils.separatorsToSystem(projectDirectory.getPath());

            // Use the build file modification time for cache invalidation; fall
            // back to the project directory mtime when no resolver (or no build
            // file name) is provided.
            final String buildFileName = (resolver != null) ? resolver.getBuildFileName() : null;
            final FileObject buildFile = (buildFileName != null)
                    ? projectDirectory.getFileObject(buildFileName)
                    : null;
            final long lastModified = buildFile != null
                    ? new File(buildFile.getPath()).lastModified()
                    : new File(folder).lastModified();

            if (cachedResult != null && cachedResult.timestamp >= lastModified) {
                return cachedResult;
            }

            // Default project name: directory name.  The resolver may override this.
            String name = projectDirectory.getName();

            // Project type and metadata come entirely from the resolver.
            // When no resolver is provided these remain null/empty.
            String type = null;
            Map<String, String> metadata = Collections.emptyMap();

            if (resolver != null) {
                if (resolver.getProjectName() != null && !resolver.getProjectName().isBlank()) {
                    name = resolver.getProjectName();
                }
                type = resolver.getProjectType();
                final Map<String, String> resolved = resolver.getProjectMetadata();
                if (resolved != null) {
                    metadata = resolved;
                }
            }

            // Cache the result
            final CachedResult result = new CachedResult(name, folder, type, metadata, lastModified);
            cache.put(project, result);
            return result;

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to read project metadata", e);
            return null;
        }
    }

    private record CachedResult(
        String name, String folder, String type,
        Map<String, String> metadata,
        long timestamp
    ) {}
}
