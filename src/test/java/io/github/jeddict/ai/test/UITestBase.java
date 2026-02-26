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

import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import org.assertj.swing.core.BasicComponentFinder;
import org.assertj.swing.core.BasicComponentPrinter;
import org.assertj.swing.core.ComponentFinder;
import org.assertj.swing.core.ComponentPrinter;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 *
 */
public class UITestBase extends TestBase {
    protected FrameFixture window;
    protected JFrame frame;
    protected Container content;
    
    protected ComponentPrinter PRINTER; 
    protected ComponentFinder FINDER;
    
    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }
    
    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();
        
        PRINTER = BasicComponentPrinter.printerWithCurrentAwtHierarchy();
        FINDER = BasicComponentFinder.finderWithCurrentAwtHierarchy();
        
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle("Test ArgumentPane");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content = frame.getContentPane();
        content.setPreferredSize(new Dimension(300, 300));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        window = new FrameFixture(frame);
        window.show();
    }
    
    @AfterEach
    @Override
    public void afterEach() {
        super.afterEach();
        window.cleanUp();
    }
}
