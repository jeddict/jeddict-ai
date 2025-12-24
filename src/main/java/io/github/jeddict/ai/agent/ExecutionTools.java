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
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.WRITE;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * This class provides tools to execute build and test commands in a project,
 * streaming output and returning the full log of the command.
 *
 * It supports different commands based on the operating system (Windows or Unix/Mac).
 * The execution logs are streamed to a JeddictStreamHandler if available.
 *
 * Usage:
 * - Create an instance with the target project and a stream handler.
 * - Invoke buildProject() to run the build command.
 * - Invoke testProject() to run the test command.
 *
 * @author Gaurav Gupta
 */
public class ExecutionTools extends AbstractTool {

    private final LogPrinter log;
    private final String projectName, buildCommand, testCommand;


    public ExecutionTools(
        final String basedir, final String projectName,
        final String buildCommand, final String testCommand
    ) {
        super(basedir);

        this.projectName = projectName;
        this.buildCommand = buildCommand;
        this.testCommand = testCommand;

        log = new LogPrinter(projectName);
    }

    @Tool("Build the project and return full log")
    @ToolPolicy(WRITE)
    public String buildProject() {
        return runCommand(buildCommand, "Building");
    }

    @Tool("Run project tests and return full log")
    @ToolPolicy(WRITE)
    public String testProject() {
        return runCommand(testCommand, "Testing");
    }

    /**
     * Runs a command in the project directory, streams output, and returns the
     * full log.
     *
     * @param goals the Maven goals to run (e.g., "clean install" or "test")
     * @param actionLabel the label to show in the AI stream ("Building",
     * "Testing", etc.)
     * @return full log of the Maven command
     */
    private String runCommand(String goals, String actionLabel) {
        progress(actionLabel + " " + projectName);
        StringBuilder fullLog = new StringBuilder();

        log.show();

        try {
            ProcessBuilder pb;

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                // Run via cmd /c on Windows
                pb = new ProcessBuilder("cmd", "/c", goals)
                        .directory(new File(basedir))
                        .redirectErrorStream(true);
            } else {
                // Unix/Mac
                pb = new ProcessBuilder("sh", "-c", goals)
                        .directory(new File(basedir))
                        .redirectErrorStream(true);
            }

            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    fullLog.append(line).append("\n");
                    log.print(line);
                }
            }

            int exitCode = process.waitFor();
            String result = (exitCode == 0 ? actionLabel + " successful"
                    : actionLabel + " failed with exit code " + exitCode);
            progress(actionLabel + " finished " + result);
            fullLog.append(result).append("\n");

        } catch (Exception e) {
            String error = actionLabel + " failed: " + e.getMessage();
            progress(actionLabel + " error " + error);
            fullLog.append(error).append("\n");
        }

        return fullLog.toString();
    }

}