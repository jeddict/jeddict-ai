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

/**
 *
 * @author Gaurav Gupta
 */
public class FileActionParser {
    public static FileAction parse(String actionContent, String sourceContent) {
        final String[] lines = actionContent.split("\\n");

        String action = "", path = "";
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("path=")) {
                path = line.substring("path=".length());
            } else if (line.startsWith("action=")) {
                action = line.substring("action=".length());
            }
        }

        //
        // TODO: shall we check if action or path are empty and throw an exception?
        //
        return new FileAction(action, path, sourceContent);
    }
}
