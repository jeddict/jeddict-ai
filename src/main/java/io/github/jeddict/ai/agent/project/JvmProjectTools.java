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
import java.io.IOException;
import org.netbeans.api.project.Project;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;

/**
 * Shared base class for JVM-based project tools (Maven and Gradle).
 *
 * <p>Extends the generic {@link ProjectTools} with a common {@link #jdkVersion()}
 * tool declaration so that LLM agents can always ask for the JDK version
 * regardless of whether the project is Maven- or Gradle-based.  Concrete
 * subclasses ({@link MavenProjectTools}, {@link GradleProjectTools}) provide
 * the build-system-specific implementation.</p>
 */
public abstract class JvmProjectTools extends ProjectTools {

    protected JvmProjectTools(final Project project) throws IOException {
        super(project);
    }

    /**
     * Returns the Java compiler / source-compatibility version configured in
     * the project's build file.
     *
     * <p>Each subclass reads the relevant property from its own build file
     * ({@code pom.xml} or {@code build.gradle[.kts]}).</p>
     *
     * @return the JDK version string, or a human-readable message when not found
     */
    @Tool(
        name = "jdkVersion",
        value = "Return the Java compiler source version configured in this JVM project's build file"
    )
    @ToolPolicy(READONLY)
    public abstract String jdkVersion() throws Exception;

    /**
     * Runs a Java class by its fully qualified name using the project's build
     * tool and returns the full output.
     *
     * @param mainClass the fully qualified name of the class to run
     * @return the combined stdout/stderr output of the command followed by a
     *         status line
     */
    @Tool(
        name = "runJavaClass",
        value = "Run a Java class by its fully qualified class name and return the full output. "
            + "Use this to execute any Java application or main class in the project."
    )
    @ToolPolicy(READWRITE)
    public abstract String runJavaClass(String mainClass) throws Exception;

    /**
     * Builds the project using the project's build tool and returns the full output.
     *
     * @return the combined stdout/stderr output of the build command followed
     *         by a status line
     */
    @Tool(
        name = "buildProject",
        value = "Build the project using its native build tool (Maven/Gradle) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public abstract String buildProject() throws Exception;

    /**
     * Runs the project's test suite using its build tool and returns the full output.
     *
     * @return the combined stdout/stderr output of the test command followed by
     *         a status line
     */
    @Tool(
        name = "testProject",
        value = "Run the project's test suite using its native build tool (Maven/Gradle) and return the full log"
    )
    @ToolPolicy(READWRITE)
    public abstract String testProject() throws Exception;
}
