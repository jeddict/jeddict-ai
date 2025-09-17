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

package io.github.jeddict.ai.agent.tools.build;

import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ProjectToolTest {

    @Test
    public void BuildProjectTool_is_a_BaseBuildTool() {
        then(BuildProjectTool.class).isAssignableTo(BaseBuildTool.class);
    }

    @Test
    public void TestProjectTool_is_a_BaseBuildTool() {
        then(TestProjectTool.class).isAssignableTo(BaseBuildTool.class);
    }

    //
    // TODO: more test cases
    //

}
