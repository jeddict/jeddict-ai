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
package io.github.jeddict.ai.agent.pair;

import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreePath;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import io.github.jeddict.ai.lang.JeddictBrain;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.apache.commons.lang3.StringUtils;

/**
 * AI assistant for chatting and non agentic tasks. It s meant to be the main
 * assistant for InterationMode.QUERY.
 */
public interface Assistant extends PairProgrammer {

    public static final String SYSTEM_MESSAGE = """
You are an expert developer that can address complex questions and resolving
problems, proposing solutions, writing and correcting code.
Take into account the following rules, if any:
global rules:
{{globalRules}}

project rules:
{{projectRules}}
    """
    ;

    public static final String USER_MESSAGE
        = "{{prompt}}\ncode: {{code}}\nproject info: {{project}}";

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    String chat(
        @V("prompt")       final String prompt,
        @V("code")         final String code,
        @V("project")      final String projectInfo,
        @V("globalRules")  final String globalRules,
        @V("projectRules") final String projectRules
    );

    @SystemMessage(SYSTEM_MESSAGE)
    @UserMessage(USER_MESSAGE)
    TokenStream chatstream(
        @V("prompt")       final String prompt,
        @V("code")         final String code,
        @V("project")      final String projectInfo,
        @V("globalRules")  final String globalRules,
        @V("projectRules") final String projectRules
    );

    default String chat(
        final String prompt,
        final TreePath tree,
        final String projectInfo,
        final String globalRules,
        final String projectRules
    ) {
        final String code = code(tree);

        LOG.finest(() -> "\n"
            + "prompt: " + StringUtils.abbreviate(prompt, 80) + "\n"
            + "code: " + StringUtils.abbreviate(code, 80) + "\n"
            + "projectInfo: " + StringUtils.abbreviate(projectInfo, 80) + "\n"
            + "globalRules: " + StringUtils.abbreviate(globalRules, 80) + "\n"
            + "globalRules: " + StringUtils.abbreviate(projectRules, 80) + "\n"
        );

        return chat(
            StringUtils.defaultIfBlank(prompt, ""),
            code,
            StringUtils.defaultIfBlank(projectInfo, ""),
            StringUtils.defaultIfBlank(globalRules, ""),
            StringUtils.defaultIfBlank(projectRules, "")
        );
    }

    default String chat(final String prompt) {
        return chat(prompt, (TreePath)null, null, null, null);
    }

    default void chat(
        final PropertyChangeListener listener,
        final String prompt,
        final TreePath tree,
        final String projectInfo,
        final String globalRules,
        final String projectRules
    ) {
        final String code = code(tree);

        LOG.finest(() -> "\n"
            + "prompt: " + StringUtils.abbreviate(prompt, 80) + "\n"
            + "code: " + StringUtils.abbreviate(code, 80) + "\n"
            + "projectInfo: " + StringUtils.abbreviate(projectInfo, 80) + "\n"
            + "globalRules: " + StringUtils.abbreviate(globalRules, 80) + "\n"
            + "globalRules: " + StringUtils.abbreviate(projectRules, 80) + "\n"
        );

        chatstream(
            StringUtils.defaultIfBlank(prompt, ""),
            code,
            StringUtils.defaultIfBlank(projectInfo, ""),
            StringUtils.defaultIfBlank(globalRules, ""),
            StringUtils.defaultIfBlank(projectRules, "")
        ).onCompleteResponse(complete -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_COMPLETED.name, null, complete));
        })
        .onPartialResponse(partial -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_PARTIAL.name, null, partial));
        })
        .onIntermediateResponse(intermediate -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_INTERMEDIATE.name, null, intermediate)))
        .beforeToolExecution(execution -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.TOOL_BEFORE_EXECUTION.name, null, execution)))
        .onToolExecuted(execution -> listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.TOOL_EXECUTED.name, null, execution)))
        .onError(error -> {
            listener.propertyChange(new PropertyChangeEvent(this, JeddictBrain.EventProperty.CHAT_ERROR.name, null, error));
        }).start();
    }

    default void chat(final PropertyChangeListener listener, final String prompt) {
        chat(listener, prompt, (TreePath)null, null, null, null);
    }

    default String code(final TreePath code) {
        final StringBuffer sb = new StringBuffer();

        if (code != null) {
            sb.append(code.getCompilationUnit().toString());
            if (code.getLeaf() instanceof MethodTree) {
                sb.append("\n\nmethod:\n").append(code.getLeaf().toString());
            }
        }

        return sb.toString();
    }
}
