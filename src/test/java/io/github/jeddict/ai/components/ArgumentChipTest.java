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
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import static org.assertj.core.api.BDDAssertions.then;
import org.assertj.swing.edt.FailOnThreadViolationRepaintManager;
import org.assertj.swing.edt.GuiActionRunner;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static ste.lloop.Loop.on;



@CacioTest
public class ArgumentChipTest {

    private static final String VALUE100 = StringUtils.repeat("abcdefghij", 10);

    private static final Pair<String,String>[] ARGUMENTS1 = new Pair[] {
        Pair.of("a1", "v1"), Pair.of("a2", "v2"), Pair.of("a3", "v3")
    };

    private FrameFixture window;
    private JFrame frame;
    private Container content;

    @BeforeClass
    public static void beforeClass() {
        FailOnThreadViolationRepaintManager.install();
    }

    @BeforeEach
    void beforeEach() {
        frame = GuiActionRunner.execute(() -> new JFrame());
        frame.setTitle(("Test ToolExecutionPane"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        content = frame.getContentPane();
        content.setPreferredSize(new Dimension(300, 300));
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        window = new FrameFixture(frame);
        window.show();
    }

    @AfterEach
    public void tearDown() throws Exception {
        window.cleanUp();
    }

    @Test
    public void chip_shows_argument_and_value() {
        on(ARGUMENTS1).loop((argument) -> {
            final String name = argument.getKey();
            final ArgumentChip chip = new ArgumentChip(name, argument.getValue());
            content.add(chip);
            window.panel(name).requireVisible();
            window.panel(name).label(name).requireText(".*%s:.*%s.*".formatted(name, argument.getValue()));
        });
    }

    @Test
    public void abbreviate_long_arguments() {
        final ArgumentChip chip = new ArgumentChip("arg", VALUE100);
        content.add(chip);

        then(window.panel("arg").label("arg").text())
            .contains(StringUtils.abbreviateMiddle(VALUE100, " ... ", 80))
            .doesNotContain(VALUE100);
    }
}
