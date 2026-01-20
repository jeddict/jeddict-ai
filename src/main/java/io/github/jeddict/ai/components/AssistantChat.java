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
package io.github.jeddict.ai.components;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.lang.InteractionMode;
import static io.github.jeddict.ai.classpath.JeddictQueryCompletionQuery.JEDDICT_EDITOR_CALLBACK;
import static io.github.jeddict.ai.components.QueryPane.createIconButton;
import static io.github.jeddict.ai.components.QueryPane.createStyledComboBox;
import io.github.jeddict.ai.components.diff.DiffPane;
import io.github.jeddict.ai.components.mermaid.MermaidPane;
import static io.github.jeddict.ai.models.registry.GenAIProvider.getModelsByProvider;
import io.github.jeddict.ai.response.TextBlock;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.review.Review;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.ColorUtil;
import static io.github.jeddict.ai.util.DiffUtil.diffAction;
import static io.github.jeddict.ai.util.DiffUtil.diffActionWithSelected;
import io.github.jeddict.ai.util.EditorUtil;
import static io.github.jeddict.ai.util.EditorUtil.createEditorKit;
import static io.github.jeddict.ai.util.EditorUtil.createInMemoryEditorCopy;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getExtension;
import static io.github.jeddict.ai.util.EditorUtil.getFontFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getTextColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.isSuitableForWebAppDirectory;
import static io.github.jeddict.ai.util.Icons.ICON_ATTACH;
import static io.github.jeddict.ai.util.Icons.ICON_CANCEL;
import static io.github.jeddict.ai.util.Icons.ICON_CONTEXT;
import static io.github.jeddict.ai.util.Icons.ICON_COPY;
import static io.github.jeddict.ai.util.Icons.ICON_EDIT;
import static io.github.jeddict.ai.util.Icons.ICON_NEW_CHAT;
import static io.github.jeddict.ai.util.Icons.ICON_NEXT;
import static io.github.jeddict.ai.util.Icons.ICON_PREV;
import static io.github.jeddict.ai.util.Icons.ICON_SEND;
import static io.github.jeddict.ai.util.Icons.ICON_SETTINGS;
import static io.github.jeddict.ai.util.Icons.ICON_STATS;
import static io.github.jeddict.ai.util.Icons.ICON_WEB;
import io.github.jeddict.ai.util.Labels;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.StringUtil.convertToCapitalized;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import static ste.lloop.Loop.on;

/**
 *
 * @author Shiwani Gupta
 */
public abstract class AssistantChat extends TopComponent {

    private static final String SPINNER_FRAMES = "◐◓◑◒";

    private final Logger LOG = Logger.getLogger(AssistantChat.class.getCanonicalName());

    private List<Review> reviews;
    public static final ImageIcon icon = new ImageIcon(AssistantChat.class.getResource("/icons/logo16.png"));
    public static final ImageIcon logoIcon = new ImageIcon(AssistantChat.class.getResource("/icons/logo28.png"));

    private static final String QUESTION_KEY = "QUESTION";
    private final JPanel parentPanel;
    private Project project;

    private final Map<JEditorPane, JPopupMenu> menus = new HashMap<>();
    private final Map<JEditorPane, List<JMenuItem>> menuItems = new HashMap<>();
    private final Map<JEditorPane, List<JMenuItem>> submenuItems = new HashMap<>();
    private String type = "java";
    private static final PreferencesManager pm = PreferencesManager.getInstance();

    // top query pane
    private JButton copyButton, editButton, saveButton, cancelButton;
    private JEditorPane queryPane;

    // bottom question pane
    private JPanel filePanel;
    private MessageContextComponentAdapter filePanelAdapter;
    private ComponentAdapter buttonPanelAdapter;
    private JComboBox<String> models;
    private JComboBox<InteractionMode> actionComboBox;
    private JButton prevButton, nextButton, openInBrowserButton, submitButton;
    private JEditorPane questionPane;
    private JScrollPane questionScrollPane;

    protected ProgressHandle handle;

    // confirmation pane
    private ToolExecutionConfirmationPane confirmationPane;

    //
    // Kind of model for this window
    //
    private Response response;

    private final Timer timer = new Timer(200, e -> {
        int index = SPINNER_FRAMES.indexOf(submitButton.getText().charAt(0));
        index = (index < 0) ? 0 : ((index+1) % SPINNER_FRAMES.length());
        submitButton.setText(String.valueOf(SPINNER_FRAMES.charAt(index)));
    });

    public AssistantChat(String name, String type, Project project) {
        setName(name);
        setLayout(new BorderLayout());
        setIcon(icon.getImage());

        this.project = project;
        if (type != null) {
            this.type = type;
        }
        parentPanel = new JPanel();
        parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
        add(parentPanel, BorderLayout.CENTER);
    }

    public abstract void onChatReset();

    public abstract void onSubmit();

    public abstract void onPrev();

    public abstract void onNext();

    public abstract void onSessionContext();

    public abstract void addFileTab(FileObject file);

    public abstract void clearFileTab();


    public JEditorPane getQuestionPane() {
        return questionPane;
    }

    public JEditorPane getQueryPane() {
        return queryPane;
    }

    public void updateButtons(Boolean prevButtonVisible, Boolean nextButtonVisible) {
        prevButton.setVisible(prevButtonVisible);
        nextButton.setVisible(nextButtonVisible);
        openInBrowserButton.setVisible(getAllEditorCount() > 0);
    }

    public void startLoading() {
        timer.restart();
        LOG.finest(() -> "startLoading with handle %s".formatted(handle));
        SwingUtilities.invokeLater(() -> {
            LOG.finest(() -> ("updpating task loader with handle " + handle));
            if (handle != null) {
                handle.finish();
            } else {
                handle = ProgressHandle.createHandle(NbBundle.getMessage(JeddictUpdateManager.class, "PROGRESS_TASK_1"));
                //handle.setDisplayName(taskName);
                handle.start();
                LOG.finest(() -> ("new task loader with handle %s created and started ".formatted(handle)));
            }
        });
    }

    public void updateLoading(final String progress) {
        if (handle == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            LOG.finest(() -> "updpating task loader with handle %s and progress %s".formatted(handle, progress));
            handle.setDisplayName(progress);
        });
    }

    public void stopLoading() {
        LOG.finest(() -> ("stopping task loader with handle " + handle));
        timer.stop();
        SwingUtilities.invokeLater(() -> {
            if (handle != null) {
                handle.finish();
                handle = null;
            }
        });
        buttonPanelResized();
    }

    public boolean isLoading() {
        return (handle != null);
    }

   public String getModelName() {
        String modelName = (String) models.getSelectedItem();
        if (modelName == null || modelName.isEmpty()) {
            return pm.getModel();
        }
        return modelName;
    }

    public void buttonPanelResized() {
         buttonPanelAdapter.componentResized(null);
    }

    public boolean isAgentEnabled() {
        return actionComboBox.getSelectedItem() != InteractionMode.ASK;
    }

    public boolean isInteractiveMode() {
        return actionComboBox.getSelectedItem() == InteractionMode.INTERACTIVE;
    }

    public InteractionMode interactiveMode() {
        return (InteractionMode)actionComboBox.getSelectedItem();
    }

    public JPanel createBottomPanel(String type, String fileName, Consumer<String> action) {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        filePanel = new JPanel();
        filePanelAdapter = new MessageContextComponentAdapter(filePanel);
        filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBackground(backgroundColor);
        filePanel.setBorder(BorderFactory.createEmptyBorder());
        filePanel.addComponentListener(filePanelAdapter);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder());

        JPanel rightButtonPanel = new JPanel();
        rightButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 8, 1));
        rightButtonPanel.setBackground(backgroundColor);
        rightButtonPanel.setBorder(BorderFactory.createEmptyBorder());
        buttonPanel.add(rightButtonPanel, BorderLayout.EAST);

        JPanel leftButtonPanel = new JPanel();
        leftButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 1));
        leftButtonPanel.setBackground(backgroundColor);
        leftButtonPanel.setBorder(BorderFactory.createEmptyBorder());
        buttonPanel.add(leftButtonPanel, BorderLayout.WEST);

        prevButton = createIconButton(Labels.PREV, ICON_PREV);
        prevButton.setToolTipText("Previous Chat");
        leftButtonPanel.add(prevButton);

        nextButton = createIconButton(Labels.NEXT, ICON_NEXT);
        nextButton.setToolTipText("Next Chat");
        leftButtonPanel.add(nextButton);

        openInBrowserButton = createIconButton(Labels.VIEW, ICON_WEB);
        openInBrowserButton.setToolTipText("Open in Browser");
        openInBrowserButton.setVisible(getAllEditorCount() > 0);
        leftButtonPanel.add(openInBrowserButton);

        Set<String> modelsByProvider = getModelsByProvider(pm.getProvider());

        modelsByProvider.add(pm.getModelName());
        models = createStyledComboBox(modelsByProvider.toArray(new String[0]));
        models.setSelectedItem(pm.getChatModel() != null ? pm.getChatModel() : pm.getModel());
        models.setToolTipText("AI Models");
        models.addActionListener(e -> {
            String selectedModel = (String) models.getSelectedItem();
            if (selectedModel != null) {
                pm.setChatModel(selectedModel);
            }
        });
        leftButtonPanel.add(models);

        InteractionMode[] options = InteractionMode.values();
        actionComboBox = createStyledComboBox(options);
        String lastAction = pm.getAssistantAction();
        if (lastAction != null) {
            try {
                actionComboBox.setSelectedItem(InteractionMode.valueOf(lastAction));
            } catch (IllegalArgumentException ex) {
                actionComboBox.setSelectedItem(InteractionMode.ASK);
            }
        } else {
            actionComboBox.setSelectedItem(InteractionMode.ASK);
        }
        actionComboBox.setToolTipText(interactionModeTooltip());
        actionComboBox.addActionListener(e -> {
            InteractionMode selectedAction = (InteractionMode) actionComboBox.getSelectedItem();
            if (selectedAction != InteractionMode.ASK) {
                if (this.project == null) {
                    final Project selectedProject = selectProject();
                    if (selectedProject == null) {
                        actionComboBox.setSelectedItem(InteractionMode.ASK);
                    } else {
                        this.project = selectedProject;
                    }
                }
            }
            selectedAction = (InteractionMode) actionComboBox.getSelectedItem();
            if (selectedAction != null) {
                pm.setAssistantAction(selectedAction.name());
            }
        });
        leftButtonPanel.add(actionComboBox);

        JButton showChartsButton = createIconButton(Labels.STATS, ICON_STATS);
        showChartsButton.setToolTipText("Show Token Usage Charts");
        rightButtonPanel.add(showChartsButton);
        showChartsButton.addActionListener(e -> {
            TokenUsageChartDialog.showDialog(SwingUtilities.getWindowAncestor(showChartsButton));
        });

        JButton optionsButton = createIconButton(Labels.SETTINGS, ICON_SETTINGS);
        optionsButton.setToolTipText("Open Jeddict AI Assistant Settings");
        optionsButton.addActionListener(e -> OptionsDisplayer.getDefault().open("JeddictAIAssistant"));
        rightButtonPanel.add(optionsButton);

        JButton messageContextButton = createIconButton(Labels.MESSAGE_CONTEXT, ICON_ATTACH);
        messageContextButton.setToolTipText("Attach a file to current message context");
        rightButtonPanel.add(messageContextButton);

        JButton sessionContextButton = createIconButton(Labels.SESSION_CONTEXT, ICON_CONTEXT);
        sessionContextButton.setToolTipText("View detailed context of the current chat session context.");
        sessionContextButton.addActionListener(e -> onSessionContext());
        rightButtonPanel.add(sessionContextButton);

        JButton newChatButton = createIconButton(Labels.NEW_CHAT, ICON_NEW_CHAT);
        newChatButton.setToolTipText("Start a new chat");
        leftButtonPanel.add(newChatButton);

        submitButton = createIconButton(Labels.SEND, ICON_SEND);
        submitButton.setToolTipText("Ask a question");
        rightButtonPanel.add(submitButton);

        buttonPanelAdapter = new ComponentAdapter() {
            boolean showOnlyIcons = false;

            @Override
            public void componentResized(ComponentEvent e) {
                double totalButtonWidth = leftButtonPanel.getPreferredSize().width
                        + rightButtonPanel.getPreferredSize().width;
                if (showOnlyIcons) {
                    totalButtonWidth = totalButtonWidth * 2;
                }

                int availableWidth = buttonPanel.getWidth();
                showOnlyIcons = availableWidth < totalButtonWidth;

                updateButton(prevButton, showOnlyIcons, ICON_PREV, Labels.PREV + " " + ICON_PREV);
                updateButton(nextButton, showOnlyIcons, ICON_NEXT, Labels.NEXT + " " + ICON_NEXT);
                updateButton(openInBrowserButton, showOnlyIcons, ICON_WEB, Labels.VIEW + " " + ICON_WEB);
//                updateButton(copyButton, showOnlyIcons, ICON_COPY, Labels.COPY + " " + ICON_COPY);
//                updateButton(saveButton, showOnlyIcons, ICON_SAVE, Labels.SAVE + " " + ICON_SAVE);
//                updateButton(saveToEditorButton, showOnlyIcons, ICON_UPDATE, Labels.UPDATE + " " + ICON_UPDATE);
                updateButton(newChatButton, showOnlyIcons, ICON_NEW_CHAT, Labels.NEW_CHAT + " " + ICON_NEW_CHAT);
                updateButton(showChartsButton, showOnlyIcons, ICON_STATS, Labels.STATS + " " + ICON_STATS);
                updateButton(optionsButton, showOnlyIcons, ICON_SETTINGS, Labels.SETTINGS + " " + ICON_SETTINGS);
                updateButton(messageContextButton, showOnlyIcons, ICON_ATTACH, Labels.MESSAGE_CONTEXT + " " + ICON_ATTACH);
                updateButton(sessionContextButton, showOnlyIcons, ICON_CONTEXT, Labels.SESSION_CONTEXT + " " + ICON_CONTEXT);
                updateButton(submitButton, showOnlyIcons, ICON_SEND, Labels.SEND + " " + ICON_SEND);
                updateCombobox(models, showOnlyIcons);
                updateCombobox(actionComboBox, showOnlyIcons);
                updateUserPaneButtons(showOnlyIcons);

            }

            private void updateButton(JButton button, boolean iconOnly, String iconText, String fullText) {
                button.setText(iconOnly ? iconText : fullText);
            }

            private <T> void updateCombobox(JComboBox<T> comboBox, boolean iconOnly) {
                comboBox.putClientProperty("minimal", iconOnly);
            }
        };
        buttonPanel.addComponentListener(buttonPanelAdapter);

        questionPane = new JEditorPane();
        questionPane.setEditorKit(createEditorKit("text/x-" + (type == null ? "java" : type)));
        questionPane.putClientProperty("AI_QUERY_EDITOR", Boolean.TRUE);

        Document doc = questionPane.getDocument();
        doc.putProperty(JEDDICT_EDITOR_CALLBACK, (Consumer<FileObject>) this::addFileTab);

        questionScrollPane = new JScrollPane(questionPane);
        questionScrollPane.setBorder(BorderFactory.createEmptyBorder());
        questionScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        questionScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        Color bgColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        boolean isDark = ColorUtil.isDarkColor(bgColor);
        if (isDark) {
            questionScrollPane.getViewport().setBackground(Color.DARK_GRAY);
            questionScrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
            questionScrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
        }
        questionPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateHeight();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateHeight();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateHeight();
            }
        });

        newChatButton.addActionListener(e -> {
            this.clear();
            this.repaint();
            questionPane.setText("");
            clearFileTab();
            onChatReset();

        });

        openInBrowserButton.addActionListener(e -> {
            try {
                File latestTempFile = File.createTempFile("gen-ai", ".html");
                latestTempFile.deleteOnExit();
                try (FileWriter writer = new FileWriter(latestTempFile)) {
                    writer.write(this.getAllEditorText());
                }
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(latestTempFile.toURI());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        submitButton.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                onSubmit();

            }
        });

        prevButton.addActionListener(e -> {
            onPrev();
        });

        nextButton.addActionListener(e -> {
            onNext();
        });

        messageContextButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser(pm.getLastBrowseDirectory());
            int result = fileChooser.showOpenDialog(bottomPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                pm.setLastBrowseDirectory(selectedFile.getParent());
                addFileTab(FileUtil.toFileObject(selectedFile));
            }
        });

        FileTransferHandler.register(bottomPanel, this::addFileTab);
        FileTransferHandler.register(questionPane, this::addFileTab);

        //
        // intercept shortkeys to submit the prompt
        //
        final String actionKey = "submit-prompt";

        String shortcut = pm.getSubmitShortcut(); // e.g. "Ctrl + Enter", "Enter", "Shift + Enter", "Alt + Enter"

        javax.swing.KeyStroke submitKey;
        switch (shortcut) {
            case "Enter":
                submitKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
                break;
            case "Shift + Enter":
                submitKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK);
                break;
            default: // "Ctrl + Enter"
                submitKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK);
        }

        final javax.swing.InputMap inputMap = questionPane.getInputMap(JComponent.WHEN_FOCUSED);
        final javax.swing.ActionMap actionMap = questionPane.getActionMap();

        inputMap.put(submitKey, actionKey);
        actionMap.put(actionKey, submitButton.getAction());

        // ---

        confirmationPane = new ToolExecutionConfirmationPane();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(filePanel);
        bottomPanel.add(Box.createVerticalStrut(0));
        bottomPanel.add(questionScrollPane);
        bottomPanel.add(confirmationPane);
        bottomPanel.add(Box.createVerticalStrut(0));
        bottomPanel.add(buttonPanel);

        return bottomPanel;
    }

    /**
     * Acquire confirmation from the user, if needed by the current interaction
     * mode, to proceed with the execution of a tool. The confirmation message
     * is provided in {@code message} and returns a future boolean that shall
     * resolve to true if the tool can be executed, false otherwise.
     *
     * @param message the confirmation message
     *
     * @return future boolean that shall resolve to true if the tool can be
     *         executed, false otherwise
     */
    public Future<Boolean> promptConfirmation(final String message) {
        //
        // We check here again the interaction mode because the user may have
        // changed through the UI. However, this does not change the JeddictBrain's
        // interaction mode or how the PairProgrammer agents have been setup.
        // Note that to keep status informatin in the agents (i.e. memory) the
        // agents instances are created at AssistantChat creation, not per use.
        //
        // Ideally, if the user changes the interaction mode, we should recreate
        // a new JeddictBrain and the Assistant/Hacker. But this would make
        // the agent loose the memory.
        //
        // So for now we just enable/disable tool execution and confirmation.
        //
        if (isInteractiveMode()) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            SwingUtilities.invokeLater(() -> {
                confirmationPane.showMessage(message);

                // remove existing listeners if any
                for (PropertyChangeListener pcl : confirmationPane.getPropertyChangeListeners(JOptionPane.VALUE_PROPERTY)) {
                    confirmationPane.removePropertyChangeListener(JOptionPane.VALUE_PROPERTY, pcl);
                }

                confirmationPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (confirmationPane.isVisible() && evt.getNewValue() != null && evt.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
                            Object value = evt.getNewValue();
                            confirmationPane.setVisible(false);

                            boolean confirmed = (value instanceof Integer && ((Integer) value) == JOptionPane.YES_OPTION);
                            future.complete(confirmed);

                            // cleanup
                            confirmationPane.removePropertyChangeListener(JOptionPane.VALUE_PROPERTY, this);
                        }
                    }
                });

                // Revalidate to show
                confirmationPane.getParent().revalidate();
                confirmationPane.getParent().repaint();
            });
            return future;
        }

        return CompletableFuture.completedFuture(isAgentEnabled());
    }

    public void updateHeight() {
        int lineHeight = questionPane.getFontMetrics(questionPane.getFont()).getHeight();
        String text = questionPane.getText().trim();
        int preferredHeight;
        int linePadding = 5;
        if (text.isEmpty()) {
            preferredHeight = lineHeight * 3 + linePadding;
        } else {
            preferredHeight = questionPane.getPreferredSize().height;
            preferredHeight = Math.max(lineHeight * 3 + linePadding,
                    Math.min(preferredHeight, lineHeight * 12 + linePadding));
        }

        Dimension size = new Dimension(questionScrollPane.getWidth(), preferredHeight);
        questionScrollPane.setPreferredSize(size);
        questionScrollPane.revalidate();
    }

    public void lastRemove() {
        parentPanel.remove(parentPanel.getComponentCount() - 1);
    }

    public void clear() {
        parentPanel.removeAll();
        menus.clear();
    }

    public void updateUserPaneButtons(boolean iconOnly) {
        if (copyButton != null) {
            copyButton.setText(iconOnly ? ICON_COPY : Labels.COPY + " " + ICON_COPY);
            editButton.setText(iconOnly ? ICON_EDIT : Labels.EDIT + " " + ICON_EDIT);
            saveButton.setText(iconOnly ? ICON_SEND : Labels.SAVE + " " + ICON_SEND);
            cancelButton.setText(iconOnly ? ICON_CANCEL : Labels.CANCEL + " " + ICON_CANCEL);
        }
    }

    private void createUserPaneButtons(Consumer<String> queryUpdate, Set<FileObject> messageContext, JPanel buttonPanel) {
        copyButton = QueryPane.createIconButton(Labels.COPY, ICON_COPY);
        editButton = QueryPane.createIconButton(Labels.EDIT, ICON_EDIT);
        saveButton = QueryPane.createIconButton(Labels.SAVE, ICON_SEND);
        cancelButton = QueryPane.createIconButton(Labels.CANCEL, ICON_CANCEL);

        Consumer<Boolean> state = bool -> {
            copyButton.setVisible(bool);
            editButton.setVisible(bool);
            saveButton.setVisible(!bool);
            cancelButton.setVisible(!bool);
        };
        state.accept(true);

        saveButton.addActionListener(e -> {
            queryPane.setEditable(false);
            state.accept(true);

            String question = queryPane.getText();
            if (!question.isEmpty()) {
                queryUpdate.accept(question);
            }
        });

        cancelButton.addActionListener(e -> {
            queryPane.setEditable(false);
            state.accept(true);
        });

        buttonPanel.add(copyButton);
        buttonPanel.add(editButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        copyButton.addActionListener(e -> queryPane.copy());
        editButton.addActionListener(e -> {
            queryPane.setEditable(true);
            state.accept(false);
            queryPane.requestFocus();
        });
    }

    public void clearFiles() {
        filePanel.removeAll();
        refreshFilePanel();
        filePanel.revalidate();
        filePanel.repaint();
    }

    public void addFile(FileObject file, Consumer<FileObject> onCloseCallback) {
        FileTab fileTab = new FileTab(file, filePanel, onCloseCallback);
        filePanel.add(fileTab);
        refreshFilePanel();
        filePanel.revalidate();
        filePanel.repaint();
    }

    public void refreshFilePanel() {
        filePanelAdapter.componentResized(null);
    }

    public JEditorPane createUserQueryPane(Consumer<String> queryUpdate, String content, Set<FileObject> messageContext) {

        Consumer<FileObject> callback = file -> {
            if (!messageContext.contains(file)) {
                messageContext.add(file);
                FileTab fileTab = new FileTab(file, filePanel, f -> {
                    messageContext.remove(f);
                    filePanelAdapter.componentResized(null);
                });
                filePanel.add(fileTab);
                filePanelAdapter.componentResized(null);
                filePanel.revalidate();
                filePanel.repaint();
            }
        };

        queryPane = new JEditorPane();
        queryPane.setEditorKit(createEditorKit("text/x-" + (type == null ? "java" : type)));
        queryPane.setText(content);
        queryPane.setEditable(false);
        queryPane.putClientProperty("AI_QUERY_EDITOR", Boolean.TRUE);
        Document doc = queryPane.getDocument();
        doc.putProperty(JEDDICT_EDITOR_CALLBACK, (Consumer<FileObject>) callback);

        Font newFont = getFontFromMimeType(MIME_PLAIN_TEXT);
        Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        boolean isDark = ColorUtil.isDarkColor(backgroundColor);
        queryPane.setBackground(isDark ? backgroundColor.brighter() : ColorUtil.darken(backgroundColor, .05f));
        queryPane.setFont(newFont);
        queryPane.setForeground(textColor);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(queryPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        bottomPanel.setBackground(isDark ? backgroundColor.brighter() : ColorUtil.darken(backgroundColor, .05f));
        filePanel = new JPanel();
        filePanelAdapter = new MessageContextComponentAdapter(filePanel);
        bottomPanel.addComponentListener(filePanelAdapter);
        filePanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        filePanel.setBorder(BorderFactory.createEmptyBorder());
        filePanel.setBackground(isDark ? backgroundColor.brighter() : ColorUtil.darken(backgroundColor, .05f));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        buttonPanel.setBackground(isDark ? backgroundColor.brighter() : ColorUtil.darken(backgroundColor, .05f));
        buttonPanel.setFont(newFont);
        buttonPanel.setForeground(textColor);
        createUserPaneButtons(queryUpdate, messageContext, buttonPanel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.85;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.WEST;
        bottomPanel.add(filePanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.15;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.EAST;
        bottomPanel.add(buttonPanel, gbc);

        wrapper.add(bottomPanel, BorderLayout.SOUTH);

        for (FileObject fileObject : messageContext) {
            FileTab fileTab = new FileTab(fileObject, filePanel, f -> {
                messageContext.remove(f);
                filePanelAdapter.componentResized(null);
            });
            filePanel.add(fileTab);
        }
        filePanelAdapter.componentResized(null);
        filePanel.revalidate();
        filePanel.repaint();
        FileTransferHandler.register(bottomPanel, callback);
        FileTransferHandler.register(queryPane, callback);

        parentPanel.add(wrapper);
        return queryPane;
    }


    public JEditorPane createHtmlPane(String content) {
        JEditorPane editorPane = new JEditorPane();
        addEditorPaneRespectingTextArea(editorPane);
        MarkdownPane.createHtmlPane(editorPane, content, this);
        return editorPane;
    }

    private void addEditorPaneRespectingTextArea(JComponent component) {
        int count = parentPanel.getComponentCount();
        if (count > 0) {
            Component lastComponent = parentPanel.getComponent(count - 1);
            if (lastComponent instanceof JTextArea) {
                parentPanel.add(component, count - 1);
                return;
            }
        }
        parentPanel.add(component);
    }

    public JTextArea createTextAreaPane() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        Font newFont = getFontFromMimeType(MIME_PLAIN_TEXT);
        Font emojiFont = new Font("Segoe UI Emoji", newFont.getStyle(), newFont.getSize());  // TODO: what if the font does not exist?
        java.awt.Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        java.awt.Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
        textArea.setFont(emojiFont);
        textArea.setForeground(textColor);
        textArea.setBackground(backgroundColor);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        parentPanel.add(textArea);
        return textArea;
    }

    public JEditorPane createPane() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        Font newFont = getFontFromMimeType(MIME_PLAIN_TEXT);
        java.awt.Color textColor = getTextColorFromMimeType(MIME_PLAIN_TEXT);
        java.awt.Color backgroundColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);

        editorPane.setFont(newFont);
        editorPane.setForeground(textColor);
        editorPane.setBackground(backgroundColor);
        addEditorPaneRespectingTextArea(editorPane);
        return editorPane;
    }

    public JEditorPane createCodePane(String mimeType, TextBlock content) {
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = MIME_PLAIN_TEXT;
        }

        EditorKit editorKit = EditorUtil.createEditorKit(mimeType == null ? ("text/x-" + type) : mimeType);

        JEditorPane editorPane;
        if (editorKit != null) {
            editorPane = new JEditorPane();
            editorPane.setEditorKit(editorKit);
        } else {
            editorPane = createInMemoryEditorCopy(mimeType);
        }

        editorPane.setText(content.getContent());
        editorPane.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                content.setContent(editorPane.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                content.setContent(editorPane.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                content.setContent(editorPane.getText());
            }
        });
        editorPane.setCaretPosition(0);
        addContextMenu(editorPane);
        addEditorPaneRespectingTextArea(editorPane);
        return editorPane;
    }

    public SVGPane createSVGPane(TextBlock content) {
        SVGPane svgPane = new SVGPane();
        JEditorPane sourcePane = svgPane.createPane(content);
        addContextMenu(sourcePane);
        addEditorPaneRespectingTextArea(svgPane);
        return svgPane;
    }

    public MermaidPane createMermaidPane(TextBlock content) {
        MermaidPane pane = new MermaidPane();
        JEditorPane sourcePane = pane.createPane(content);
        addContextMenu(sourcePane);
        addEditorPaneRespectingTextArea(pane);
        return pane;
    }

    public MarkdownPane createMarkdownPane(TextBlock content) {
        MarkdownPane pane = new MarkdownPane();
        JEditorPane sourcePane = pane.createPane(content, this);
        addContextMenu(sourcePane);
        addEditorPaneRespectingTextArea(pane);
        return pane;
    }

    public DiffPane createDiffPane(final String path, final String content) {
        LOG.finest(() -> "createDiffPane for interaction " + path);
        final DiffPane diffPane = new DiffPane(getProject(), path, content);
        diffPane.createPane();
        addEditorPaneRespectingTextArea(diffPane);

        return diffPane;
    }

    public ToolExecutionPane createToolExecutionPane(
        final ToolExecutionRequest execution, final String result
    ) {
        LOG.finest(() -> "createToolExecutionPane for execution " + execution);

        final ToolExecutionPane toolPane = new ToolExecutionPane(execution, result);
        addEditorPaneRespectingTextArea(toolPane);

        return toolPane;
    }

    private void addContextMenu(JEditorPane editorPane) {
        JPopupMenu contextMenu = new JPopupMenu();
        menus.put(editorPane, contextMenu);
        menuItems.clear();
        submenuItems.clear();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(e -> {
            if (editorPane.getSelectedText() != null) {
                // Copy selected text
                editorPane.copy();
            } else {
                // Select all and copy
                editorPane.selectAll();
                editorPane.copy();
                editorPane.select(0, 0);
            }
        });
        contextMenu.add(copyItem);

        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(e -> saveAs(editorPane.getContentType(), editorPane.getText()));
        contextMenu.add(saveAsItem);

        // Add mouse listener to show context menu
        editorPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    public void saveAs(String mimeType, String content) {

        // Create the file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save As");

        if (mimeType != null && mimeType.equals(JAVA_MIME)) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Java Files", "java"));
            String className = extractClassName(content);
            String packageName = extractPackageName(content);
            boolean isTestClass = className != null && className.endsWith("Test");
            String baseDir = isTestClass ? "src/test/java" : "src/main/java";

            if (project != null) {
                String projectRootDir = project.getProjectDirectory().getPath();
                String filePath = Paths.get(projectRootDir, baseDir).toString();
                if (packageName != null) {
                    filePath = Paths.get(filePath, packageName.split("\\.")).toString();
                }
                File targetDir = new File(filePath);
                if (!targetDir.exists()) {
                    boolean dirsCreated = targetDir.mkdirs();
                    if (!dirsCreated) {
                        JOptionPane.showMessageDialog(null, "Failed to create directories: " + targetDir.getAbsolutePath());
                        return;
                    }
                }
                fileChooser.setCurrentDirectory(new File(filePath));
            }
            if (className != null) {
                String defaultFileName = className.endsWith(".java") ? className : className + ".java";
                fileChooser.setSelectedFile(new File(defaultFileName));
            }
        } else {
            if (project != null) {
                String filePath = project.getProjectDirectory().getPath();
                if (isSuitableForWebAppDirectory(mimeType)) {
                    filePath = Paths.get(filePath, "src/main/webapp").toString();
                }
                fileChooser.setCurrentDirectory(new File(filePath));
            } else {
                fileChooser.setCurrentDirectory(new File(pm.getLastBrowseDirectory()));
            }
            String fileExtension = getExtension(mimeType);
            if (fileExtension != null) {
                fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(convertToCapitalized(fileExtension) + " Files", fileExtension));
            }
        }

        // Show the save dialog
        int userSelection = fileChooser.showSaveDialog(null);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String fileExtension = getExtension(mimeType);
            if (!file.getName().endsWith("." + fileExtension)
                    && fileExtension != null) {
                file = new File(file.getAbsolutePath() + "." + fileExtension);
            }
            pm.setLastBrowseDirectory(file.getParent());
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error saving file: " + ex.getMessage());
            }
        }
    }

    public String extractClassName(String content) {
        String regex = "(?<=\\bclass\\s+)\\w+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(); // Return the class name found
        } else {
            return null; // No class name found
        }
    }

    // Method to extract Java package name using regex
    public String extractPackageName(String content) {
        String regex = "(?<=\\bpackage\\s+)([a-zA-Z_][a-zA-Z0-9_]*(\\.[a-zA-Z_][a-zA-Z0-9_]*)*);?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1); // Return the package name found (group 1 without the semicolon)
        } else {
            return null; // No package name found
        }
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        questionPane.setText(prefs.get(QUESTION_KEY, ""));
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        Preferences prefs = Preferences.userNodeForPackage(this.getClass());
        prefs.put(QUESTION_KEY, questionPane.getText());
    }

    public JPanel getParentPanel() {
        return parentPanel;
    }

    public String getAllCodeEditorText() {
        StringBuilder allText = new StringBuilder();
        for (int i = 0; i < parentPanel.getComponentCount(); i++) {
            if (parentPanel.getComponent(i) instanceof JEditorPane editorPane) {
                if (!(editorPane.getEditorKit() instanceof javax.swing.text.html.HTMLEditorKit)) {
                    allText.append("\n");
                    allText.append(editorPane.getText());
                    allText.append("\n");
                }
            }
        }
        return allText.toString().trim();
    }

    public String getAllEditorText() {
        StringBuilder allText = new StringBuilder();
        for (int i = 0; i < parentPanel.getComponentCount(); i++) {
            if (parentPanel.getComponent(i) instanceof JEditorPane editorPane) {
                if (!editorPane.getEditorKit().getContentType().equals("text/html")
                        && editorPane.getEditorKit().getContentType().startsWith("text")) {
                    allText.append("<pre><code>");
                    allText.append(editorPane.getText());
                    allText.append("</code></pre>");
                } else {
                    allText.append(editorPane.getText().replaceAll("(?is)<style[^>]*?>.*?</style>", ""));
                }
            }
        }
        return allText.toString().trim();
    }

    public int getAllCodeEditorCount() {
        int count = 0;
        for (int i = 0; i < parentPanel.getComponentCount(); i++) {
            if (parentPanel.getComponent(i) instanceof JEditorPane) {
                JEditorPane editorPane = (JEditorPane) parentPanel.getComponent(i);
                if (!editorPane.getEditorKit().getContentType().equals("text/html")
                        && editorPane.getEditorKit().getContentType().startsWith("text")) {
                    count++;
                }
            }
        }
        return count;
    }

    public void getParseCodeEditor(List<FileObject> context) {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
        Map<JEditorPane, Map<String, String>> editorMethodSignCache = new HashMap<>();
        Map<JEditorPane, Map<String, String>> editorMethodCache = new HashMap<>();

        //
        // Parse all files in context and all code blocks in the editors to
        // extract method/class/interface signatures and code and create context
        // menus to diff each block.
        //
        // NOTES (TODO):
        // 1. this code should probably be refactored breaking it in smaller
        //    pieces at least, but most importantly to separate the collection
        //    logic and the logic to create the menu
        // 2. the main loop processes multiple times the EditorPane components,
        //    they should probably be separated into two same-level loops
        // 3. the loops to fill cachedMethodSignatures and editorMethodCache
        //    can probably be combined into one
        // 4. before changing this logic, a unit test to drive the collection
        //    logic should be first created.
        // 5. editors are processed only if they contain java code; this is a
        //    limitation that can probably be removed as we should be able to
        //    do some diffs even for files other than java
        //
        //
        for (FileObject fileObject : context) {
            if (!fileObject.getExt().equalsIgnoreCase("java")) {
                continue;
            }
            //
            // Parse the file and extract all method declarations
            //
            try (InputStream stream = fileObject.getInputStream()) {
                String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                CompilationUnit cu = StaticJavaParser.parse(content);
                List<MethodDeclaration> methods = cu.findAll(MethodDeclaration.class);

                //
                // Save all method signatures (excluding anonymous inner class
                // methods) in map fileMethodSignatures with the signature as
                // key and the method code size (method.toString().length()) as
                // value
                //
                Map<String, Integer> fileMethodSignatures = new HashMap<>();
                for (MethodDeclaration method : methods) {
                    if (isAnonymousInnerMethod(method)) {
                        continue; // Skip anonymous inner class method
                    }
                    String signature = buildMethodSignature(method);
                    fileMethodSignatures.put(signature, method.toString().length());
                }

                //
                // Count the methods overloads and save the counters in map
                // fileMethods<method name, count>
                //
                Map<String, Long> fileMethods = new HashMap<>();
                for (MethodDeclaration method : methods) {
                    if (isAnonymousInnerMethod(method)) {
                        continue; // Skip anonymous inner class method
                    }
                    String methodName = method.getNameAsString();
                    fileMethods.put(methodName, fileMethods.getOrDefault(methodName, 0L) + 1);
                }

                //
                // For each EditorPane (i.e. code block) collect the method,
                // class and interface signatures parsing the block first as a
                // method, if it fails, as a class, if it fails as an interface.
                // For each element the corresponding code is also saved in the
                // map cachedMethodSignatures<signature, code>.
                // Do the same with just the method/class/interface name, saving
                // name and code in map cachedMethods<name, code>.
                // (note that the two steps above are done in two separate loops,
                // they can probbly be merged into one.
                //
                // Once the cachedMethodSignatures and cachedMethods are filled,
                // loop through the signatures found in the context files (saved
                // in fileMethodSignatures and fileMethods) and create context
                // manues to diff block and file code.
                //
                for (int i = 0; i < parentPanel.getComponentCount(); i++) {
                    if (parentPanel.getComponent(i) instanceof JEditorPane) {
                        JEditorPane editorPane = (JEditorPane) parentPanel.getComponent(i);
                        if (editorPane.getEditorKit().getContentType().equals(JAVA_MIME)) {
                            Map<String, String> cachedMethodSignatures = editorMethodSignCache.computeIfAbsent(editorPane, ep -> {
                                Map<String, String> snippetSignatures = new HashMap<>();
                                String editorText = editorPane.getText();
                                String[] lines = editorText.split("\n");
                                try {
                                    // check if snippet is method otherwise throw exception
                                    extractMethod(snippetSignatures, editorText);
                                } catch (Exception e1) {
                                    try {
                                        CompilationUnit aiCu = StaticJavaParser.parse(editorText);
                                        extractClasses(aiCu, lines, snippetSignatures, editorText);
                                        extractMethods(aiCu, lines, snippetSignatures);
                                    } catch (Exception e2) {
                                        try {
                                            CompilationUnit aiCu = StaticJavaParser.parse(editorText);
                                            extractClasses(aiCu, lines, snippetSignatures, editorText);
                                            if (aiCu.getTypes().isNonEmpty()) {
                                                snippetSignatures.put(aiCu.getType(0).getNameAsString(), editorText);
                                            }
                                        } catch (Exception e3) {
                                            // ignore
                                        }
                                    }
                                }
                                return snippetSignatures;
                            });

                            Map<String, String> cachedMethods = editorMethodCache.computeIfAbsent(editorPane, ep -> {
                                Map<String, String> snippetSignatures = new HashMap<>();
                                String editorText = editorPane.getText();
                                String[] lines = editorText.split("\n");
                                try {
                                    extractMethod(snippetSignatures, editorText);
                                } catch (Exception e) {
                                    try {
                                        CompilationUnit aiCu = StaticJavaParser.parse(editorText);
                                        extractMethods(aiCu, lines, snippetSignatures);
                                    } catch (Exception e1) {
                                        try {
                                            CompilationUnit aiCu = StaticJavaParser.parse(editorText);
                                            if (aiCu.getTypes().isNonEmpty()) {
                                                snippetSignatures.put(aiCu.getType(0).getNameAsString(), editorText);
                                            }
                                        } catch (Exception e2) {
                                            // ignore
                                        }
                                    }
                                }
                                return snippetSignatures;
                            });

                            //
                            // First check if there is any full signature match
                            // in fileMethodSignatures and if there is, create a
                            // context menu to diff it; if no context menu have
                            // been created on the full signature, check method/class/interface
                            // names.
                            // Then create a menu item to diff with the file
                            //
                            try {
                                int menuCreationCount = 0;
                                for (Map.Entry<String, Integer> signature : fileMethodSignatures.entrySet()) {
                                    if (createEditorPaneMenus(fileObject, signature.getKey(), signature.getValue(), editorPane, cachedMethodSignatures)) {
                                        menuCreationCount++;
                                    }
                                }
                                if (menuCreationCount == 0) {
                                    for (String method : fileMethods.keySet()) {
                                        if (fileMethods.get(method) == 1) {
                                            if (createEditorPaneMenus(fileObject, method, -1, editorPane, cachedMethods)) {
                                                menuCreationCount++;
                                            }
                                        }
                                    }
                                }

                                createEditorPaneMenus(fileObject, fileObject.getName(), -1, editorPane, cachedMethodSignatures);
                            } catch (Exception e) {
                                LOG.finest(() -> "Error parsing single method declaration from editor content: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.finest(() -> "Error parsing file: " + fileObject.getName() + " - " + e.getMessage());
            }
        }
    }

    /**
     * Add the menus to the relevant editor's context menu
     */
    public void attachMenusToEditors() {
        // Create the menu to diff with selected text
        for (int i = 0; i < parentPanel.getComponentCount(); i++) {
            if (parentPanel.getComponent(i) instanceof JEditorPane editorPane) {
                menuItems.computeIfAbsent(editorPane, k -> new ArrayList<>());
                if (JAVA_MIME.equals(editorPane.getEditorKit().getContentType())) {
                    JMenuItem diffMethodItem = new JMenuItem("Diff with Selected Snippet");
                    diffMethodItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
                        JTextComponent currenteditor = EditorRegistry.lastFocusedComponent();
                        String currentSelectedText = currenteditor.getSelectedText();
                        final StyledDocument currentDocument = (StyledDocument) currenteditor.getDocument();
                        DataObject currentDO = NbEditorUtilities.getDataObject(currentDocument);
                        if (currentDO != null) {
                            FileObject focusedfile = currentDO.getPrimaryFile();
                            if (focusedfile != null && currentSelectedText != null && !currentSelectedText.trim().isEmpty()) {
                                diffActionWithSelected(currentSelectedText, focusedfile, editorPane);
                            } else {
                                JOptionPane.showMessageDialog(null, "Please select text in the source editor.");
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "Please select text in the source editor.");
                        }
                    }));
                    menuItems.get(editorPane).add(diffMethodItem);
                }
            }
        }

        for (Map.Entry<JEditorPane, List<JMenuItem>> entry : menuItems.entrySet()) {
            JPopupMenu mainMenu = menus.get(entry.getKey());
            if (mainMenu != null) {
                for (JMenuItem jMenuItem : entry.getValue()) {
                    mainMenu.add(jMenuItem);
                }
            }
        }

        for (Map.Entry<JEditorPane, List<JMenuItem>> entry : submenuItems.entrySet()) {
            JMenu methodMenu = new JMenu("Methods");
            for (JMenuItem jMenuItem : entry.getValue()) {
                methodMenu.add(jMenuItem);
            }
            JPopupMenu mainMenu = menus.get(entry.getKey());
            if (mainMenu != null) {
                mainMenu.add(methodMenu);
            }
        }
    }

    public Project getProject() {
        return project;
    }

    public Project selectProject() {
        final Project[] openProjects = org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
        if (openProjects.length == 1) {
            project = openProjects[0];
            DialogDisplayer.getDefault().notify(
                new NotifyDescriptor.Message(
                        "Connected chat to project: " + ProjectUtils.getInformation(project).getDisplayName(),
                        NotifyDescriptor.INFORMATION_MESSAGE
                )
            );
        } else if (openProjects.length > 1) {
            JComboBox<Project> projectComboBox = new JComboBox<>(openProjects);
            projectComboBox.setRenderer(new javax.swing.ListCellRenderer<>() {
                private final javax.swing.DefaultListCellRenderer defaultRenderer = new javax.swing.DefaultListCellRenderer();

                @Override
                public java.awt.Component getListCellRendererComponent(javax.swing.JList<? extends Project> list, Project value, int index, boolean isSelected, boolean cellHasFocus) {
                    String displayName = (value == null) ? "" : ProjectUtils.getInformation(value).getDisplayName();
                    return defaultRenderer.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
                }
            });
            NotifyDescriptor descriptor = new NotifyDescriptor(
                projectComboBox,
                "Select Project for AI Agent Mode",
                NotifyDescriptor.OK_CANCEL_OPTION,
                NotifyDescriptor.QUESTION_MESSAGE,
                null,
                NotifyDescriptor.OK_OPTION
            );
            Object dialogResult = DialogDisplayer.getDefault().notify(descriptor);
            if (NotifyDescriptor.OK_OPTION.equals(dialogResult)) {
                Project selectedProject = (Project) projectComboBox.getSelectedItem();
                if (selectedProject != null) {
                    project = selectedProject;
                    DialogDisplayer.getDefault().notify(
                        new NotifyDescriptor.Message(
                            "Connected chat to project: " + ProjectUtils.getInformation(project).getDisplayName(),
                            NotifyDescriptor.INFORMATION_MESSAGE
                        )
                    );

                    return project;
                }
            }
        } else {
            NotifyDescriptor.Message msg = new NotifyDescriptor.Message(
                    "To use AI agent mode, connect chat to any project by dropping any source file on chat window or start new chat from project/package/source file context.",
                    NotifyDescriptor.WARNING_MESSAGE
            );
            DialogDisplayer.getDefault().notify(msg);
            actionComboBox.setSelectedItem(InteractionMode.ASK);
        }

        return null;
    }

    // --------------------------------------------------------- private methods

    private boolean isAnonymousInnerMethod(MethodDeclaration method) {
        return method.getParentNode().isPresent() && method.getParentNode().get() instanceof ObjectCreationExpr;
    }

    private String buildMethodSignature(MethodDeclaration method) {
        return method.getNameAsString() + "("
                + method.getParameters().stream()
                        .map(param -> param.getType().asString())
                        .collect(Collectors.joining(",")) + ")";
    }

    private void extractClasses(CompilationUnit aiCu, String[] lines,
            Map<String, String> snippetSignatures, String source) {
        List<ClassOrInterfaceDeclaration> classDecls = aiCu.findAll(ClassOrInterfaceDeclaration.class);
        int classesCount = classDecls.size();

        for (ClassOrInterfaceDeclaration classDecl : classDecls) {
            if (classesCount == 1 || classDecl.isPublic()) {
                snippetSignatures.put(classDecl.getNameAsString(), source);
            } else {
                classDecl.getRange().ifPresent(range -> {
                    String classSource = extractSource(lines, range.begin.line, range.end.line);
                    snippetSignatures.put(classDecl.getNameAsString(), classSource);
                });
            }
        }
    }

    private void extractMethods(CompilationUnit aiCu, String[] lines, Map<String, String> snippetSignatures) {
        List<MethodDeclaration> aiMethods = aiCu.findAll(MethodDeclaration.class);
        for (MethodDeclaration aiMethod : aiMethods) {
            String signature = buildMethodSignature(aiMethod);
            aiMethod.getRange().ifPresent(range -> {
                String methodSource = extractSource(lines, range.begin.line, range.end.line);
                snippetSignatures.put(signature, methodSource);
            });
        }
    }

    private void extractMethod(Map<String, String> snippetSignatures, String editorText) {
        MethodDeclaration aiMethod = StaticJavaParser.parseMethodDeclaration(editorText);
        String signature = aiMethod.getNameAsString();
        snippetSignatures.put(signature, editorText);
    }

    private String extractSource(String[] lines, int startLine, int endLine) {
        StringBuilder sb = new StringBuilder();
        for (int i = startLine - 1; i <= endLine - 1; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }

    private boolean createEditorPaneMenus(FileObject fileObject, String signature, Integer bodyLength, JEditorPane editorPane, Map<String, String> cachedMethodSignatures) {
        boolean classSignature = fileObject.getName().equals(signature);
        if (cachedMethodSignatures.get(signature) != null
                && (cachedMethodSignatures.get(signature).length() != bodyLength || bodyLength == -1)) {
            if (menuItems.get(editorPane) == null) {
                menuItems.put(editorPane, new ArrayList<>());
            }
            if (submenuItems.get(editorPane) == null) {
                submenuItems.put(editorPane, new ArrayList<>());
            }
            String menuSubText = (classSignature ? "" : (signature + " in "));
            JMenuItem updateMethodItem = new JMenuItem("Update " + menuSubText + fileObject.getName());
            updateMethodItem.addActionListener(e -> {
                SwingUtilities.invokeLater(() -> {
                    if (signature.equals(fileObject.getName())) {
                        SourceUtil.updateFullSourceInFile(fileObject, cachedMethodSignatures.get(signature));
                    } else {
                        SourceUtil.updateMethodInSource(fileObject, signature, cachedMethodSignatures.get(signature));
                    }
                });
            });
            if (fileObject.getName().equals(signature)) {
                menuItems.get(editorPane).add(updateMethodItem);
            } else {
                submenuItems.get(editorPane).add(updateMethodItem);
            }

            JMenuItem diffMethodItem = new JMenuItem("Diff " + menuSubText + fileObject.getName());
            diffMethodItem.addActionListener(e -> {
                SwingUtilities.invokeLater(() -> {
                    diffAction(classSignature, fileObject, signature, editorPane, cachedMethodSignatures);
                });
            });
            if (fileObject.getName().equals(signature)) {
                menuItems.get(editorPane).add(diffMethodItem);
            } else {
                submenuItems.get(editorPane).add(diffMethodItem);
            }
            return true;
        }
        return false;
    }

    public int getAllEditorCount() {
        int count = 0;
        for (int i = 0; i < parentPanel.getComponentCount(); i++) {
            if (parentPanel.getComponent(i) instanceof JEditorPane) {
                count++;
            }
        }
        return count;
    }

    public List<Review> getReviews() {
        return reviews;
    }

    public void setReviews(List<Review> reviews) {
        this.reviews = reviews;
    }

    private String interactionModeTooltip() {
        final StringBuffer sb = new StringBuffer("<html>");
        on(InteractionMode.values()).loop((mode) -> {
            sb.append("<b>").append(mode.toString()).append("</b> - ");
            sb.append(
                NbBundle.getMessage(AssistantChat.class, mode.name())
            ).append("<br>");
        });
        sb.append("</html>");

        return sb.toString();
    }


    public void response(final Response response) {
        this.response = response;
    }

    public Response response() {
        return response;
    }
}
