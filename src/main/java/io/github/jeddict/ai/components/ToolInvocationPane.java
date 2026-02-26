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
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_NAME;
import static io.github.jeddict.ai.components.ArgumentChip.CHIP_SAMPLE_ARGUMENT_VALUE;
import io.github.jeddict.ai.util.UIUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Map;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.json.JSONObject;
import static ste.lloop.Loop.on;

/**
 * A Swing component that displays a tool invocation, including the tool name and its arguments.
 * This panel is used to visually represent tool executions in the user interface.
 */
public class ToolInvocationPane extends JPanel {
    public static final String ARGUMENT_SAMPLE_TOOL_NAME = "toolName";
    
    public final JPanel argumentsPanel;
    public final JLabel nameLabel;
    
    public ToolInvocationPane() {
        this(ARGUMENT_SAMPLE_TOOL_NAME, Map.of(CHIP_SAMPLE_ARGUMENT_NAME, CHIP_SAMPLE_ARGUMENT_VALUE));
    }
    
    public ToolInvocationPane(
        final String name,
        final Map<String, Object> arguments
    ) {
        super(new BorderLayout());
        
        setName(name);
        setBackground(UIUtil.COLOR_JEDDICT_MAIN_BACKGROUND);
        setBorder(UIUtil.BORDER_JEDDICT_SPACED);
        setOpaque(true);
        
        //
        // Tool name
        //
        nameLabel = new JLabel(name);
        nameLabel.setFont(UIUtil.FONT_HEADER);
        nameLabel.setForeground(UIUtil.COLOR_JEDDICT_ACCENT1);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameLabel.setName("name");
        
        //
        // Tool invocation arguments
        //
        argumentsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        argumentsPanel.setName("arguments");
        argumentsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        argumentsPanel.setBackground(UIUtil.COLOR_JEDDICT_MAIN_BACKGROUND);
        on(arguments).loop((argument, value) -> {
            argumentsPanel.add(new ArgumentChip(argument, String.valueOf(value)));
        });
        
        //
        // Put everything together
        //
        add(nameLabel, java.awt.BorderLayout.NORTH);
        final JScrollPane scrollPane = new JScrollPane(argumentsPanel);
        scrollPane.setBorder(null);
        add(scrollPane, java.awt.BorderLayout.CENTER);
    }
    
    public ToolInvocationPane(final ToolExecutionRequest execution) {
        this(
            execution.name(), 
            (execution.arguments() != null) 
                ? new JSONObject(execution.arguments()).toMap()
                : Map.of()
        );
    }
    
    public void toolInvocation(final ToolExecutionRequest execution) {
        nameLabel.setText(execution.name());
        argumentsPanel.removeAll();
        final JSONObject arguments = new JSONObject(execution.arguments());
        on(arguments.keySet()).loop((key) -> {
            argumentsPanel.add(new ArgumentChip(key, arguments.getString(key)));
        });
    }
}
