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

import java.io.File;
import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class DummyToolTest {

    @Test
    public void initialization_with_basedir() throws IOException{
        then(new DummyTool().basedir()).isEqualTo(new File(".").getAbsolutePath());
    }

    @Test
    public void track_executions() throws IOException {
        final DummyTool t = new DummyTool();

        then(t.executed).isFalse(); then(t.executed()).isFalse();
        then(t.dummyTool()).isEqualTo("true");
        then(t.executed).isTrue(); then(t.executed()).isTrue();
    }

}
