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
 * <a href="https://github.com/payara/ecosystem-maven">Payara Server Maven Plugin</a>
 * ({@code fish.payara.maven.plugins:payara-server-maven-plugin}).
 *
 * <p>Inherits the standard Maven {@code buildProject} ({@code mvn clean install}),
 * {@code runJavaClass} ({@code mvn exec:java}), and {@code testProject}
 * ({@code mvn test}) goals from {@link MavenProjectTools}, and exposes
 * additional tools for the Payara Server lifecycle:</p>
 * <ul>
 *   <li><b>startServer</b> – start Payara Server and deploy the application
 *       ({@code payara-server:start})</li>
 *   <li><b>stopServer</b>  – stop the running instance
 *       ({@code payara-server:stop})</li>
 *   <li><b>devMode</b>     – development mode with auto deploy and live reload
 *       ({@code payara-server:dev});
 *       <em>always prefer this over {@code startServer} during development</em></li>
 * </ul>
 */
public class PayaraServerMavenProjectTools extends MavenProjectTools {

    public PayaraServerMavenProjectTools(final Project project) throws IOException {
        super(project);
    }

    // -----------------------------------------------------------------------
    // Payara Server-specific tools
    // -----------------------------------------------------------------------

    @Tool(
        name = "startServer",
        value = "Start Payara Server and deploy the application using 'mvn payara-server:start' "
            + "(or the Maven wrapper) and return the full output. "
            + "Always prefer devMode over startServer for development workflows."
    )
    @ToolPolicy(READWRITE)
    public String startServer() {
        return runCommand(resolveWrapper() + " payara-server:start", "Starting Payara Server");
    }

    @Tool(
        name = "stopServer",
        value = "Stop the running Payara Server instance using 'mvn payara-server:stop' "
            + "(or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String stopServer() {
        return runCommand(resolveWrapper() + " payara-server:stop", "Stopping Payara Server");
    }

    /**
     * Starts Payara Server in development mode (auto deploy + live reload).
     *
     * <p><b>Always prefer this over {@link #startServer()}</b> during active
     * development: {@code dev} automatically enables hot-deploy, browser
     * live-reload, and session persistence without a server restart.</p>
     */
    @Tool(
        name = "devMode",
        value = "Start Payara Server in development mode with auto deploy and live reload "
            + "using 'mvn payara-server:dev' (or the Maven wrapper). "
            + "Always prefer devMode over startServer for development workflows."
    )
    @ToolPolicy(READWRITE)
    public String devMode() {
        return runCommand(resolveWrapper() + " payara-server:dev", "Dev mode Payara Server");
    }
}
