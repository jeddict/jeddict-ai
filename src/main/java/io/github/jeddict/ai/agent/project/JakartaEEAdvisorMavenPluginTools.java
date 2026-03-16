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
package io.github.jeddict.ai.agent.project;

import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import java.io.File;
import java.io.IOException;

/**
 * Agent tools for the
 * <a href="https://mvnrepository.com/artifact/fish.payara.advisor/advisor-maven-plugin/2.0">
 * Payara Advisor Maven Plugin</a> ({@code fish.payara.advisor:advisor-maven-plugin:2.0}).
 *
 * <p>This tool class is added to the agent context when the project's
 * {@link io.github.jeddict.ai.scanner.ProjectMetadataInfo.BuildMetadataResolver#getProjectMetadata()}
 * indicates a Jakarta EE framework (i.e. the {@code "EE Version"} metadata key
 * starts with {@code "jakarta"}).</p>
 *
 * <p>The plugin is always invoked using its full Maven coordinates
 * ({@code fish.payara.advisor:advisor-maven-plugin:2.0:&lt;goal&gt;}) so the
 * project does not need to declare it in {@code pom.xml}.</p>
 *
 * <p>Two goals are exposed:</p>
 * <ul>
 *   <li><b>jakartaEEAdvise</b>    – analyzes Jakarta EE incompatibilities
 *       ({@code :advise} goal; default target version: {@code 10})</li>
 *   <li><b>microprofileAdvise</b> – analyzes MicroProfile incompatibilities
 *       ({@code :microprofile-advise} goal; default target version: {@code 6})</li>
 * </ul>
 */
public class JakartaEEAdvisorMavenPluginTools extends AbstractTool {

    /** Full Maven coordinates for the advisor plugin. */
    static final String PLUGIN = "fish.payara.advisor:advisor-maven-plugin:2.0";

    public JakartaEEAdvisorMavenPluginTools(final String basedir) throws IOException {
        super(basedir);
    }

    // -----------------------------------------------------------------------
    // Tools
    // -----------------------------------------------------------------------

    /**
     * Runs the Jakarta EE upgrade advisor for the given target version.
     *
     * <p>Analyzes the project and reports incompatibilities that must be
     * resolved before upgrading to the specified Jakarta EE version.
     * Accepted values: {@code 10} (Jakarta EE 10) or {@code 11} (Jakarta EE 11);
     * defaults to {@code 10} when not specified.</p>
     */
    @Tool(
        name = "jakartaEEAdvise",
        value = "Analyze the Jakarta EE project and report incompatibilities before upgrading "
            + "using 'mvn fish.payara.advisor:advisor-maven-plugin:2.0:advise'. "
            + "The adviseVersion parameter selects the target Jakarta EE version "
            + "(accepted values: 10, 11; default: 10). "
            + "The plugin is always invoked with its full coordinates so it does not need "
            + "to be declared in pom.xml."
    )
    @ToolPolicy(READONLY)
    public String jakartaEEAdvise(final String adviseVersion) {
        return runCommand(resolveAdviseCommand(adviseVersion),
            "Jakarta EE advisor (target version " + normalizeVersion(adviseVersion, "10") + ")");
    }

    /**
     * Runs the MicroProfile upgrade advisor for the given target version.
     *
     * <p>Analyzes the project and reports incompatibilities that must be
     * resolved before upgrading to the specified MicroProfile version.
     * Accepted values: {@code 6} (MicroProfile 6);
     * defaults to {@code 6} when not specified.</p>
     */
    @Tool(
        name = "microprofileAdvise",
        value = "Analyze the MicroProfile project and report incompatibilities before upgrading "
            + "using 'mvn fish.payara.advisor:advisor-maven-plugin:2.0:microprofile-advise'. "
            + "The adviseVersion parameter selects the target MicroProfile version "
            + "(accepted values: 6; default: 6). "
            + "The plugin is always invoked with its full coordinates so it does not need "
            + "to be declared in pom.xml."
    )
    @ToolPolicy(READONLY)
    public String microprofileAdvise(final String adviseVersion) {
        return runCommand(resolveMicroprofileAdviseCommand(adviseVersion),
            "MicroProfile advisor (target version " + normalizeVersion(adviseVersion, "6") + ")");
    }

    // -----------------------------------------------------------------------
    // Package-private helpers (used by tests)
    // -----------------------------------------------------------------------

    /**
     * Returns the Maven command for the {@code :advise} goal.
     * Defaults to version {@code "10"} when {@code adviseVersion} is null or blank.
     */
    String resolveAdviseCommand(final String adviseVersion) {
        return resolveWrapper() + " " + PLUGIN + ":advise -DadviseVersion="
            + normalizeVersion(adviseVersion, "10");
    }

    /**
     * Returns the Maven command for the {@code :microprofile-advise} goal.
     * Defaults to version {@code "6"} when {@code adviseVersion} is null or blank.
     */
    String resolveMicroprofileAdviseCommand(final String adviseVersion) {
        return resolveWrapper() + " " + PLUGIN + ":microprofile-advise -DadviseVersion="
            + normalizeVersion(adviseVersion, "6");
    }

    private static String normalizeVersion(final String version, final String defaultVersion) {
        return (version == null || version.isBlank()) ? defaultVersion : version;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the Maven executable to use: the Maven wrapper ({@code ./mvnw}
     * or {@code mvnw.cmd}) when present in the project directory, otherwise
     * the system {@code mvn}.
     */
    String resolveWrapper() {
        final boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        final File basedirFile = new File(basedir);
        if (isWindows) {
            if (new File(basedirFile, "mvnw.cmd").exists()) return "mvnw.cmd";
            if (new File(basedirFile, "mvnw").exists()) return "./mvnw";
            return "mvn";
        } else {
            return new File(basedirFile, "mvnw").exists() ? "./mvnw" : "mvn";
        }
    }
}
