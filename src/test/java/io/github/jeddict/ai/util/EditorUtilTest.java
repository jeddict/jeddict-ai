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
package io.github.jeddict.ai.util;

import io.github.jeddict.ai.test.TestBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class EditorUtilTest extends TestBase {

    @Test
    public void wrapClassNamesWithAnchor_should_handle_special_characters_in_code_blocks() {
        String input = "Here is some code: <code>$variable</code> and <code>C:\\path</code>";
        String result = EditorUtil.wrapClassNamesWithAnchor(input);

        assertThat(result).contains("<code>$variable</code>");
        assertThat(result).contains("<code>C:\\path</code>");
    }
}