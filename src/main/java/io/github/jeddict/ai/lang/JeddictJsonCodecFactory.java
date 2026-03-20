/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.jeddict.ai.lang;


import dev.langchain4j.internal.Json;


import dev.langchain4j.spi.json.JsonCodecFactory;
import java.util.logging.Logger;

public class JeddictJsonCodecFactory implements JsonCodecFactory {

    private final Logger LOG = Logger.getLogger(getClass().getName());

    @Override
    public Json.JsonCodec create() {
        final JeddictJsonCodec codec = new JeddictJsonCodec();

        LOG.finest(() -> "using json codec " + codec);

        return codec;
    }


}