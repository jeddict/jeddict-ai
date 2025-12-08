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


import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import static io.github.jeddict.ai.agent.pair.PairProgrammer.LOG;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

//
// TODO: global and project rules -> provide a tool
//


public interface Hacker extends PairProgrammer {
    public static final String SYSTEM_MESSAGE = """
You are an expert developer that can address complex tasks by resolving problems,
proposing solutions, writing and correcting code. If you need information or
interactions with the system you can use the tools provided.
Take into account the following project rules, if any:
{{rules}}
    """
    ;

    @SystemMessage(SYSTEM_MESSAGE)
    String _hack_(
        @UserMessage String prompt,
        @UserMessage List<ImageContent> images,
        @V("rules") final String rules
    );

    @SystemMessage(SYSTEM_MESSAGE)
    TokenStream _hackstream_(
        @UserMessage String prompt,
        @UserMessage List<ImageContent> images,
        @V("rules") final String rules
    );

    default String hack(final String prompt) {
        return hack(prompt, "none");
    }

    default TokenStream hackstream(final String prompt) {
        return hackstream(prompt, "none");
    }

    default String hack(final String prompt, final String rules) {
        return hack(prompt, List.of(), rules);
    }

    default TokenStream hackstream(final String prompt, final String rules) {
        return hackstream(prompt, List.of(), rules);
    }

    default String hack(final String prompt, final List<String> images, final String rules) {
        LOG.finest(() -> "\nprompt: %s\nrules: %s\n# images: %d".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(rules, 80),
            (images != null) ? images.size() : 0
        ));

        List<ImageContent> content = new ArrayList();

        if ((images != null)) {
            images.forEach((image) -> {
                content.add(new ImageContent(image));
            });
        }

        return _hack_(prompt, content, rules);
    }

    default TokenStream hackstream(final String prompt, final List<String> images, final String rules) {
        LOG.finest(() -> "\nprompt: %s\nrules: %s\n# images: %d".formatted(
            StringUtils.abbreviate(prompt, 80),
            StringUtils.abbreviate(rules, 80),
            (images != null) ? images.size() : 0
        ));

        List<ImageContent> content = new ArrayList();

        if ((images != null)) {
            images.forEach((image) -> {
                content.add(new ImageContent(image));
            });
        }

        return _hackstream_(prompt, content, rules);
    }


}
