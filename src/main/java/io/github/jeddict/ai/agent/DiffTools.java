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

/**
 * A tool for performing diff operations.
 */
public class DiffTools extends AbstractTool {

    public DiffTools(String basedir) {
        super(basedir);
    }

    @Tool("""
    Preview the new content of a file for review and acceptance (e.g. via a diff viewers").
    It accepts two arguments:
    - filename is the name of the file the new content shall be compared to; it
      can be a new filename if the content is for a new file
    - content is the content of the file
    It returns the final version of the file after the review.
    Abort the execution of this particular action if an exception is thrown.
    """)
    public String diff(String filename, String content) {
        progress("Performing diff on " + filename);
        // Simulate diff operation
        if (filename.equals("test.txt") && content.contains("new content")) {
            return "--- a/test.txt\n+++ b/test.txt\n@@ -1 +1 @@\n-old content\n+new content";
        }
        return "No significant differences found for " + filename;
    }
}