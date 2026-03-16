/**
 * Copyright 2026 the original author or authors from the Jeddict project (https://jeddict.github.io/).
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

import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 * JSONObject extension to represent a tool execution. It basically consists of
 * a name and a list of arguments. Each argument can be a string or an array
 * of strings (for now, more types may be supported in the future).
 *
 */
public class ExecutionJSONObjectTest {
    @Test
    public void construction() {
        ExecutionJSONObject o = new ExecutionJSONObject("tool");

        then(o.name).isEqualTo("tool");
        then(o.arguments()).isEmpty();

        o = new ExecutionJSONObject("anotherTool", """
        "arg1": "string value",
        "arg2": null,
        "arg3": [
             "one", "two", "three"
        ]
        """);
        then(o.name).isEqualTo("anotherTool");
        then(o.arguments()).isNotNull().isNotEmpty();
    }

    @Test
    public void arguments_by_direct_access() {
        final ExecutionJSONObject o = new ExecutionJSONObject("anotherTool", """
        "arg1": "string value",
        "arg2": null,
        "arg3": [
             "one", "two", "three"
        ]
        }""");
        final Object[] a = o.arguments("arg1", "arg2", "arg3");

        then(o.name).isEqualTo("anotherTool");
        then(o.arguments()).hasSize(3);
        then(a).containsExactly("string value", null, List.of("one", "two", "three"));
    }

    @Test
    public void arguments_returns_null_for_missing_parameters() {
        ExecutionJSONObject o = new ExecutionJSONObject("anotherTool", """
        "argOne": "valueOne"
        """);
        then(o.arguments("argOne", "argTwo")).containsExactly("valueOne", null);
    }

    @Test
    public void turn_into_echo_execution_in_case_of_errors() {
        final String INVALID_JSON = """
        "argOne": "valueOne"
        "argTwo": {
        """;
        final ExecutionJSONObject o = new ExecutionJSONObject("aTool", INVALID_JSON);
        then(o.name).isEqualTo("echo");
        then(o.arguments()).containsExactly("""
        ERR error parsing tool execution for aTool, make sure it is in well formed JSON:
        "argOne": "valueOne"
        "argTwo": {
        """);
    }
}