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
package io.github.jeddict.ai.lang;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ToolChoice;
import io.github.jeddict.ai.models.DummyChatModel;
import io.github.jeddict.ai.settings.PreferencesManager;
import java.util.logging.Logger;

/**
 *
 * This is a replacement of the class JeddictChatModelBuilder that is used when
 * running the tests (maven loads first classes in testing classpath, thus this
 * code is loaded instead of the production code).
 * It creates a Dummy model that inspects the request body for an instruction
 * to "use mock {file}", where {file} is the path to a mock file in
 * src/test/resources/mocks. The model will respond with the content of the
 * specified mock file.
 * <p>
 * The file name can be unquoted or enclosed in double quotes.
 * For example:
 * <ul>
 *     <li>use mock my_file.txt</li>
 *     <li>use mock "my file with spaces.txt"</li>
 * </ul>
 * <p>
 *
 */
public class JeddictChatModelBuilder {

    public final Logger LOG = Logger.getLogger(JeddictChatModelBuilder.class.getCanonicalName());

    protected static PreferencesManager pm = PreferencesManager.getInstance();
    final private String modelName;
    final private boolean withTools;

    private final ChatModelListener listener;

    public JeddictChatModelBuilder() {
        this(null);
    }

    public JeddictChatModelBuilder(final String modelName) {
        this(modelName, null);
    }

    public JeddictChatModelBuilder(
        final String modelName, final ChatModelListener listener
    ) {
        this.modelName = modelName; // P2 - TODO: can this be null?
        this.listener = listener;
        withTools = (modelName != null) && modelName.endsWith("-with-tools");
    }

    public ChatModel build() {
        LOG.finest(() -> "Building testing dummy model instead of " + modelName);

        if (modelName == null) {
            throw new IllegalArgumentException("modelName can not be null");
        }

        final DummyChatModel model = new DummyChatModel();

        model.toolChoice = (withTools) ? ToolChoice.AUTO : ToolChoice.NONE;

        return model;
    }

    public StreamingChatModel buildStreaming() {
        LOG.finest(() -> "Building testing dummy streaming model instead of " + modelName);

        final DummyChatModel model = new DummyChatModel();

        model.toolChoice = (withTools) ? ToolChoice.AUTO : ToolChoice.NONE;

        return model;
    }
}
