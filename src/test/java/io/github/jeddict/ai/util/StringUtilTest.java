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
package io.github.jeddict.ai.util;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

public class StringUtilTest {

    @Test
    public void camelCase_to_human_readable_single_word() {
        then(StringUtil.camelCaseToHumanReadable("check")).isEqualTo("Check");
    }

    @Test
    public void camelCase_to_human_readable_multi_word() {
        then(StringUtil.camelCaseToHumanReadable("checkMavenLocalRepoLocation"))
            .isEqualTo("Check maven local repo location");
    }

    @Test
    public void camelCase_to_human_readable_read_logs() {
        then(StringUtil.camelCaseToHumanReadable("readLogsFromBashSession"))
            .isEqualTo("Read logs from bash session");
    }

    @Test
    public void camelCase_to_human_readable_acronym_then_camel() {
        then(StringUtil.camelCaseToHumanReadable("thisIsATool1"))
            .isEqualTo("This is a tool 1");
    }

    @Test
    public void camelCase_to_human_readable_null_returns_null() {
        then(StringUtil.camelCaseToHumanReadable(null)).isNull();
    }

    @Test
    public void camelCase_to_human_readable_empty_returns_empty() {
        then(StringUtil.camelCaseToHumanReadable("")).isEqualTo("");
    }
}
