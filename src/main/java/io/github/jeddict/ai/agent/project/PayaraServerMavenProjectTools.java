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
 * <p>Overrides the generic Maven run command with Payara Server-specific
 * goals, and exposes additional tools for the full lifecycle:</p>
 * <ul>
 *   <li><b>start</b> – start Payara Server and deploy the application
 *       ({@code payara-server:start})</li>
 *   <li><b>dev</b>   – development mode with auto deploy and live reload
 *       ({@code payara-server:dev})</li>
 * </ul>
 *
 * <p>The standard {@code buildProject()} ({@code mvn clean install}) and
 * {@code testProject()} ({@code mvn test}) goals are inherited from
 * {@link MavenProjectTools} unchanged.</p>
 */
public class PayaraServerMavenProjectTools extends MavenProjectTools {

    public PayaraServerMavenProjectTools(final Project project) throws IOException {
        super(project);
    }

    // -----------------------------------------------------------------------
    // Overrides – run
    // -----------------------------------------------------------------------

    @Override
    @Tool(
        name = "runJavaClass",
        value = "Start Payara Server and deploy the application using 'mvn payara-server:start' "
            + "(or the Maven wrapper) and return the full output"
    )
    @ToolPolicy(READWRITE)
    public String runJavaClass(final String mainClass) {
        return runCommand(resolveRunCommand(mainClass), "Starting Payara Server");
    }

    /**
     * Returns the Payara Server start command ({@code mvn[w] payara-server:start}).
     * The {@code mainClass} parameter is not used because Payara Server manages
     * deployment through its own plugin configuration.
     */
    @Override
    String resolveRunCommand(final String mainClass) {
        return resolveWrapper() + " payara-server:start";
    }

    // -----------------------------------------------------------------------
    // Payara Server-specific tools
    // -----------------------------------------------------------------------

    @Tool(
        name = "devMode",
        value = "Start Payara Server in development mode with auto deploy and live reload "
            + "using 'mvn payara-server:dev' (or the Maven wrapper)"
    )
    @ToolPolicy(READWRITE)
    public String devMode() {
        return runCommand(resolveWrapper() + " payara-server:dev", "Dev mode Payara Server");
    }
}
