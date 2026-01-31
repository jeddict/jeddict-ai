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

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.UITestBase;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.assertj.core.api.BDDAssertions;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
@CacioTest
public class UIUtilTest extends UITestBase {
    
    private static final JPanel rootPanel = new JPanel(); 
    private static final JPanel innerPanel = new JPanel();
    private static final JLabel innerLabel = new JLabel("inner");
    private static final JPanel innerInnerPanel = new JPanel();
    private static final JLabel innerInnerLabel = new JLabel("label");
    private static final JButton innerInnerButton = new JButton("button");
    
    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        
        rootPanel.setName("root");
        innerPanel.add("inner-inner-panel", innerInnerPanel);
        innerPanel.add("inner-inner-label", innerInnerLabel);
        innerPanel.add("inner-inner-button", innerInnerButton);
        rootPanel.add("inner", innerPanel);
        rootPanel.add("inner-label", innerLabel);
        
        content.add("root", rootPanel);
    }
    
    @Test
    public void find_finds_requested_components() {
        List<JPanel> panels = UIUtil.find(rootPanel, JPanel.class);
        then(panels).containsExactly(innerPanel, innerInnerPanel);
        
        panels = UIUtil.find(innerPanel, JPanel.class);
        then(panels).containsExactly(innerInnerPanel);
        
        List<JLabel> labels = UIUtil.find(rootPanel, JLabel.class);
        then(labels).containsExactly(innerInnerLabel, innerLabel);
        
        labels = UIUtil.find(innerPanel, JLabel.class);
        then(labels).containsExactly(innerInnerLabel);
    }
    
    @Test
    public void find_sanity_check() {
        
        BDDAssertions.thenThrownBy(() -> {
            UIUtil.find(innerPanel, null);
        }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("ofType can not be null");
        
        BDDAssertions.thenThrownBy(() -> {
            UIUtil.find(null, JPanel.class);
        }).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("parent can not be null");
        
    }
}
