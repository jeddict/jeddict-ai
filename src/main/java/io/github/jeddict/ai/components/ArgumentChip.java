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

import static io.github.jeddict.ai.util.ColorUtil.web;
import static io.github.jeddict.ai.util.UIUtil.FONT_MONOSPACED;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import org.apache.commons.lang3.StringUtils;
import static io.github.jeddict.ai.util.UIUtil.COLOR_JEDDICT_ACCENT2;
import static io.github.jeddict.ai.util.UIUtil.COLOR_JEDDICT_MAIN_BACKGROUND;

/**
 * A visual chip component that displays a parameter name and value.
 * This component is used to visually represent tool arguments in the user interface.
 */
public class ArgumentChip extends JPanel {
    
    public static final String CHIP_SAMPLE_ARGUMENT_NAME = "argument";
    public static final String CHIP_SAMPLE_ARGUMENT_VALUE = "value";
    
    public ArgumentChip() {
        this(CHIP_SAMPLE_ARGUMENT_NAME, CHIP_SAMPLE_ARGUMENT_VALUE);
    }

    public ArgumentChip(final String name, final String value) {
        setLayout(new BorderLayout());
        setName(name);
        setBorder(new EmptyBorder(2, 0, 0, 2));
        setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        setOpaque(true);

        // Create the accent ribbon
        final Box.Filler ribbon = new Box.Filler(
            new Dimension(3, 16),  // min
            new Dimension(3, 16),  // pref
            new Dimension(3, Integer.MAX_VALUE)   // max
        );
        ribbon.setBackground(COLOR_JEDDICT_ACCENT2);
        ribbon.setOpaque(true);

        // Create the label
        JLabel label = new JLabel(
            "<html><font color='%s'><b>%s:</b></font> %s</html>"
                .formatted(web(COLOR_JEDDICT_ACCENT2), name, StringUtils.abbreviateMiddle(value, " ... ", 80))
        );
        label.setName(name);
        label.setFont(FONT_MONOSPACED);
        label.setBorder(new CompoundBorder(
            new LineBorder(new Color(222, 226, 230), 1),
            new EmptyBorder(4, 10, 4, 8)
        ));

        // Assemble the chip
        add(ribbon, BorderLayout.WEST);
        add(label, BorderLayout.CENTER);
    }
}