/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DummyToolTest {

    @Test
    public void initialization_with_basedir() throws IOException {
        then(new DummyTool().basedir()).isEqualTo(new File(".").getAbsolutePath());
    }

    @Test
    public void track_executions() throws IOException {
        final DummyTool t = new DummyTool();

        then(t.executed).isFalse(); then(t.executed()).isFalse();
        then(t.dummyTool()).isEqualTo("true");
        then(t.executed).isTrue(); then(t.executed()).isTrue();
    }

    @Test
    public void track_execution_with_args() throws IOException {
        final DummyTool t = new DummyTool();
        final List<String> arg2 = List.of("val2");
        final List<String> arg22 = List.of("val22");

        t.dummyToolWithArgs("val1", arg2);
        then(t.arguments).containsExactly("val1", arg2);

        t.dummyToolWithArgs("val2", arg22);
        then(t.arguments).containsExactly("val2", arg22);
    }

    @Test
    public void return_values() throws IOException {
        final DummyTool t = new DummyTool();
        final String arg11 = "val11", arg12 = "val12";
        final List<String> arg21 = List.of("val21");
        final List<String> arg22 = List.of("val22");

        then(t.dummyTool()).isEqualTo("true");
        then(t.dummyToolWithArgs(arg11, arg21)).isEqualTo("true\narg1: val11\narg2: [val21]");
        then(t.dummyToolWithArgs(arg12, arg22)).isEqualTo("true\narg1: val12\narg2: [val22]");
    }

    @Test
    public void reset_cleans_the_status() throws IOException {
        final DummyTool t = new DummyTool();
        t.dummyToolWithArgs("val1", List.of());

        then(t.executed).isTrue();
        then(t.arguments()).isNotNull();

        t.reset();

        then(t.executed).isFalse();
        then(t.arguments).isNull();
    }
}
