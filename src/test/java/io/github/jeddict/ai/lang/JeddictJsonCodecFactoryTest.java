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

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.BDDAssertions.then;
import java.util.ServiceLoader;
import dev.langchain4j.spi.json.JsonCodecFactory;
import io.github.jeddict.ai.test.TestBase;
import java.util.logging.Level;

/**
 *
 */
public class JeddictJsonCodecFactoryTest extends TestBase {

    @Test
    public void json_codec_factory_service_loading() {
        ServiceLoader<JsonCodecFactory> loader = ServiceLoader.load(JsonCodecFactory.class);
        then(loader.iterator().hasNext()).isTrue();

        JsonCodecFactory factory = loader.iterator().next();
        then(factory).isNotNull();
        then(factory).isInstanceOf(JeddictJsonCodecFactory.class);
        then(factory).isInstanceOf(JsonCodecFactory.class);
    }

    @Test
    public void lanchain4j_picks_jeddict_factory() {
        dev.langchain4j.internal.Json.toJson(1);
        then(logHandler.getMessages(Level.FINEST)).hasSize(1)
            .element(0).asString().startsWith("using json codec " + JeddictJsonCodec.class.getName());
    }
}