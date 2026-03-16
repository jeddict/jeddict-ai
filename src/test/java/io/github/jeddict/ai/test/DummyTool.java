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

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.ToolPolicy;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.INTERACTIVE;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READONLY;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.READWRITE;
import static io.github.jeddict.ai.agent.ToolPolicy.Policy.UNKNOWN;


import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 */
public class DummyTool extends AbstractTool {

    protected boolean executed = false;
    protected Object[] arguments = null;

    public DummyTool() throws IOException {
        this(new File(".").getAbsolutePath());
    }

    public DummyTool(String basedir) throws IOException {
        super(basedir);
    }

    public boolean executed() {
        return executed;
    }

    public Object[] arguments() {
        return arguments;
    }

    public void reset() {
        executed = false;
        arguments = null;
    }

    @Tool("simple tool that does nothing")
    public String dummyTool() {
        progress("executing dummyTool");
        return String.valueOf(executed = true);
    }

    @Tool("simple tool that does nothing but with arguments")
    public String dummyToolWithArgs(
        @P("the first argument")
        String arg1,
        List<String> arg2 // no annotation on purpose
    ) {
        arguments = new Object[] { arg1, arg2 };
        progress("executing dummyToolWithArgs with args (%s,%s)".formatted(arg1, String.valueOf(arg2)));
        return "%s\narg1: %s\narg2: %s"
            .formatted(String.valueOf(executed = true), arg1, String.valueOf(arg2));
    }

    @Tool("simple READONLY tool that does nothing")
    @ToolPolicy(READONLY)
    public void dummyToolRead() {
        progress("executing dummyToolRead");
        executed = true;
    }

    @Tool("simple INTERACTIVE tool that does nothing")
    @ToolPolicy(INTERACTIVE)
    public void dummyToolInteractive() {
        progress("executing dummyToolInteractive");
        executed = true;
    }

    @Tool("simple READWRITE tool that does nothing")
    @ToolPolicy(READWRITE)
    public void dummyToolWrite() {
        progress("executing dummyToolWriter");
        executed = true;
    }

    @Tool() // no description on purpose
    @ToolPolicy(UNKNOWN)
    public void dummyToolUnknown() {
        progress("executing dummyToolUnknown");
        executed = true;
    }

    @Tool() // no description on purpose
    @ToolPolicy(UNKNOWN)
    public void dummyToolError() {
        progress("executing dummyToolError");
        throw new RuntimeException("error in dummyTool");
    }
}