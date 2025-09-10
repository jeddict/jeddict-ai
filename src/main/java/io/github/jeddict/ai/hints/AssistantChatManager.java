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
package io.github.jeddict.ai.hints;

import io.github.jeddict.ai.agent.AssistantAction;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.ENUM;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.util.TreePath;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.jeddict.ai.JeddictUpdateManager;
import static io.github.jeddict.ai.classpath.JeddictQueryCompletionQuery.JEDDICT_EDITOR_CALLBACK;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.completion.SQLCompletion;
import io.github.jeddict.ai.components.AssistantChat;
import static io.github.jeddict.ai.components.AssistantChat.createEditorKit;
import io.github.jeddict.ai.components.ContextDialog;
import io.github.jeddict.ai.components.CustomScrollBarUI;
import io.github.jeddict.ai.components.FileTab;
import io.github.jeddict.ai.components.FileTransferHandler;
import static io.github.jeddict.ai.components.MarkdownPane.getHtmlWrapWidth;
import io.github.jeddict.ai.components.MessageContextComponentAdapter;
import static io.github.jeddict.ai.components.QueryPane.createIconButton;
import static io.github.jeddict.ai.components.QueryPane.createStyledComboBox;
import io.github.jeddict.ai.components.TokenUsageChartDialog;
import io.github.jeddict.ai.lang.JeddictChatModel;
import io.github.jeddict.ai.lang.JeddictStreamHandler;
import io.github.jeddict.ai.response.Block;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.review.Review;
import static io.github.jeddict.ai.review.ReviewUtil.convertReviewsToHtml;
import static io.github.jeddict.ai.review.ReviewUtil.parseReviewsFromYaml;
import static io.github.jeddict.ai.settings.GenAIProvider.getModelsByProvider;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.ColorUtil;
import static io.github.jeddict.ai.util.ContextHelper.getFilesContextList;
import static io.github.jeddict.ai.util.ContextHelper.getImageFilesContext;
import static io.github.jeddict.ai.util.ContextHelper.getProjectContext;
import static io.github.jeddict.ai.util.ContextHelper.getTextFilesContext;
import io.github.jeddict.ai.util.EditorUtil;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getHTMLContent;
import static io.github.jeddict.ai.util.Icons.ICON_ATTACH;
import static io.github.jeddict.ai.util.Icons.ICON_CONTEXT;
import static io.github.jeddict.ai.util.Icons.ICON_NEW_CHAT;
import static io.github.jeddict.ai.util.Icons.ICON_NEXT;
import static io.github.jeddict.ai.util.Icons.ICON_PREV;
import static io.github.jeddict.ai.util.Icons.ICON_SEND;
import static io.github.jeddict.ai.util.Icons.ICON_SETTINGS;
import static io.github.jeddict.ai.util.Icons.ICON_STATS;
import static io.github.jeddict.ai.util.Icons.ICON_WEB;
import io.github.jeddict.ai.util.Labels;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import static io.github.jeddict.ai.util.ProjectUtil.getSourceFiles;
import io.github.jeddict.ai.util.RandomTweetSelector;
import io.github.jeddict.ai.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.Document;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.options.OptionsDisplayer;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.spi.java.hints.JavaFix;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Shiwani Gupta
 */
public class AssistantChatManager extends JavaFix {

    private static final Logger LOG = Logger.getLogger(AssistantChatManager.class.getCanonicalName());

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public static final String ASSISTANT_CHAT_MANAGER_KEY = "ASSISTANT_CHAT_MANAGER_KEY";

    private TreePath treePath;
    private final Action action;
    private SQLCompletion sqlCompletion;
    private ComponentAdapter buttonPanelAdapter;
    private JComboBox<String> models;
    private JComboBox<AssistantAction> actionComboBox;
    private Timer timer;
    private JButton prevButton, nextButton, openInBrowserButton, submitButton;//copyButton, saveButton,
    private AssistantChat assistantChat;
    private JEditorPane questionPane;
    private JScrollPane questionScrollPane;
    private final List<Response> responseHistory = new ArrayList<>();
    private int currentResponseIndex = -1;
    private String sourceCode;
    private Project projectContext;
    private Project project;
    private final Set<FileObject> threadContext = new HashSet<>();
    private final Set<FileObject> messageContext = new HashSet<>();
    private FileObject fileObject;
    private String commitChanges;
    private final PreferencesManager pm = PreferencesManager.getInstance();
    private Tree leaf;

    private Project getProject() {
        if (project == null) {
            if (projectContext != null) {
                project = projectContext;
            } else if (threadContext != null && !threadContext.isEmpty()) {
                project = FileOwnerQuery.getOwner(threadContext.toArray(FileObject[]::new)[0]);
            } else if (fileObject != null) {
                project = FileOwnerQuery.getOwner(fileObject);
            } else if (messageContext != null && !messageContext.isEmpty()) {
                project = FileOwnerQuery.getOwner(messageContext.toArray(FileObject[]::new)[0]);
            }
        }
        return project;
    }

    public AssistantChatManager(TreePathHandle tpHandle, Action action, TreePath treePath) {
        super(tpHandle);
        this.treePath = treePath;
        this.action = action;
    }

    public AssistantChatManager(Action action) {
        super(null);
        this.action = action;
    }

    public AssistantChatManager(Action action, SQLCompletion sqlCompletion) {
        super(null);
        this.action = action;
        this.sqlCompletion = sqlCompletion;
    }

    public AssistantChatManager(Action action, Project project) {
        super(null);
        this.action = action;
        this.projectContext = project;
    }

    public AssistantChatManager(Action action, List<FileObject> selectedFileObjects) {
        super(null);
        this.action = action;
        this.threadContext.addAll(selectedFileObjects);
    }

    public AssistantChatManager(Action action, FileObject selectedFileObject) {
        super(null);
        this.action = action;
        this.threadContext.add(selectedFileObject);
    }

    @Override
    protected String getText() {
        if (action == Action.LEARN) {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_LEARN",
                    StringUtil.convertToCapitalized(treePath.getLeaf().getKind().toString()));
        } else if (action == Action.QUERY) {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_QUERY",
                    StringUtil.convertToCapitalized(treePath.getLeaf().getKind().toString()));
        } else if (action == Action.TEST) {
            return NbBundle.getMessage(JeddictUpdateManager.class, "HINT_TEST",
                    StringUtil.convertToCapitalized(treePath.getLeaf().getKind().toString()));
        }
        return null;
    }

    @Override
    protected void performRewrite(JavaFix.TransformationContext tc) throws Exception {
        WorkingCopy copy = tc.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        leaf = tc.getPath().getLeaf();
        this.fileObject = copy.getFileObject();

        if (leaf.getKind() == CLASS
                || leaf.getKind() == INTERFACE
                || leaf.getKind() == ENUM
                || leaf.getKind() == METHOD) {
            executorService.submit(() -> {
                String name;
                if (leaf instanceof MethodTree) {
                    name = ((MethodTree) leaf).getName().toString();
                } else {
                    name = ((ClassTree) leaf).getSimpleName().toString();
                }
                String fileName = fileObject != null ? fileObject.getName() : null;
                Set<FileObject> messageContextCopy = new HashSet<>(messageContext);
                SwingUtilities.invokeLater(() -> {
                    displayHtmlContent(fileName, name + " AI Assistant");
                    JeddictStreamHandler handler = new JeddictStreamHandler(assistantChat) {
                        @Override
                        public void onCompleteResponse(ChatResponse response) {
                            super.onCompleteResponse(response);

                            final Response r = new Response(null, response.aiMessage().text(), messageContextCopy);
                            sourceCode = EditorUtil.updateEditors(null, getProject(), assistantChat, r, getContextFiles());
                            responseHistory.add(r);
                            currentResponseIndex = responseHistory.size() - 1;
                        }
                    };
                    String response;
                    if (action == Action.TEST) {
                        if (leaf instanceof MethodTree) {
                            response = new JeddictChatModel(handler, getModelName()).generateTestCase(getProject(), null, null, leaf.toString(), null, null);
                        } else {
                            response = new JeddictChatModel(handler, getModelName()).generateTestCase(getProject(), null, treePath.getCompilationUnit().toString(), null, null, null);
                        }
                    } else {
                        if (leaf instanceof MethodTree) {
                            response = new JeddictChatModel(handler, getModelName()).assistJavaMethod(getProject(), leaf.toString());
                        } else {
                            response = new JeddictChatModel(handler, getModelName()).assistJavaClass(getProject(), treePath.getCompilationUnit().toString());
                        }
                    }
                    if (response != null && !response.isEmpty()) {
                        handler.onCompleteResponse(ChatResponse.builder().aiMessage(new AiMessage(response)).build());
                    }
                });

            });
        }
    }

    private Set<FileObject> getProjectContextList() {
        return getSourceFiles(projectContext);
    }

    public void askQueryForProjectCommit(Project project, String commitChanges, String intitalCommitMessage) {
        ProjectInformation info = ProjectUtils.getInformation(project);
        String projectName = info.getDisplayName();
        displayHtmlContent(null, projectName + " GenAI Commit");
        this.commitChanges = commitChanges;
        this.commitMessage = true;
        handleQuestion(intitalCommitMessage, messageContext, true);
    }

    private boolean commitMessage, codeReview;

    public void askQueryForCodeReview(Project project, String commitChanges, String contextMessage) {
        ProjectInformation info = ProjectUtils.getInformation(project);
        String projectName = info.getDisplayName();
        displayHtmlContent(null, projectName + " Code Review");
        this.commitChanges = commitChanges;
        this.codeReview = true;
        handleQuestion(contextMessage, messageContext, true);
    }

    public void displayHtmlContent(String filename, String title) {
        Preferences prefs = Preferences.userNodeForPackage(AssistantChat.class);
        prefs.putBoolean(AssistantChat.PREFERENCE_KEY, true);
        assistantChat = new AssistantChat(title, null, getProject());
        assistantChat.putClientProperty(ASSISTANT_CHAT_MANAGER_KEY, new WeakReference<>(AssistantChatManager.this));
        JScrollPane scrollPane = new JScrollPane(assistantChat.getParentPanel());
        assistantChat.add(scrollPane, BorderLayout.CENTER);
        assistantChat.add(createBottomPanel(null, filename, null), BorderLayout.SOUTH);
        assistantChat.open();
        assistantChat.requestActive();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                if (assistantChat != null) {
                    assistantChat.close();
                }
            });
        }));
    }

    public void openChat(String type, final String query, String fileName, String title, Consumer<String> action) {
        SwingUtilities.invokeLater(() -> {
            new JeddictUpdateManager().checkForJeddictUpdate();
            Preferences prefs = Preferences.userNodeForPackage(AssistantChat.class);
            prefs.putBoolean(AssistantChat.PREFERENCE_KEY, true);
            assistantChat = new AssistantChat(title, type, getProject());
            assistantChat.setLayout(new BorderLayout());
            assistantChat.putClientProperty(ASSISTANT_CHAT_MANAGER_KEY, new WeakReference<>(AssistantChatManager.this));
            JScrollPane scrollPane = new JScrollPane(assistantChat.getParentPanel());
            Color bgColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
            boolean isDark = ColorUtil.isDarkColor(bgColor);
            if (isDark) {
                scrollPane.getViewport().setBackground(Color.DARK_GRAY);
                scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
                scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
            }
            assistantChat.add(scrollPane, BorderLayout.CENTER);
            assistantChat.add(createBottomPanel(type, fileName, action), BorderLayout.SOUTH);
            if (PreferencesManager.getInstance().getChatPlacement().equals("Left")) {
                WindowManager.getDefault()
                        .findMode("explorer")
                        .dockInto(assistantChat);
            } else if (PreferencesManager.getInstance().getChatPlacement().equals("Right")) {
                WindowManager.getDefault()
                        .findMode("properties")
                        .dockInto(assistantChat);
            }
            assistantChat.open();
            assistantChat.requestActive();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (assistantChat != null) {
                    SwingUtilities.invokeLater(() -> assistantChat.close());
                }
            }));
            questionPane.setText(query);
            initialMessage();
        });
    }

    private static final String HOME_PAGE = "<div style='margin:20px; padding:20px; border-radius:10px;'>"
            + "<div style='text-align:center;'>"
            + "👋 <strong>Welcome!</strong><br><br>"
            + "I'm here to assist you with any questions you have.<br>"
            + "Feel free to ask anything!<br><br><br><br><br>"
            + "<a href='https://jeddict.github.io/page.html?l=tutorial/AI' style='text-decoration:none; color:#28a745;'>📄 View Documentation</a><br><br>"
            + "<a href='https://jeddict.github.io/page.html?l=tutorial/AIContext' style='text-decoration:none; color:#007bff;'>📘 Learn about context rules and scopes</a><br><br>"
            + "<a href='https://github.com/jeddict/jeddict-ai' style='text-decoration:none; color:#ff6600;'>⭐ Like it? Give us a star</a><br><br>"
            + "<a href='tweet' style='text-decoration:none; color:#1DA1F2;'>🐦 Tweet about Jeddict AI</a>"
            + "</div>"
            + "</div>";

    private void initialMessage() {
        JEditorPane init = assistantChat.createHtmlPane(HOME_PAGE);
        EventQueue.invokeLater(() -> questionPane.requestFocusInWindow());
        init.addHyperlinkListener(e -> {
            if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
                String link = e.getDescription();
                if ("home.html".equals(link)) {
                    try {
                        String content = getHTMLContent(getHtmlWrapWidth(init), HOME_PAGE);
                        init.setText(content);
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                } else if ("tweet".equals(link)) {
                    try {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(RandomTweetSelector.getRandomTweet()));
                    } catch (Exception ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        });
    }

    private JPanel filePanel;
    private MessageContextComponentAdapter filePanelAdapter;

    public void addFileTab(FileObject file) {
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
    }

    public void clearFileTab() {
        messageContext.clear();
        filePanel.removeAll();
        filePanelAdapter.componentResized(null);
        filePanel.revalidate();
        filePanel.repaint();
    }

    private JPanel createBottomPanel(String type, String fileName, Consumer<String> action) {
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
        openInBrowserButton.setVisible(assistantChat.getAllEditorCount() > 0);
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

        AssistantAction[] options = AssistantAction.values();
        actionComboBox = createStyledComboBox(options);
        actionComboBox.setSelectedItem(AssistantAction.ASK);
        actionComboBox.setToolTipText("<html><b>Chat</b> – for general queries<br><b>Agent</b> – for file/project generation actions</html>");
        actionComboBox.addActionListener(e -> {
            AssistantAction selectedAction = (AssistantAction) actionComboBox.getSelectedItem();
            if (selectedAction == AssistantAction.BUILD) {
                if (getProject() == null) {
                    Project[] openProjects = org.netbeans.api.project.ui.OpenProjects.getDefault().getOpenProjects();
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
                            } else {
                                actionComboBox.setSelectedItem(AssistantAction.ASK);
                            }
                        } else {
                            actionComboBox.setSelectedItem(AssistantAction.ASK);
                        }
                    } else {
                        NotifyDescriptor.Message msg = new NotifyDescriptor.Message(
                                "To use AI agent mode, connect chat to any project by dropping any source file on chat window or start new chat from project/package/source file context.",
                                NotifyDescriptor.WARNING_MESSAGE
                        );
                        DialogDisplayer.getDefault().notify(msg);
                        actionComboBox.setSelectedItem(AssistantAction.ASK);
                    }
                }
            }
        });
        leftButtonPanel.add(actionComboBox);

        int javaEditorCount = assistantChat.getAllCodeEditorCount();

//        copyButton = createIconButton(Labels.COPY, ICON_COPY);
//        copyButton.setToolTipText("Copy to clipboard");
//        copyButton.setVisible(javaEditorCount > 0);
//        leftButtonPanel.add(copyButton);
//
//        saveButton = createIconButton(Labels.SAVE, ICON_SAVE);
//        saveButton.setToolTipText("Save as");
//        saveButton.setVisible(javaEditorCount == 1);
//        leftButtonPanel.add(saveButton);
//        JButton saveToEditorButton = createIconButton(Labels.UPDATE + " " + fileName, ICON_UPDATE);
//        saveToEditorButton.setToolTipText("Update " + fileName);
//        saveToEditorButton.setVisible(fileName != null);
//        leftButtonPanel.add(saveToEditorButton);
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
        sessionContextButton.addActionListener(e -> showFilePathPopup());
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
                assistantChat.updateUserPaneButtons(showOnlyIcons);

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

//        copyButton.addActionListener(e -> {
//            StringSelection stringSelection = new StringSelection(topComponent.getAllCodeEditorText());
//            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//            clipboard.setContents(stringSelection, null);
//        });
//        saveButton.addActionListener(e -> {
//            topComponent.saveAs(null, topComponent.getAllCodeEditorText());
//        });
//        saveToEditorButton.addActionListener(e -> {
//            if (action != null) {
//                action.accept(topComponent.getAllCodeEditorText());
//            }
//        });
        newChatButton.addActionListener(e -> {
            assistantChat.clear();
            assistantChat.repaint();
            initialMessage();
            responseHistory.clear();
            questionPane.setText("");
            clearFileTab();
            currentResponseIndex = -1;
            updateButtons(prevButton, nextButton);
        });

        openInBrowserButton.addActionListener(e -> {
            try {
                File latestTempFile = File.createTempFile("gen-ai", ".html");
                latestTempFile.deleteOnExit();
                try ( FileWriter writer = new FileWriter(latestTempFile)) {
                    writer.write(assistantChat.getAllEditorText());
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
                if (result != null && !result.isDone()) {
                    NotifyDescriptor.Confirmation confirmDialog = new NotifyDescriptor.Confirmation(
                            "The AI Assistant is still processing the request. Do you want to cancel it?",
                            "Interrupt AI Assistant",
                            NotifyDescriptor.YES_NO_OPTION
                    );
                    Object answer = DialogDisplayer.getDefault().notify(confirmDialog);
                    if (NotifyDescriptor.YES_OPTION.equals(answer)) {
                        result.cancel(true);
                        if (handler != null && handler.getProgressHandle() != null) {
                            handler.getProgressHandle().finish();
                        }
                        result = null;
                        stopLoading();
                    }
                } else {
                    result = null;
                    String question = questionPane.getText();
                    Map<String, String> prompts = PreferencesManager.getInstance().getPrompts();

                    //
                    // To make sure the longest matching shortcut matches (i.e.
                    // 'shortcutlong' is 'shortcut' is defined as well) let's
                    // sort the shurtcuts in descending order; this guarantees
                    // 'shortcut2' is matched before "shortcut" in the for loop
                    //
                    ArrayList<String> promptKeys = new ArrayList();
                    promptKeys.addAll(prompts.keySet());
                    promptKeys.sort(Comparator.reverseOrder());

                    for (String key: promptKeys) {
                        String prompt = prompts.get(key);

                        String toReplace = "/" + key;

                        if (question.contains(toReplace)) {
                            question = question.replace(toReplace, prompt);
                        }
                    }
                    if (!question.isEmpty()) {
                        handleQuestion(question, messageContext, true);
                    }
                }
            }
        });

        BiConsumer<String, Set<FileObject>> queryUpdate = (newQuery, messageContext) -> {
            handleQuestion(newQuery, messageContext, false);
        };
        prevButton.addActionListener(e -> {
            if (currentResponseIndex > 0) {
                currentResponseIndex--;
                Response historyResponse = responseHistory.get(currentResponseIndex);
                sourceCode = EditorUtil.updateEditors(queryUpdate, getProject(), assistantChat, historyResponse, getContextFiles());
                updateButtons(prevButton, nextButton);
            }
        });

        nextButton.addActionListener(e -> {
            if (currentResponseIndex < responseHistory.size() - 1) {
                currentResponseIndex++;
                Response historyResponse = responseHistory.get(currentResponseIndex);
                sourceCode = EditorUtil.updateEditors(queryUpdate, getProject(), assistantChat, historyResponse, getContextFiles());
                updateButtons(prevButton, nextButton);
            }
        });

        updateButtons(prevButton, nextButton);

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

        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(filePanel);
        bottomPanel.add(Box.createVerticalStrut(0));
        bottomPanel.add(questionScrollPane);
        bottomPanel.add(Box.createVerticalStrut(0));
        bottomPanel.add(buttonPanel);

        return bottomPanel;
    }

    private void startLoading() {
        final String[] spinnerFrames = {"◐", "◓", "◑", "◒"};
        final int[] frameIndex = {0};
        timer = new Timer(200, e -> {
            submitButton.setText(spinnerFrames[frameIndex[0]]);
            frameIndex[0] = (frameIndex[0] + 1) % spinnerFrames.length;
        });
        timer.start();
    }

    private void stopLoading() {
        timer.stop();
    }

    private void updateHeight() {
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

    private Set<FileObject> getContextFiles() {
        Set<FileObject> fileObjects = new HashSet<>();
        if (projectContext != null) {
            fileObjects.addAll(getProjectContextList());
        }
        if (threadContext != null) {
            fileObjects.addAll(getFilesContextList(threadContext));
        }
        return fileObjects;
    }

    private void showFilePathPopup() {
        Set<FileObject> fileObjects = getContextFiles();
        String projectRootDir = null;
        if (projectContext != null) {
            projectRootDir = projectContext.getProjectDirectory().getPath();
        } else if (!fileObjects.isEmpty()) {
            projectRootDir = FileOwnerQuery.getOwner(fileObjects.iterator().next()).getProjectDirectory().getPath();
        }

        boolean enableRules = true;
        String rules = pm.getSessionRules();
        if (commitChanges != null) {
            rules = commitChanges;
            enableRules = false;
        }
        ContextDialog dialog = new ContextDialog((JFrame) SwingUtilities.windowForComponent(assistantChat),
                enableRules, rules,
                projectRootDir, fileObjects);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(800, 800);
        dialog.setLocationRelativeTo(SwingUtilities.windowForComponent(assistantChat));
        dialog.setVisible(true);
        if (commitChanges == null) {
            pm.setSessionRules(dialog.getRules());
        }
    }

    private Future result;
    private JeddictStreamHandler handler;

    private void handleQuestion(String question, Set<FileObject> messageContext, boolean newQuery) {
        result = executorService.submit(() -> {
            try {
                startLoading();
                if (currentResponseIndex >= 0
                        && currentResponseIndex + 1 < responseHistory.size()) {
                    responseHistory.subList(currentResponseIndex + 1, responseHistory.size()).clear();
                }
                if (!newQuery && !responseHistory.isEmpty()) {
                    responseHistory.remove(responseHistory.size() - 1);
                    currentResponseIndex = currentResponseIndex - 1;
                }

                int historyCount = pm.getConversationContext();
                List<Response> prevChatResponses;
                if (historyCount == -1) {
                    // Entire conversation
                    prevChatResponses = new ArrayList<>(responseHistory);
                } else {
                    int startIndex = Math.max(0, responseHistory.size() - historyCount);
                    prevChatResponses = responseHistory.subList(startIndex, responseHistory.size());
                }
                Set<FileObject> messageContextCopy = new HashSet<>(messageContext);
                handler = new JeddictStreamHandler(assistantChat) {
                    @Override
                    public void onCompleteResponse(ChatResponse response) {
                        super.onCompleteResponse(response);

                        final String textResponse = response.aiMessage().text();
                        final Response r = new Response(question, textResponse, messageContextCopy);
                        if (responseHistory.isEmpty() || !textResponse.equals(responseHistory.get(responseHistory.size() - 1))) {
                            responseHistory.add(r);
                            currentResponseIndex = responseHistory.size() - 1;
                        }
                        SwingUtilities.invokeLater(() -> {
                            BiConsumer<String, Set<FileObject>> queryUpdate = (newQuery, messageContext) -> {
                                handleQuestion(newQuery, messageContext, false);
                            };
                            if (codeReview) {
                                List<Review> reviews = parseReviewsFromYaml(r.getBlocks().get(0).getContent());
                                String web = convertReviewsToHtml(reviews);
                                assistantChat.setReviews(reviews);
                                r.getBlocks().clear();
                                r.getBlocks().add(new Block("web", web));
                            }
                            sourceCode = EditorUtil.updateEditors(queryUpdate, getProject(), assistantChat, r, getContextFiles());

                            stopLoading();
                            updateButtons(prevButton, nextButton);
                            buttonPanelAdapter.componentResized(null);
                        });
                    }
                };
                String response;
                if (sqlCompletion != null) {
                    String context = sqlCompletion.getMetaData();
                    String messageScopeContent = getTextFilesContext(messageContext);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    List<String> messageScopeImgages = getImageFilesContext(messageContext);
                    response = new JeddictChatModel(handler, getModelName()).assistDbMetadata(context, question, messageScopeImgages, prevChatResponses);
                } else if (commitMessage && commitChanges != null) {
                    String context = commitChanges;
                    String messageScopeContent = getTextFilesContext(messageContext);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    List<String> messageScopeImgages = getImageFilesContext(messageContext);
                    response = new JeddictChatModel(handler, getModelName()).generateCommitMessageSuggestions(context, question, messageScopeImgages, prevChatResponses);
                } else if (codeReview && commitChanges != null) {
                    String context = commitChanges;
                    String messageScopeContent = getTextFilesContext(messageContext);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    List<String> messageScopeImgages = getImageFilesContext(messageContext);
                    response = new JeddictChatModel(handler, getModelName()).generateCodeReviewSuggestions(context, question, messageScopeImgages, prevChatResponses);
                } else if (projectContext != null || threadContext != null) {
                    Set<FileObject> mainThreadContext;
                    String threadScopeContent;
                    if (projectContext != null) {
                        mainThreadContext = getProjectContextList();
                        threadScopeContent = getProjectContext(mainThreadContext);
                    } else {
                        mainThreadContext = this.threadContext;
                        threadScopeContent = getTextFilesContext(mainThreadContext);
                    }
                    List<String> threadScopeImgages = getImageFilesContext(mainThreadContext);

                    Set<FileObject> fitleredMessageContext = new HashSet<>(messageContext);
                    fitleredMessageContext.removeAll(mainThreadContext);
                    String messageScopeContent = getTextFilesContext(fitleredMessageContext);
                    List<String> messageScopeImgages = getImageFilesContext(fitleredMessageContext);
                    List<String> images = new ArrayList<>();
                    images.addAll(threadScopeImgages);
                    images.addAll(messageScopeImgages);
                    if (actionComboBox.getSelectedItem() == AssistantAction.BUILD) {
                        response = new JeddictChatModel(handler, getModelName()).agent(getProject(), threadScopeContent + '\n' + messageScopeContent, null, images, prevChatResponses, question);
                    } else {
                        response = new JeddictChatModel(handler, getModelName()).generateDescription(getProject(), threadScopeContent + '\n' + messageScopeContent, null, images, prevChatResponses, question);
                    }
                } else if (treePath == null) {
                    response = new JeddictChatModel(handler, getModelName()).generateDescription(getProject(), null, null, null, prevChatResponses, question);
                } else if (action == Action.TEST) {
                    if (leaf instanceof MethodTree) {
                        response = new JeddictChatModel(handler, getModelName()).generateTestCase(getProject(), null, null, leaf.toString(), prevChatResponses, question);
                    } else {
                        response = new JeddictChatModel(handler, getModelName()).generateTestCase(getProject(), null, treePath.getCompilationUnit().toString(), null, prevChatResponses, question);
                    }
                } else {
                    response = new JeddictChatModel(handler, getModelName()).generateDescription(getProject(), treePath.getCompilationUnit().toString(), treePath.getLeaf() instanceof MethodTree ? treePath.getLeaf().toString() : null, null, prevChatResponses, question);
                }

                if (response != null && !response.isEmpty()) {
                    handler.onCompleteResponse(ChatResponse.builder().aiMessage(new AiMessage(response)).build());
                }

                questionPane.setText("");
                updateHeight();
                clearFileTab();
            } catch (Exception e) {
                e.printStackTrace();
                buttonPanelAdapter.componentResized(null);
//                submitButton.setEnabled(true);
            }
        });
    }

    private String getModelName() {
        String modelName = (String) models.getSelectedItem();
        if (modelName == null || modelName.isEmpty()) {
            return pm.getModel();
        }
        return modelName;
    }

    private void updateButtons(JButton prevButton, JButton nextButton) {
        prevButton.setVisible(currentResponseIndex > 0);
        nextButton.setVisible(currentResponseIndex < responseHistory.size() - 1);

        int javaEditorCount = assistantChat.getAllCodeEditorCount();
//        copyButton.setVisible(javaEditorCount > 0);
//        saveButton.setVisible(javaEditorCount > 0);

        openInBrowserButton.setVisible(assistantChat.getAllEditorCount() > 0);
    }

    public void addToSessionContext(List<FileObject> files) {
        threadContext.addAll(files);
    }

}
