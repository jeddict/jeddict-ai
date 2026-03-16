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

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import static ste.lloop.Loop.on;

/**
 * Object to represent a tool execution. It basically consists of
 * a name and a list of arguments. Each argument can be a string or an array
 * of strings (for now, more types may be supported in the future).
 * If an error arises while converting the arguments to JSON, it falls back
 * to an execution of the tool "echo" with a proper error message.
 */
public class ExecutionJSONObject {

    public String name;
    public JSONObject arguments;

    public ExecutionJSONObject(final String name) {
        this(name, "");
    }

    public ExecutionJSONObject(final String name, final String content) {
        try {
            this.name = name;
            this.arguments = new JSONObject("{" + content + "}");
        } catch (final Throwable t) {
            this.name = "echo";
            this.arguments = new JSONObject();
            arguments.put(
                "message",
                "ERR error parsing tool execution for %s, make sure it is in well formed JSON:\n%s"
                    .formatted(name, content)
           );
        }
    }

    public String name() {
        return name;
    }

    public Object[] arguments() {
        final List values = new ArrayList();
        on(arguments.keys()).loop((key) -> {
            values.add(arguments.get(key));
        });
        return values.toArray();
    }

    public Object[] arguments(String... args) {
        final Object[] values = new Object[args.length];
        on(args).loop((i, arg) -> {
                values[i] = arguments.opt(arg);
                if (values[i] instanceof JSONArray jarray) {
                    values[i] = jarray.toList();
                } else if (values[i] == JSONObject.NULL) {
                    values[i] = null;
                }
        });
        return values;
    }
}
