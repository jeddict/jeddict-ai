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
package io.github.jeddict.ai.test;

import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.agent.AbstractTool;
import java.io.File;
import java.io.IOException;

/**
 *
 */
public class DummyTool extends AbstractTool {

    protected boolean executed = false;

    public DummyTool() throws IOException {
        this(new File(".").getAbsolutePath());
    }

    public DummyTool(String basedir) throws IOException {
        super(basedir);
    }

    public boolean executed() {
        return executed;
    }

    @Tool
    public String dummyTool() {
        return String.valueOf(executed = true);
    }
}
