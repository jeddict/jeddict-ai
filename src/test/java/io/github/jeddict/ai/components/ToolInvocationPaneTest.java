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

package io.github.jeddict.ai.components;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_NAME;
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_VALUE;
import static io.github.jeddict.ai.components.ToolInvocationPane.ARGUMENT_SAMPLE_TOOL_NAME;
import io.github.jeddict.ai.test.UITestBase;
import io.github.jeddict.ai.util.UIUtil;
import java.util.List;
import java.util.Map;
import javax.swing.JScrollBar;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



@CacioTest
public class ToolInvocationPaneTest extends UITestBase {

    private static final String VALUE100 = StringUtils.repeat("abcdefghij", 10);
    private static final Pair<String,String>[] ARGUMENTS1 = new Pair[] {
        Pair.of("a1", "v1"), Pair.of("a2", "v2"), Pair.of("a3", "v3")
    };

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        frame.setTitle(("Test ArgumentPane"));
    }
    
    @Test
    public void no_argument_contructor_create_a_sample_argument_pane() {
        //
        // This is particularly useful to add the component to the GUI Builder
        // palette
        //
        final ToolInvocationPane pane = new ToolInvocationPane();
        content.add(pane);
        window.panel(ARGUMENT_SAMPLE_TOOL_NAME).requireVisible();
        window.panel("arguments").panel(CHIP_SAMPLE_ARGUMENT_NAME).requireVisible();
        window.panel("arguments").label("argument")
            .requireText(".*%s:.* %s.*".formatted(CHIP_SAMPLE_ARGUMENT_NAME, CHIP_SAMPLE_ARGUMENT_VALUE));
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(1);
    }
    
    @Test
    public void constructors_with_executions() {
        ToolInvocationPane pane = 
            new ToolInvocationPane("callMe", Map.of("phone", "+1234567890"));
        
        content.add(pane);
        
        window.panel("callMe").requireVisible();
        window.panel("arguments").panel("phone").requireVisible();
        window.panel("arguments").label("phone")
            .requireText(".*%s:.* %s.*".formatted("phone", "\\+1234567890"));
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(1);
        
        pane = new ToolInvocationPane(
            ToolExecutionRequest.builder()
            .name("callMeAgain").arguments("{\"phone\":\"+1234567890\",\"mobile\":\"+1222333444\"}")
            .build()
        );
        
        content.removeAll(); content.add(pane);
        then(FINDER.findAll(pane, (component) -> {
            return component instanceof ArgumentChip;
        })).hasSize(2);
    }
    
    @Test
    public void show_scrollbars_when_not_enough_space() {
        ToolInvocationPane pane = new ToolInvocationPane(
            "callMe", Map.of("name", "James Bond", "phone", "+1234567890")
        );
        
        pane.setSize(500, 200); content.add(pane);
        pane.validate(); pane.repaint();
        
        final List<JScrollBar> scrollbars = UIUtil.find(pane, JScrollBar.class);
        then(scrollbars).hasSize(2);
        
        then(scrollbars.get(0).isVisible()).isFalse();
        then(scrollbars.get(1).isVisible()).isFalse();
        
        pane.setSize(100, 50); content.add(pane);
        pane.validate(); pane.repaint();

        then(scrollbars.get(0).isVisible()).isTrue();
        then(scrollbars.get(1).isVisible()).isTrue();
        
        
    }
}
