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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import static io.github.jeddict.ai.util.Icons.ICON_CHEVRON_DOWN;
import static io.github.jeddict.ai.util.Icons.ICON_CHEVRON_RIGHT;
import static io.github.jeddict.ai.util.Icons.ICON_TERMINAL;
import static io.github.jeddict.ai.util.StringUtil.camelCaseToHumanReadable;
import static io.github.jeddict.ai.util.UIUtil.BORDER_JEDDICT_SPACED;
import static io.github.jeddict.ai.util.UIUtil.BORDER_JEDDICT_SPACED_LINE_1;
import static io.github.jeddict.ai.util.UIUtil.COLOR_JEDDICT_MAIN_BACKGROUND;
import static io.github.jeddict.ai.util.UIUtil.FONT_MONOSPACED;
import static io.github.jeddict.ai.util.UIUtil.FONT_NORMAL_TEXT;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * A Swing component that displays the details and results of a tool execution
 * in a collapsible GitHub Copilot-style row.
 *
 * <p>The pane has two visual states:</p>
 * <ul>
 *   <li><b>Collapsed</b> (default) — a single header row that shows a terminal
 *       icon, the human-readable tool name, and an expand chevron.</li>
 *   <li><b>Expanded</b> — the header plus a details section containing the
 *       tool invocation arguments (via {@link ToolInvocationPane}) and the
 *       execution result in a scrollable text area.</li>
 * </ul>
 *
 * <p>Clicking anywhere on the header row toggles between collapsed and
 * expanded states.</p>
 *
 * @see ToolExecutionRequest
 * @see ToolInvocationPane
 * @see AssistantChat
 */
public class ToolExecutionPane extends JPanel {

    private final JPanel detailsPanel;
    private final JLabel chevronLabel;
    private boolean expanded = false;

    /**
     * Creates a new ToolExecutionPane with the given execution details and result.
     *
     * @param execution the tool execution request containing tool name and arguments
     * @param result    the result/output of the tool execution to display
     */
    public ToolExecutionPane(final ToolExecutionRequest execution, final String result) {
        setLayout(new BorderLayout());
        setName("root");
        setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        setBorder(BORDER_JEDDICT_SPACED_LINE_1);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));

        // ── Header row ────────────────────────────────────────────────────────
        final JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setName("header");
        headerPanel.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        headerPanel.setBorder(BORDER_JEDDICT_SPACED);
        headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Terminal icon  >_  rendered in a small bordered box
        final JLabel terminalIconLabel = new JLabel(ICON_TERMINAL);
        terminalIconLabel.setName("terminalIcon");
        terminalIconLabel.setFont(FONT_MONOSPACED);
        terminalIconLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));

        // Human-readable tool name label
        final JLabel nameLabel = new JLabel(camelCaseToHumanReadable(execution.name()));
        nameLabel.setName("toolName");
        nameLabel.setFont(FONT_NORMAL_TEXT);
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        // Left side: terminal icon + name
        final JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setName("headerLeft");
        leftPanel.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        leftPanel.add(terminalIconLabel);
        leftPanel.add(nameLabel);

        // Expand / collapse chevron on the right
        chevronLabel = new JLabel(ICON_CHEVRON_RIGHT);
        chevronLabel.setName("chevron");
        chevronLabel.setFont(FONT_NORMAL_TEXT);
        chevronLabel.setForeground(new Color(120, 120, 120));
        chevronLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));

        headerPanel.add(leftPanel, BorderLayout.CENTER);
        headerPanel.add(chevronLabel, BorderLayout.EAST);

        // ── Details section (initially collapsed) ─────────────────────────────
        detailsPanel = new JPanel(new BorderLayout());
        detailsPanel.setName("details");
        detailsPanel.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        detailsPanel.setVisible(false);

        // Tool invocation (arguments chips)
        final ToolInvocationPane invocationPane = new ToolInvocationPane(execution);
        invocationPane.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        detailsPanel.add(invocationPane, BorderLayout.NORTH);

        // Execution result
        final JPanel resultPane = new JPanel(new BorderLayout());
        resultPane.setName("result");
        resultPane.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        resultPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        resultPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(5, 5, 5, 5),
            BorderFactory.createLineBorder(new Color(234, 234, 234))
        ));

        final JTextArea resultTextArea = new JTextArea();
        resultTextArea.setName("result");
        resultTextArea.setEditable(false);
        resultTextArea.setBackground(COLOR_JEDDICT_MAIN_BACKGROUND);
        resultTextArea.setFont(FONT_NORMAL_TEXT);
        resultTextArea.setLineWrap(true);
        resultTextArea.setWrapStyleWord(true);
        resultTextArea.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        resultTextArea.setText(result);
        resultTextArea.setRows(Math.min(5, resultTextArea.getLineCount()));

        final JScrollPane resultScrollPane = new JScrollPane(resultTextArea);
        resultScrollPane.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        resultScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        resultPane.add(resultScrollPane, BorderLayout.CENTER);
        detailsPanel.add(resultPane, BorderLayout.CENTER);

        // ── Assemble ─────────────────────────────────────────────────────────
        add(headerPanel, BorderLayout.NORTH);
        add(detailsPanel, BorderLayout.CENTER);

        // Toggle on any click within the header area
        final MouseAdapter toggle = new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                toggleExpanded();
            }
        };
        headerPanel.addMouseListener(toggle);
        leftPanel.addMouseListener(toggle);
        terminalIconLabel.addMouseListener(toggle);
        nameLabel.addMouseListener(toggle);
        chevronLabel.addMouseListener(toggle);
    }

    /** Toggles the expanded / collapsed state of the details section. */
    private void toggleExpanded() {
        expanded = !expanded;
        detailsPanel.setVisible(expanded);
        chevronLabel.setText(expanded ? ICON_CHEVRON_DOWN : ICON_CHEVRON_RIGHT);
        revalidate();
        repaint();
    }

    /**
     * Toggles expanded / collapsed state programmatically.
     * This method is equivalent to clicking the header row.
     */
    public void toggle() {
        toggleExpanded();
    }

    /** Returns {@code true} when the details section is currently visible. */
    public boolean isExpanded() {
        return expanded;
    }
}
