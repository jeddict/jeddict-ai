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


import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import org.apache.commons.lang3.StringUtils;


public interface HackerWithoutTools extends Hacker {
    public static final String SYSTEM_MESSAGE = """
    You are an expert developer that can address complex tasks by resolving problems,
    proposing solutions, writing and correcting code.
    """
    ;
    @SystemMessage(SYSTEM_MESSAGE)
    String _hack_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream _hackstream_(
        @UserMessage String prompt,
        @V("globalRules") final String globalRules,
        @V("projectRules") final String projectRules
    );

    @Override
    default String hack(final String prompt, final String globalRules, final String projectRules) {
        LOG.finest(() -> "\nprompt: %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80)
        ));

        return _hack_(prompt, globalRules, projectRules);
    }

    @Override
    default void hack(
        final JeddictBrainListener listener,
        final String prompt,
        final String globalRules, final String projectRules
    ) {
        LOG.finest(() -> "\nprompt (streming): %s\nglobal rules: %s\nprojectRules: %s".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(globalRules, 80),
            StringUtils.abbreviate(projectRules, 80)
        ));

        _hackstream_(prompt, globalRules, projectRules)
        .onError(error -> {
            if (listener != null) {
                listener.onError(error);
            }
        })
        .onPartialResponse(progress -> {
            if (listener != null) {
                listener.onProgress(progress);
            }
        })
        .start();
    }
}
