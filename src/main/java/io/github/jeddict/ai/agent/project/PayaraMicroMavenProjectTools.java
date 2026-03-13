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
 * <p>Inherits the standard Maven {@code buildProject} ({@code mvn clean install})
 * and {@code runJavaClass} goals from {@link MavenProjectTools}, and exposes
 * additional tools for the Payara Micro lifecycle:</p>
 * <ul>
 *   <li><b>bundleMicro</b> – create an Uber JAR ({@code payara-micro:bundle})</li>
 *   <li><b>startMicro</b> – start Payara Micro ({@code payara-micro:start})</li>
 *   <li><b>stopMicro</b>  – stop the running instance ({@code payara-micro:stop})</li>
 *   <li><b>reloadApplication</b> – redeploy without restarting ({@code payara-micro:reload})</li>
 *   <li><b>devMode</b>   – development mode with live reload ({@code payara-micro:dev});
 * </ul>
 */
public class PayaraMicroMavenProjectTools extends MavenProjectTools {

    public PayaraMicroMavenProjectTools(final Project project) throws IOException {
        super(project);
    }

    // -----------------------------------------------------------------------
    // Payara Micro-specific tools
    // -----------------------------------------------------------------------

    @Tool(
        name = "bundleMicro",
        value = "Create a Payara Micro Uber JAR (application + micro) "
            + "using 'mvn payara-micro:bundle' (or the Maven wrapper) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public String bundleMicro() {
        return runCommand(resolveWrapper() + " payara-micro:bundle", "Bundling Payara Micro Uber JAR");
    }

    @Tool(
        name = "startMicro",
        value = "Start the Payara Micro using 'mvn payara-micro:start' "
            + "(or the Maven wrapper) and return the full output. "
            + "Prefer devMode over startMicro for development workflows."
    )
    @ToolPolicy(READWRITE)
    public String startMicro() {
        return runCommand(resolveWrapper() + " payara-micro:start", "Starting Payara Micro");
    }

    @Tool(
        name = "stopMicro",
        value = "Stop the running Payara Micro instance using 'mvn payara-micro:stop' "
            + "(or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String stopMicro() {
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

    /**
     * Starts Payara Micro in development mode (auto deploy + live reload).
     *
     * <p><b>Always prefer this over {@link #startMicro()}</b> during active
     * development: {@code dev} automatically enables hot-deploy, browser
     * live-reload, and session persistence without a micro restart.</p>
     */
    @Tool(
        name = "devMode",
        value = "Start Payara Micro in development mode with auto deploy and live reload "
            + "using 'mvn payara-micro:dev' (or the Maven wrapper). "
            + "Always prefer devMode over startMicro for development workflows."
    )
    @ToolPolicy(READWRITE)
    public String devMode() {
        return runCommand(resolveWrapper() + " payara-micro:dev", "Dev mode Payara Micro");
    }
}
