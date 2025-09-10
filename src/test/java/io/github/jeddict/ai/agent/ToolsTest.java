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

import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.lang.JeddictStreamHandler;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;

/**
 *
 * @author Gaurav Gupta
 */
public class ToolsTest {

    protected Project project;

    protected DummyHandler handler;

    static class DummyProject implements Project {

        @Override
        public FileObject getProjectDirectory() {
            return null;
        }

        @Override
        public Lookup getLookup() {
            return null;
        }
    }

    static class DummyHandler extends JeddictStreamHandler {

        private final StringBuilder responses = new StringBuilder();

        public DummyHandler(AssistantChat topComponent) {
            super(topComponent);
        }

        @Override
        public void onPartialResponse(String response) {
            responses.append(response);
        }

        public String getResponses() {
            return responses.toString();
        }
    }

}
