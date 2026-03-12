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
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import java.io.IOException;
import org.netbeans.api.project.Project;

/**
 * Project-tool specialisation for Maven projects that use the
 * <a href="https://github.com/payara/ecosystem-maven">Payara Micro Maven Plugin</a>
 * ({@code fish.payara.maven.plugins:payara-micro-maven-plugin}).
 *
 * <p>Overrides the generic Maven build/run commands with Payara Micro-specific
 * goals, and exposes additional tools for the full lifecycle:</p>
 * <ul>
 *   <li><b>bundle</b> – create an Uber JAR ({@code payara-micro:bundle})</li>
 *   <li><b>start</b> – start Payara Micro ({@code payara-micro:start})</li>
 *   <li><b>stop</b>  – stop the running instance ({@code payara-micro:stop})</li>
 *   <li><b>reload</b> – redeploy without restarting ({@code payara-micro:reload})</li>
 *   <li><b>dev</b>   – development mode with live reload ({@code payara-micro:dev})</li>
 * </ul>
 */
public class PayaraMicroMavenProjectTools extends MavenProjectTools {

    public PayaraMicroMavenProjectTools(final Project project) throws IOException {
        super(project);
    }

    // -----------------------------------------------------------------------
    // Overrides – build / run / test
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "buildProject",
        value = "Build the Payara Micro project by creating an Uber JAR "
            + "using 'mvn payara-micro:bundle' (or the Maven wrapper) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public String buildProject() {
        return runCommand(resolveBuildCommand(), "Bundling");
    }

    /**
     * Returns the command to bundle the Payara Micro Uber JAR
     * ({@code mvn[w] payara-micro:bundle}).
     */
    @Override
    String resolveBuildCommand() {
        return resolveWrapper() + " payara-micro:bundle";
    }

    @Override
    @Tool(
        name = "runJavaClass",
        value = "Start the Payara Micro server using 'mvn payara-micro:start' "
            + "(or the Maven wrapper) and return the full output"
    )
    @ToolPolicy(READWRITE)
    public String runJavaClass(final String mainClass) {
        return runCommand(resolveRunCommand(mainClass), "Starting Payara Micro");
    }

    /**
     * Returns the Payara Micro start command ({@code mvn[w] payara-micro:start}).
     * The {@code mainClass} parameter is not used because Payara Micro manages
     * deployment through its own plugin configuration.
     */
    @Override
    String resolveRunCommand(final String mainClass) {
        return resolveWrapper() + " payara-micro:start";
    }

    // -----------------------------------------------------------------------
    // Payara Micro-specific tools
    // -----------------------------------------------------------------------

    @Tool(
        name = "stopServer",
        value = "Stop the running Payara Micro instance using 'mvn payara-micro:stop' "
            + "(or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String stopServer() {
        return runCommand(resolveWrapper() + " payara-micro:stop", "Stopping Payara Micro");
    }

    @Tool(
        name = "reloadApplication",
        value = "Redeploy the application without restarting Payara Micro "
            + "using 'mvn payara-micro:reload' (or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String reloadApplication() {
        return runCommand(resolveWrapper() + " payara-micro:reload", "Reloading Payara Micro");
    }

    @Tool(
        name = "devMode",
        value = "Start Payara Micro in development mode with auto deploy and live reload "
            + "using 'mvn payara-micro:dev' (or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String devMode() {
        return runCommand(resolveWrapper() + " payara-micro:dev", "Dev mode Payara Micro");
    }
}
