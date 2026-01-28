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

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import static com.sun.source.tree.Tree.Kind.CLASS;
import static com.sun.source.tree.Tree.Kind.ENUM;
import static com.sun.source.tree.Tree.Kind.INTERFACE;
import static com.sun.source.tree.Tree.Kind.METHOD;
import com.sun.source.util.TreePath;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.github.jeddict.ai.JeddictUpdateManager;
import io.github.jeddict.ai.agent.AbstractTool;
import io.github.jeddict.ai.agent.InteractiveFileEditor;
import io.github.jeddict.ai.agent.ExecutionTools;
import io.github.jeddict.ai.agent.ExplorationTools;
import io.github.jeddict.ai.agent.FileSystemTools;
import io.github.jeddict.ai.agent.GradleTools;
import io.github.jeddict.ai.agent.MavenTools;
import io.github.jeddict.ai.agent.RefactoringTools;
import io.github.jeddict.ai.agent.pair.Assistant;
import io.github.jeddict.ai.agent.pair.DBSpecialist;
import io.github.jeddict.ai.agent.pair.DiffSpecialist;
import io.github.jeddict.ai.agent.pair.Hacker;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.agent.pair.TechWriter;
import io.github.jeddict.ai.agent.pair.TestSpecialist;
import io.github.jeddict.ai.completion.Action;
import io.github.jeddict.ai.completion.SQLCompletion;
import io.github.jeddict.ai.components.AssistantChat;
import io.github.jeddict.ai.components.AssistantJeddictBrainListener;
import io.github.jeddict.ai.components.ContextDialog;
import io.github.jeddict.ai.components.CustomScrollBarUI;
import static io.github.jeddict.ai.components.MarkdownPane.getHtmlWrapWidth;
import io.github.jeddict.ai.lang.InteractionMode;
import static io.github.jeddict.ai.lang.InteractionMode.INTERACTIVE;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import io.github.jeddict.ai.response.TextBlock;
import io.github.jeddict.ai.response.Response;
import io.github.jeddict.ai.response.ToolExecutionBlock;
import io.github.jeddict.ai.review.Review;
import static io.github.jeddict.ai.review.ReviewUtil.convertReviewsToHtml;
import static io.github.jeddict.ai.review.ReviewUtil.parseReviewsFromYaml;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import io.github.jeddict.ai.settings.PreferencesManager;
import io.github.jeddict.ai.util.ColorUtil;
import static io.github.jeddict.ai.util.ContextHelper.getFilesContextList;
import static io.github.jeddict.ai.util.ContextHelper.getImageFilesContext;
import static io.github.jeddict.ai.util.ContextHelper.getProjectContext;
import static io.github.jeddict.ai.util.ContextHelper.getTextFilesContext;
import io.github.jeddict.ai.util.EditorUtil;
import static io.github.jeddict.ai.util.EditorUtil.getBackgroundColorFromMimeType;
import static io.github.jeddict.ai.util.EditorUtil.getHTMLContent;
import static io.github.jeddict.ai.util.MimeUtil.MIME_PLAIN_TEXT;
import static io.github.jeddict.ai.util.ProjectUtil.getSourceFiles;
import io.github.jeddict.ai.util.RandomTweetSelector;
import io.github.jeddict.ai.util.StringUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.HyperlinkEvent;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.TreePathHandle;
import org.netbeans.api.java.source.WorkingCopy;
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

    public static final String ASSISTANT_CHAT_MANAGER_KEY = "ASSISTANT_CHAT_MANAGER_KEY";

    private TreePath treePath;
    private final Action action;
    private SQLCompletion sqlCompletion;
    private AssistantChat ac;
    private final List<Response> responseHistory = new ArrayList<>();
    private int currentResponseIndex = -1;
    private String sourceCode;
    private Project projectContext;
    private final Set<FileObject> sessionContext = new HashSet<>();
    private final Set<FileObject> messageContext = new HashSet<>();
    private FileObject fileObject;
    private String commitChanges;
    private final PreferencesManager pm = PreferencesManager.getInstance();
    private Tree leaf;
    private final Map<String, String> params = new HashMap();
    private String question;
    private Future result;
    private AssistantJeddictBrainListener listener;

    private boolean commitMessage, codeReview;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /*
     * After a first kickoff in performRewrite the conversation can continue in
     * the chat window, handled in the handleQuestion() method. This requires
     * the history to be retained, which is done by langchain4j's agents, as
     * long as the same instance is used. Therefore testSpecialist,
     * dbSpecialist, diffSpecialist keep the instance of the agent once created.
     */
    private TestSpecialist testSpecialist = null;
    private DBSpecialist dbSpecialist = null;
    private DiffSpecialist diffSpecialist = null;

    /*
     * Similarly, a conversation opened creating a new chat window or from
     * project context, shall retain memory of the conversation
     */
    private Assistant assistant = null;
    private Assistant projectAssistant = null;
    private Hacker hacker = null;

    private Project getProject() {
        Project project = null;

        if (projectContext != null) {
            project = projectContext;
        } else if (sessionContext != null && !sessionContext.isEmpty()) {
            project = FileOwnerQuery.getOwner(sessionContext.toArray(FileObject[]::new)[0]);
        } else if (fileObject != null) {
            project = FileOwnerQuery.getOwner(fileObject);
        } else if (messageContext != null && !messageContext.isEmpty()) {
            project = FileOwnerQuery.getOwner(messageContext.toArray(FileObject[]::new)[0]);
        } else if (ac != null) {
            project = ac.getProject();
        }

        final Project logProject = project;
        LOG.finest(() -> "returning project " + logProject);

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
        this.sessionContext.addAll(selectedFileObjects);
    }

    public AssistantChatManager(Action action, FileObject selectedFileObject) {
        super(null);
        this.action = action;
        this.sessionContext.add(selectedFileObject);
    }

    public AssistantChatManager(
        final Action action,
        final Project project,
        final Map<String, String> params
    ) {
        super(null);
        this.action = action;
        this.projectContext = project;
        this.params.putAll(params);
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
    protected void performRewrite(JavaFix.TransformationContext context) throws Exception {
        WorkingCopy copy = context.getWorkingCopy();
        if (copy.toPhase(JavaSource.Phase.RESOLVED).compareTo(JavaSource.Phase.RESOLVED) < 0) {
            return;
        }
        leaf = context.getPath().getLeaf();
        this.fileObject = copy.getFileObject();

        if (leaf.getKind() == CLASS
            || leaf.getKind() == INTERFACE
            || leaf.getKind() == ENUM
            || leaf.getKind() == METHOD) {

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
                JeddictBrainListener listener = new AssistantJeddictBrainListener(ac) {
                    @Override
                    public void onChatCompleted(final ChatResponse response) {
                        super.onChatCompleted(response);

                        final Response r = new Response(null, response.aiMessage().text(), messageContextCopy);
                        sourceCode = EditorUtil.updateEditors(null, ac, r, getContextFiles());

                        responseHistory.add(r);
                        currentResponseIndex = responseHistory.size() - 1;
                    }
                };
                String modelName = ac.getModelName();
                if (action == Action.TEST) {
                    final TestSpecialist pair = testSpecialist(listener, modelName);
                    final String prompt = pm.getPrompts().get("test");
                    final String rules = pm.getSessionRules();
                    if (leaf instanceof MethodTree) {
                        async(() -> pair.generateTestCase(null, null, null, leaf.toString(), prompt, rules), listener);
                    } else {
                        async(() -> pair.generateTestCase(null, null, treePath.getCompilationUnit().toString(), null, prompt, rules), listener);
                    }
                } else {
                    final String rules = pm.getSessionRules();
                    final TechWriter pair =
                        newJeddictBrain(listener, modelName, InteractionMode.ASK)
                        .pairProgrammer(PairProgrammer.Specialist.TECHWRITER);
                    if (leaf instanceof MethodTree) {
                        async(() -> pair.describeCode(leaf.toString(), rules), listener);
                    } else {
                        async(() -> pair.describeCode(treePath.getCompilationUnit().toString(), rules), listener);
                    }
                }
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
        handlePrompt(intitalCommitMessage, true);
    }

    public void askQueryForCodeReview() {
        ProjectInformation info = ProjectUtils.getInformation(projectContext);
        String projectName = info.getDisplayName();
        displayHtmlContent(null, projectName + " Code Review");
        this.codeReview = true;
        handlePrompt("", true);
    }

    private AssistantChat createChatInstance(String title, String type, Project project) {
        final Consumer<String> queryUpdate = (newQuery) -> {
            handlePrompt(newQuery, false);
        };

        final AssistantChat chat = new AssistantChat(title, type, project) {
            @Override
            public void onChatReset() {
                initialMessage();
                responseHistory.clear();
                currentResponseIndex = -1;
                updateButtons(currentResponseIndex > 0, currentResponseIndex < responseHistory.size() - 1);
            }

            @Override
            public void onSubmit() {
                if (isLoading()) {
                    NotifyDescriptor.Confirmation confirmDialog = new NotifyDescriptor.Confirmation(
                            "The AI Assistant is still processing the request. Do you want to cancel it?",
                            "Interrupt AI Assistant",
                            NotifyDescriptor.YES_NO_OPTION
                    );
                    Object answer = DialogDisplayer.getDefault().notify(confirmDialog);
                    if (NotifyDescriptor.YES_OPTION.equals(answer)) {
                        result.cancel(true); result = null;
                        stopLoading();
                    }
                } else {
                    result = null;
                    String question = getQuestionPane().getText();
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

                    for (String key : promptKeys) {
                        String prompt = prompts.get(key);

                        String toReplace = "/" + key;

                        if (question.contains(toReplace)) {
                            question = question.replace(toReplace, prompt);
                        }
                    }
                    if (!question.isEmpty()) {
                        handlePrompt(question, true);
                    }
                }
            }

            @Override
            public void onPrev() {
                if (currentResponseIndex > 0) {
                    currentResponseIndex--;
                    Response historyResponse = responseHistory.get(currentResponseIndex);
                    sourceCode = EditorUtil.updateEditors(queryUpdate, ac, historyResponse, getContextFiles());
                    updateButtons(currentResponseIndex > 0, currentResponseIndex < responseHistory.size() - 1);
                }
            }

            @Override
            public void onNext() {
                if (currentResponseIndex < responseHistory.size() - 1) {
                    currentResponseIndex++;
                    Response historyResponse = responseHistory.get(currentResponseIndex);
                    sourceCode = EditorUtil.updateEditors(queryUpdate, ac, historyResponse, getContextFiles());
                    updateButtons(currentResponseIndex > 0, currentResponseIndex < responseHistory.size() - 1);
                }
            }

            @Override
            public void onSessionContext() {
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
                ContextDialog dialog = new ContextDialog((JFrame) SwingUtilities.windowForComponent(ac),
                        enableRules, rules,
                        projectRootDir, fileObjects);
                dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                dialog.setSize(800, 800);
                dialog.setLocationRelativeTo(SwingUtilities.windowForComponent(ac));
                dialog.setVisible(true);
                if (commitChanges == null) {
                    pm.setSessionRules(dialog.getRules());
                }
            }

            @Override
            public void addFileTab(FileObject file) {
                if (!messageContext.contains(file)) {
                    messageContext.add(file);
                     Consumer<FileObject> onCloseCallback =  f -> {
                        messageContext.remove(f);
                        ac.refreshFilePanel();
                    };
                    ac.addFile(file, onCloseCallback);
                }
            }

            @Override
            public void clearFileTab() {
                messageContext.clear();
                clearFiles();
            }
        };

        handler(chat);

        return chat;
    }

    public void displayHtmlContent(String filename, String title) {
        ac = createChatInstance(title, null, getProject());
        ac.putClientProperty(ASSISTANT_CHAT_MANAGER_KEY, new WeakReference<>(AssistantChatManager.this));
        JScrollPane scrollPane = new JScrollPane(ac.getParentPanel());
        ac.add(scrollPane, BorderLayout.CENTER);
        ac.add(ac.createBottomPanel(null, filename, null), BorderLayout.SOUTH);
        ac.open();
        ac.requestActive();
        ac.updateButtons(currentResponseIndex > 0, currentResponseIndex < responseHistory.size() - 1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                if (ac != null) {
                    ac.close();
                }
            });
        }));
    }

    public void openChat(String type, final String query, String fileName, String title, Consumer<String> action) {
        SwingUtilities.invokeLater(() -> {
            new JeddictUpdateManager().checkForJeddictUpdate();
            ac = createChatInstance(title, type, getProject());
            ac.setLayout(new BorderLayout());
            ac.putClientProperty(ASSISTANT_CHAT_MANAGER_KEY, new WeakReference<>(AssistantChatManager.this));
            JScrollPane scrollPane = new JScrollPane(ac.getParentPanel());
            Color bgColor = getBackgroundColorFromMimeType(MIME_PLAIN_TEXT);
            boolean isDark = ColorUtil.isDarkColor(bgColor);
            if (isDark) {
                scrollPane.getViewport().setBackground(Color.DARK_GRAY);
                scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
                scrollPane.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
            }
            ac.add(scrollPane, BorderLayout.CENTER);
            ac.add(ac.createBottomPanel(type, fileName, action), BorderLayout.SOUTH);
            if (PreferencesManager.getInstance().getChatPlacement().equals("Left")) {
                WindowManager.getDefault()
                        .findMode("explorer")
                        .dockInto(ac);
            } else if (PreferencesManager.getInstance().getChatPlacement().equals("Right")) {
                WindowManager.getDefault()
                        .findMode("properties")
                        .dockInto(ac);
            }
            ac.open();
            ac.requestActive();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (ac != null) {
                    SwingUtilities.invokeLater(() -> ac.close());
                }
            }));
            if (!query.isEmpty()) {
                ac.getQuestionPane().setText(query);
            }
            initialMessage();
        });
    }

    public void addToSessionContext(List<FileObject> files) {
        sessionContext.addAll(files);
    }

    // --------------------------------------------------------- private methods
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
        JEditorPane init = ac.createHtmlPane(HOME_PAGE);
        EventQueue.invokeLater(() -> ac.getQuestionPane().requestFocusInWindow());
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

    private Set<FileObject> getContextFiles() {
        Set<FileObject> fileObjects = new HashSet<>();
        if (projectContext != null) {
            fileObjects.addAll(getProjectContextList());
        }
        if (sessionContext != null) {
            fileObjects.addAll(getFilesContextList(sessionContext, pm.getFileExtensionListToInclude()));
        }
        return fileObjects;
    }

    private void handlePrompt(String question, boolean newQuery) {
        this.question = question;
        ac.startLoading();
        result = executorService.submit(() -> {
            //
            // Note thay history is not the same think as memory. The former
            // is applicaiton specific and has the purpose of reconstruct
            // exactly the history of a chat from a user perspective. The
            // lateter is model specific and may be different from the history
            // (for example if the model - or the app, decides to remove
            // or summarize past messages to improve the efficiency of the
            // response)
            if (currentResponseIndex >= 0
                    && currentResponseIndex + 1 < responseHistory.size()) {
                responseHistory.subList(currentResponseIndex + 1, responseHistory.size()).clear();
            }
            if (!newQuery && !responseHistory.isEmpty()) {
                responseHistory.remove(responseHistory.size() - 1);
                currentResponseIndex = currentResponseIndex - 1;
            }

            final int historySize = pm.getConversationContext();
            List<Response> prevChatResponses;
            if (historySize == -1) {
                // Entire conversation
                prevChatResponses = new ArrayList<>(responseHistory);
            } else {
                int startIndex = Math.max(0, responseHistory.size() - historySize);
                prevChatResponses = responseHistory.subList(startIndex, responseHistory.size());
            }

            final boolean agentEnabled = ac.isAgentEnabled();
            final boolean excludeJavadoc = pm.isExcludeJavadocEnabled();
            final String globalRules = pm.getGlobalRules();
            final String sessionRules = pm.getSessionRules();
            final List<String> includeFiles = pm.getFileExtensionListToInclude();
            final String modelName = ac.getModelName();

            String response = null; // This will hold the response for non-streaming cases
            try {
                if (sqlCompletion != null) {
                    String context = sqlCompletion.getMetaData();
                    String messageScopeContent = getTextFilesContext(messageContext, getProject(), excludeJavadoc, includeFiles, agentEnabled);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    response = dbSpecialist(listener, modelName).assistDbMetadata(question, context, sessionRules);
                } else if (commitMessage && commitChanges != null) {
                    String context = commitChanges;
                    String messageScopeContent = getTextFilesContext(messageContext, getProject(), excludeJavadoc, includeFiles, agentEnabled);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    response = diffSpecialist(listener, modelName).suggestCommitMessages(context, question);
                } else if (codeReview) {
                    String context = params.get("diff");
                    if (context == null) {
                        context = "";
                    }
                    final String messageScopeContent = getTextFilesContext(messageContext, projectContext, excludeJavadoc, includeFiles, agentEnabled);
                    if (messageScopeContent != null && !messageScopeContent.isEmpty()) {
                        context = context + "\n\n Files:\n" + messageScopeContent;
                    }
                    response = diffSpecialist(listener, modelName).reviewChanges(context, params.get("granularity"), params.get("feature"));
                } else if (action == Action.TEST) {
                    final TestSpecialist pair = testSpecialist(listener, modelName);
                    final String prompt = pm.getPrompts().get("test");
                    final String rules = pm.getSessionRules();
                    if (leaf instanceof MethodTree) {
                        response = pair.generateTestCase(question, null, null, leaf.toString(), prompt, rules, prevChatResponses);
                    } else {
                        response = pair.generateTestCase(question, null, treePath.getCompilationUnit().toString(), null, prompt, rules, prevChatResponses);
                    }
                } else if (projectContext != null || sessionContext != null) {
                    Project selectedProject = getProject();
                    //
                    // Here we are in a generic chat created by the user
                    //
                    // If not agentic, provide all project context, otherwise
                    // no project context is provided, just the global and
                    // project rules; the agent is instructed to gather the
                    // information it requires using tools
                    //
                    final String projectInfo = ProjectMetadataInfo.get(selectedProject);
                    if (!agentEnabled) {
                        final Set<FileObject> mainSessionContext;
                        final String sessionScopeContent;

                        if (projectContext != null) {
                            mainSessionContext = getProjectContextList();
                            sessionScopeContent = getProjectContext(mainSessionContext, selectedProject, excludeJavadoc, agentEnabled);
                        } else {
                            mainSessionContext = new HashSet(sessionContext);
                            sessionScopeContent = getTextFilesContext(mainSessionContext, selectedProject, excludeJavadoc, includeFiles, agentEnabled);
                        }
                        List<String> sessionScopeImages = getImageFilesContext(mainSessionContext, includeFiles);

                        Set<FileObject> fitleredMessageContext = new HashSet(messageContext);
                        fitleredMessageContext.removeAll(mainSessionContext);
                        String messageScopeContent = getTextFilesContext(fitleredMessageContext, selectedProject, excludeJavadoc, includeFiles, agentEnabled);
                        List<String> messageScopeImages = getImageFilesContext(fitleredMessageContext, includeFiles);
                        List<String> images = new ArrayList<>();
                        images.addAll(sessionScopeImages);
                        images.addAll(messageScopeImages);

                        final String prompt = question
                                + "\nSession content: " + sessionScopeContent
                                + "\nAdditional conent: " + messageScopeContent;

                        final Assistant a = projectAssistant(listener, modelName);
                        if (pm.isStreamEnabled()) {
                            a.chat(listener, prompt, images, projectInfo, globalRules, sessionRules);
                        } else {
                            response = a.chat(prompt, images, projectInfo, globalRules, sessionRules);
                        }
                    } else {
                        //
                        // When agentic mode is enabled, a project is required
                        // (in the future we may just add a tool to pick a
                        // project if none is seleted).
                        //
                        if (agentEnabled && (selectedProject == null)) {
                            ac.selectProject();
                            selectedProject = getProject();
                        }
                        final Hacker h = hacker(listener, modelName, ac.interactiveMode());
                        if (pm.isStreamEnabled()) {
                            h.hack(listener, question, projectInfo, pm.getGlobalRules(), sessionRules);
                        } else {
                            response = h.hack(question, projectInfo, pm.getGlobalRules(), sessionRules);
                        }
                    }
                } else {
                    final Assistant a = assistant(listener, modelName);
                    final String projectInfo = ProjectMetadataInfo.get(getProject());
                    if (pm.isStreamEnabled()) {
                        a.chat(listener, question, treePath, projectInfo, globalRules, sessionRules);
                    } else {
                        response = a.chat(question, treePath, projectInfo, globalRules, sessionRules);
                    }
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
                ac.buttonPanelResized();
            }
        });
    }

    private void handler(final AssistantChat chat) {
        //
        // TODO: can't we merge this in the base AssistantJeddictBrainListener ?
        //
        listener = new AssistantJeddictBrainListener(chat) {

            @Override
            public void onChatCompleted(final ChatResponse response) {
                super.onChatCompleted(response);

                final StringBuilder textResponse = new StringBuilder(response.aiMessage().text());

                final Response res = chat.response();
                res.getMessageContext().clear(); res.addContext(messageContext);
                // TODO: this shall be used for history; it won't be used for memory,
                // which is instead managed by the agents and services
                if (responseHistory.isEmpty() || !textResponse.equals(responseHistory.get(responseHistory.size() - 1))) {
                    responseHistory.add(res);
                    currentResponseIndex = responseHistory.size() - 1;
                }
                SwingUtilities.invokeLater(() -> {
                    Consumer<String> queryUpdate = (newQuery) -> {
                        handlePrompt(newQuery, false);
                    };
                    if (codeReview) {
                        List<Review> reviews = parseReviewsFromYaml(res.getBlocks().get(0).getContent());
                        String web = convertReviewsToHtml(reviews);
                        ac.setReviews(reviews);
                        res.getBlocks().clear();
                        res.getBlocks().add(new TextBlock("web", web));
                    }
                    sourceCode = EditorUtil.updateEditors(queryUpdate, ac, res, getContextFiles());

                    ac.updateButtons(currentResponseIndex > 0, currentResponseIndex < responseHistory.size() - 1);
                    ac.buttonPanelResized();
                });
            }

            @Override
            public void onToolExecuted(final ToolExecutionRequest request, final String result) {
                chat.response().addBlock(new ToolExecutionBlock(request, result));
            }
        };
    }

    private JeddictBrain newJeddictBrain(
        final JeddictBrainListener listener,
        final String modelName,
        final InteractionMode mode
    ) {
        final JeddictBrain brain = new JeddictBrain(
            modelName, pm.isStreamEnabled(),
            mode,
            (execution)-> {
                try {
                    return ac.promptConfirmation(execution).get();
                } catch (InterruptedException | ExecutionException x) {
                    return false;
                }
            },
            (mode == InteractionMode.ASK) ? null : buildToolsList(getProject(), listener, mode)
        );
        brain.addListener(listener);
        return brain;
    }

    /**
     * Returns a TestSpecialist with memory reusing a previously created agent
     * if <code>testSpecialist</code> is not null. If null, a new instance is
     * created.
     *
     * @return
     */
    private TestSpecialist testSpecialist(final JeddictBrainListener listener, final String modelName) {

        if (testSpecialist != null) {
            return testSpecialist;
        }

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, InteractionMode.ASK);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (testSpecialist = brain.pairProgrammer(PairProgrammer.Specialist.TEST));
    }

    /**
     * Returns a DBSpecialist with memory reusing a previously created agent if
     * <code>dbSpecialist</code> is not null. If null, a new instance is
     * created.
     *
     * @return
     */
    private DBSpecialist dbSpecialist(final JeddictBrainListener listener, final String modelName) {

        if (dbSpecialist != null) {
            return dbSpecialist;
        }

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, InteractionMode.ASK);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (dbSpecialist = brain.pairProgrammer(PairProgrammer.Specialist.DB));
    }

    /**
     * Returns a DiffSpecialist with memory reusing a previously created agent
     * if <code>diffSpecialist</code> is not null. If null, a new instance is
     * created.
     *
     * @return a new orexisting DiffSpecialist
     */
    private DiffSpecialist diffSpecialist(final JeddictBrainListener listener, final String modelName) {

        if (diffSpecialist != null) {
            return diffSpecialist;
        }

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, InteractionMode.ASK);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (diffSpecialist = brain.pairProgrammer(PairProgrammer.Specialist.DIFF));
    }

    /**
     * Returns a Assistant with memory reusing a previously created agent
     * if <code>assistant</code> is not null. If null, a new  instance is
     * created.
     *
     * @return a new or existing Assistant object
     */
    private Assistant assistant(final JeddictBrainListener listener, final String modelName) {

        if (assistant != null) return assistant;

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, InteractionMode.ASK);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (assistant = brain.pairProgrammer(PairProgrammer.Specialist.ASSISTANT));
    }

    /**
     * Returns a Assistant with memory reusing a previously created agent
     * if <code>assistant</code> is not null. If null, a new  instance is
     * created.
     *
     * @return a new or existing Assistant object
     */
    private Assistant projectAssistant(final JeddictBrainListener listener, final String modelName) {

        if (projectAssistant != null) return projectAssistant;

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, InteractionMode.ASK);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (projectAssistant = brain.pairProgrammer(PairProgrammer.Specialist.ASSISTANT));
    }

    /**
     * Returns an Hacker with memory reusing a previously created agent
     * if <code>hacker</code> is not null. If null, a new  instance is
     * created. An Hacker is agentic and executes tools.
     *
     * @return a new or existing Hacker object
     */
    private Hacker hacker(
        final JeddictBrainListener listener,
        final String modelName,
        final InteractionMode mode
    ) {
        if (hacker != null) return hacker;

        int memorySize = pm.getConversationContext();

        JeddictBrain brain = newJeddictBrain(listener, modelName, mode);
        brain.withMemory((memorySize < 0) ? Integer.MAX_VALUE : memorySize);

        return (hacker = brain.pairProgrammer(PairProgrammer.Specialist.HACKER));
    }

    /**
     * Buildes the tool list to be given to JeddictBrain for agentic interactions
     *
     * @param project instance of the project attached to the chat
     * @param handler a event listener to track events generated in the conversation
     *
     * @return the list of Tool objects
     */
    private List<AbstractTool> buildToolsList(
        final Project project,
        final JeddictBrainListener listener,
        final InteractionMode mode
    ) {
        if (project == null) {
            return List.of();
        }
        //
        // TODO: make this automatic with some discoverability approach (maybe
        // NB lookup registration?)
        //
        final String basedir =
            FileUtil.toPath(project.getProjectDirectory())
            .toAbsolutePath().normalize()
            .toString();

        try {
            final List<AbstractTool> toolsList = new ArrayList();

            //
            // Tools for interactive mode
            //
            if (mode == INTERACTIVE) {
                toolsList.add(new InteractiveFileEditor(basedir, ac));
            }

            //
            // Tools commmon to both AGENT and INTERACTIVE mode
            //
            toolsList.add(new FileSystemTools(basedir));
            toolsList.add(
                new ExecutionTools(
                    basedir, project.getProjectDirectory().getName(),
                    pm.getBuildCommand(project), pm.getTestCommand(project)
                )
            );
            toolsList.add(new ExplorationTools(basedir, project.getLookup()));
            toolsList.add(new GradleTools(basedir));
            toolsList.add(new MavenTools(basedir));
            toolsList.add(new RefactoringTools(basedir));

            //
            // The handler wants to know about tool execution
            //
            toolsList.forEach((tool) -> tool.addListener(listener));

            return toolsList;
        } catch (IOException x) {
            Exceptions.printStackTrace(x);
            return List.of();
        }
    }

    private void async(Supplier<String> answer, final JeddictBrainListener listener) {
        new SwingWorker<String, Object>() {
            @Override
            public String doInBackground() {
                return answer.get();
            }

            @Override
            protected void done() {
                try {
                    listener.onChatCompleted(ChatResponse.builder().aiMessage(
                        AiMessage.from(get())
                    ).build());
                } catch (InterruptedException | ExecutionException x) {
                    //
                    // TODO: better error handler
                    //
                    Exceptions.printStackTrace(x);
                }
            }
        }.execute();
    }

}
