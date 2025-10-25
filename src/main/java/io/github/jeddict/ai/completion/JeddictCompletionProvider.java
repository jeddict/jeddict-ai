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
package io.github.jeddict.ai.completion;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import io.github.jeddict.ai.agent.pair.PairProgrammer;
import io.github.jeddict.ai.lang.JeddictBrain;
import io.github.jeddict.ai.lang.Snippet;
import io.github.jeddict.ai.scanner.MyTreePathScanner;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getClassDataContent;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getFileObjectFromEditor;
import io.github.jeddict.ai.settings.AIClassContext;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.MimeUtil.JAVA_MIME;
import io.github.jeddict.ai.util.SourceUtil;
import static io.github.jeddict.ai.util.StringUtil.removeAllSpaces;
import static io.github.jeddict.ai.util.StringUtil.trimLeadingSpaces;
import static io.github.jeddict.ai.util.StringUtil.trimTrailingSpaces;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.SwingUtilities;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.api.editor.settings.AttributesUtilities;
import org.netbeans.api.java.lexer.JavaTokenId;
import static org.netbeans.api.java.lexer.JavaTokenId.BLOCK_COMMENT;
import static org.netbeans.api.java.lexer.JavaTokenId.CHAR_LITERAL;
import static org.netbeans.api.java.lexer.JavaTokenId.DOUBLE_LITERAL;
import static org.netbeans.api.java.lexer.JavaTokenId.FLOAT_LITERAL;
import static org.netbeans.api.java.lexer.JavaTokenId.FLOAT_LITERAL_INVALID;
import static org.netbeans.api.java.lexer.JavaTokenId.INT_LITERAL;
import static org.netbeans.api.java.lexer.JavaTokenId.INVALID_COMMENT_END;
import static org.netbeans.api.java.lexer.JavaTokenId.JAVADOC_COMMENT;
import static org.netbeans.api.java.lexer.JavaTokenId.LINE_COMMENT;
import static org.netbeans.api.java.lexer.JavaTokenId.LONG_LITERAL;
import static org.netbeans.api.java.lexer.JavaTokenId.STRING_LITERAL;
import org.netbeans.api.java.source.CancellableTask;
import org.netbeans.api.java.source.JavaSource;
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.java.source.WorkingCopy;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.db.sql.loader.SQLEditorSupport;
import org.netbeans.modules.editor.indent.api.Reformat;
import org.netbeans.spi.editor.completion.*;
import static org.netbeans.spi.editor.completion.CompletionProvider.COMPLETION_ALL_QUERY_TYPE;
import static org.netbeans.spi.editor.completion.CompletionProvider.COMPLETION_QUERY_TYPE;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import io.github.jeddict.ai.scanner.ProjectMetadataInfo;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import java.util.logging.Logger;
import io.github.jeddict.ai.agent.pair.CodeAdvisor;

@MimeRegistration(mimeType = "", service = CompletionProvider.class, position = 100)
public class JeddictCompletionProvider implements CompletionProvider {

    private static final Logger LOG = Logger.getLogger(JeddictCompletionProvider.class.getName());

    private final PreferencesManager pm = PreferencesManager.getInstance();

    private static final String HIGHLIGHTED_TEXT_KEY = "HIGHLIGHTED_TEXT_KEY";
    private static final String HIGHLIGHTED_TEXT_LOC_KEY = "HIGHLIGHTED_TEXT_LOC_KEY";
    private static final Object KEY_PRE_TEXT = new Object();

    public OffsetsBag getPreTextBag(Document doc, JTextComponent component) {
        OffsetsBag bag = (OffsetsBag) doc.getProperty(KEY_PRE_TEXT);

        if (bag == null) {
            doc.putProperty(KEY_PRE_TEXT, bag = new OffsetsBag(doc));
            final OffsetsBag offsetsBag = bag;

//            Object stream = doc.getProperty(Document.StreamDescriptionProperty);
//            if (stream instanceof DataObject) {
//                TimesCollector.getDefault().reportReference(((DataObject) stream).getPrimaryFile(), "ImportsHighlightsBag", "[M] Imports Highlights Bag", bag);
//            }

            component.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER
                            && (pm.isInlineHintEnabled() || pm.isInlinePromptHintEnabled())) {
                        try {
                            Snippet snippet = (Snippet) doc.getProperty(HIGHLIGHTED_TEXT_KEY);
                            Integer textLocation = (Integer) doc.getProperty(HIGHLIGHTED_TEXT_LOC_KEY);
                            if (snippet != null && textLocation != null) {
                                Document doc = component.getDocument();
                                int caretPosition = component.getCaretPosition();

                                if (textLocation.equals(caretPosition)) {
                                    doc.insertString(caretPosition, snippet.getSnippet(), null);
                                    int lineStart = Utilities.getRowStart(component, caretPosition);
                                    int lineEnd = Utilities.getRowEnd(component, caretPosition + snippet.getSnippet().length());

                                    Reformat reformat = Reformat.get(doc);
                                    reformat.lock();
                                    try {
                                        reformat.reformat(lineStart, lineEnd);
                                    } finally {
                                        reformat.unlock();
                                    }

                                    CancellableTask<WorkingCopy> task = new CancellableTask<WorkingCopy>() {
                                        @Override
                                        public void run(WorkingCopy workingCopy) throws Exception {
                                            workingCopy.toPhase(JavaSource.Phase.RESOLVED);
                                            SourceUtil.addImports(workingCopy, snippet.getImports());
                                        }

                                        @Override
                                        public void cancel() {
                                        }
                                    };
                                    JavaSource javaSource = JavaSource.forDocument(component.getDocument());
                                    if (javaSource != null) {
                                        javaSource.runModificationTask(task).commit();
                                    }
                                    e.consume();
                                }
                            }
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }
                    doc.putProperty(HIGHLIGHTED_TEXT_KEY, null);
                    offsetsBag.clear();
                }
            });
        }

        return bag;
    }

    @Override
    public CompletionTask createTask(int type, JTextComponent component) {
        if (!pm.isAiAssistantActivated()) {
            return null;
        }
        if (!pm.isSmartCodeEnabled()) {
            return null;
        }
        if ((pm.isCompletionAllQueryType() && type == COMPLETION_ALL_QUERY_TYPE)
                || (!pm.isCompletionAllQueryType() && type == COMPLETION_QUERY_TYPE)) {
            return new AsyncCompletionTask(new JeddictCompletionQuery(type, component.getSelectionStart()), component);
        }
        return null;
    }

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        if (typedText.length() == 1
                && typedText.charAt(0) == '\n') {
            if (currentTask != null && !currentTask.isDone()) {
                currentTask.cancel(true);
            }
            boolean inlineHintEnabled = pm.isInlineHintEnabled();
            boolean inlinePromptHintEnabled = pm.isInlinePromptHintEnabled();
            LineScanResult result = inlinePromptHintEnabled ? getPreviousLineUntilSlash(component) : null;
            boolean shouldExecuteQuery = (result == null && inlineHintEnabled) || (result != null && inlinePromptHintEnabled);
            if (shouldExecuteQuery) {
                currentTask = executorService.submit(() -> {
                    JeddictCompletionQuery query = new JeddictCompletionQuery(-1, component.getSelectionStart());
                    if (result != null) {
                        query.setHintContext(pm.getPrompts().get(result.getFirstWord()) + " - " + result.getSecondWord());
                    }
                    query.prepareQuery(component);
                    query.query(null, component.getDocument(), component.getSelectionStart());
                });
            }

        }
        return 0;
    }

    public LineScanResult getPreviousLineUntilSlash(JTextComponent component) {
        try {
            int selectionStart = component.getCaretPosition();

            if (selectionStart <= 1) {
                return new LineScanResult("", "", "", -1); // No valid data
            }
            int i = selectionStart - 1;
            int newlineCount = 0;

            // Scan backwards to find the start of the previous-previous line
            while (i > 0) {
                if (component.getDocument().getText(i, 1).toCharArray()[0] == '\n') {
                    newlineCount++;
                    if (newlineCount == 2) {
                        break; // Found two newlines
                    }
                }

                i--;
            }

            int lineStart = i + 1; // Start of previous line
            int searchIndex = selectionStart - 1;

            // Scan backwards in the previous line to find the last slash
            while (searchIndex >= lineStart && component.getDocument().getText(searchIndex, 1).toCharArray()[0] != '/') {
                searchIndex--;
            }

            if (searchIndex < lineStart) {
                return null; // No slash found in the previous line
            }
            int slashPosition = searchIndex;
            String extractedText = component.getDocument().getText(slashPosition, selectionStart - slashPosition).trim();

            // Split words after slash
            String[] words = extractedText.split("\\s+");

            // Check if prefsManager contains the first word after slash
            if (words.length > 0 && pm.getPrompts().get(words[0].substring(1)) != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        component.setCaretPosition(slashPosition);
                        component.getDocument().remove(slashPosition, selectionStart - slashPosition);

                        highlightLoading(component, slashPosition);
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                });
                String remainingText = "";
                if (words.length > 1) {
                    // Find the index of the first space and extract everything after it
                    int firstSpace = extractedText.indexOf(" ");
                    if (firstSpace != -1 && firstSpace + 1 < extractedText.length()) {
                        remainingText = extractedText.substring(firstSpace + 1);
                    }
                }
                LineScanResult result = new LineScanResult(
                        extractedText,
                        words.length > 0 ? words[0].substring(1) : "", // First word after slash
                        remainingText, // Entire remaining string after first word
                        slashPosition
                );
                return result;
            }
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }

    public void highlightLoading(JTextComponent component, int caretOffset) {
        try {
            Document doc = component.getDocument();
            int startOffset = component.getCaretPosition();
            OffsetsBag preTextBag = new OffsetsBag(doc);
            preTextBag.addHighlight(startOffset, startOffset + 1,
                    AttributesUtilities.createImmutable("virtual-text-prepend", "Loading..."));
            getPreTextBag(doc, component).clear();
            getPreTextBag(doc, component).setHighlights(preTextBag);
        } catch (Exception e) {
            e.printStackTrace(); // Handle the exception appropriately
        }
    }

    final class JeddictCompletionQuery extends AsyncCompletionQuery {

        private JTextComponent component;
        private final int queryType;
        private int caretOffset;
        private String hintContext;

        private JeddictCompletionQuery(int queryType, int caretOffset) {
            this.queryType = queryType;
            this.caretOffset = caretOffset;
        }

        public String getHintContext() {
            return hintContext;
        }

        public void setHintContext(String context) {
            this.hintContext = context;
        }

        @Override
        protected void preQueryUpdate(JTextComponent component) {
            int newCaretOffset = component.getSelectionStart();
            if (newCaretOffset >= caretOffset) {
                try {
                    if (isJavaIdentifierPart(component.getDocument().getText(caretOffset, newCaretOffset - caretOffset), false)) {
                        return;
                    }
                } catch (BadLocationException e) {
                }
            }
            Completion.get().hideCompletion();
        }

        @Override
        protected void prepareQuery(JTextComponent component) {
            this.component = component;
        }

        @Override
        protected void filter(CompletionResultSet resultSet) {

//            CompletionContext context = new CompletionContext(component.getDocument(),
//                    component.getCaretPosition(), queryType);
//            SpringCompletionResult springCompletionResult = completor.filter(context);
            resultSet.finish();
        }


        public String getLineText(Document doc, int caretOffset) {
            try {
                int lineStart = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(caretOffset)).getStartOffset();
                int lineEnd = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(caretOffset)).getEndOffset();
                return doc.getText(lineStart, lineEnd - lineStart);
            } catch (BadLocationException e) {
                e.printStackTrace();
                return null;
            }
        }

        public String getLineTextBeforeCaret(Document doc, int caretOffset) {
            try {
                int lineStart = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(caretOffset)).getStartOffset();
                return doc.getText(lineStart, caretOffset - lineStart);
            } catch (BadLocationException e) {
                e.printStackTrace();
                return null;
            }
        }

        public String insertPlaceholderAtCaret(Document doc, int caretOffset, String placeholder) {
            try {
                caretOffset = caretOffset > doc.getLength() ? doc.getLength() : caretOffset;
                String docText = doc.getText(0, doc.getLength());
                String updatedText = docText.substring(0, caretOffset)
                        + placeholder
                        + docText.substring(caretOffset);
                return updatedText;
            } catch (BadLocationException e) {
                e.printStackTrace();
                return null;
            }
        }

        public String getVariableNameAtCaret(Document doc, int caretOffset) {
            try {
                int lineStart = doc.getDefaultRootElement().getElement(doc.getDefaultRootElement().getElementIndex(caretOffset)).getStartOffset();
                StringBuilder variableName = new StringBuilder();
                for (int i = caretOffset - 1; i >= lineStart; i--) {
                    char ch = doc.getText(i, 1).charAt(0);
                    if (Character.isWhitespace(ch)) {
                        break;
                    }
                    variableName.insert(0, ch);
                }
                return variableName.toString();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            return null;
        }

        private JavacTask getJavacTask(Document doc) {
            try {
                String sourceCode = doc.getText(0, doc.getLength());
                JavaFileObject fileObject = new SimpleJavaFileObject(URI.create("string:///Test.java"), JavaFileObject.Kind.SOURCE) {
                    @Override
                    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                        return sourceCode;
                    }
                };
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                // Redirecting output and error streams to suppress logs
                OutputStream nullOutputStream = new OutputStream() {
                    @Override
                    public void write(int b) {
                        // No-op, discard output
                    }
                };
                PrintWriter nullWriter = new PrintWriter(nullOutputStream);
                JavacTask task = (JavacTask) compiler.getTask(nullWriter, null, nullWriter::print, null, null, Collections.singletonList(fileObject));
                return task;
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
            return null;
        }

        public TreePath findTreePathAtCaret(CompilationUnitTree compilationUnit, JavacTask task) throws IOException {
            Trees trees = Trees.instance(task);
            DocTrees docTrees = DocTrees.instance(task);  // Get the instance of DocTrees
            MyTreePathScanner treePathScanner = new MyTreePathScanner(trees, docTrees, caretOffset, compilationUnit);
            treePathScanner.scan(compilationUnit, null);
            TreePath resultPath = treePathScanner.getTargetPath();
            return resultPath;
        }

        private JeddictItem createItem(Snippet snippet, String line, String lineTextBeforeCaret, JavaToken javaToken, Tree.Kind kind, Document doc) throws BadLocationException {
            int newcaretOffset = caretOffset;
            if (javaToken.getId() == STRING_LITERAL && kind == Tree.Kind.STRING_LITERAL) {
                lineTextBeforeCaret = doc.getText(javaToken.getOffset(), newcaretOffset - javaToken.getOffset());
                if (lineTextBeforeCaret.startsWith("\"")) {
                    lineTextBeforeCaret = lineTextBeforeCaret.substring(1);
                }
            }

            String snippetWOSpace = removeAllSpaces(snippet.getSnippet());
            String tlLine = trimLeadingSpaces(line);
            String tlLineTextBeforeCaret = trimLeadingSpaces(lineTextBeforeCaret);

            // Handle line and snippet first words safely
            String firstWordLine = "";
            String lastWordLine = "";
            if (lineTextBeforeCaret != null && !lineTextBeforeCaret.trim().isEmpty()) {
                String[] textSegments = lineTextBeforeCaret.trim().split("[^.a-zA-Z0-9<]+");
                if (textSegments.length > 0) {
                    firstWordLine = textSegments[0];
                    lastWordLine = textSegments[textSegments.length - 1];
                }
            }

            String firstWordSnippet = "";
            if (snippet.getSnippet() != null && !snippet.getSnippet().trim().isEmpty()) {
                String[] textSegments = snippet.getSnippet().trim().split("[^.a-zA-Z0-9]+");
                if (textSegments.length > 0) {
                    firstWordSnippet = textSegments[0]; // Split by any non-alphanumeric character
                }
            }

            boolean midWordMatched = false;
            if (firstWordSnippet != null && !firstWordSnippet.isEmpty()
                    && firstWordLine.equalsIgnoreCase(firstWordSnippet)) {
                newcaretOffset = newcaretOffset - tlLineTextBeforeCaret.length();
            } else if (snippetWOSpace.startsWith(lastWordLine)) {
                midWordMatched = true;
                newcaretOffset = newcaretOffset - lastWordLine.length();
            } else if (snippetWOSpace.startsWith(removeAllSpaces(line))) {
                newcaretOffset = newcaretOffset - tlLine.length();
            } else if (snippetWOSpace.startsWith(removeAllSpaces(lineTextBeforeCaret))) {
                newcaretOffset = newcaretOffset - tlLineTextBeforeCaret.length();
            }
            // for annotations
            if (tlLine != null && !tlLine.isEmpty() && tlLine.charAt(0) == '@'
                    && snippetWOSpace != null && !snippetWOSpace.isEmpty() && snippetWOSpace.charAt(0) == '@') {
                newcaretOffset = newcaretOffset - tlLine.length();
            }
            int caretToEndLength = -1;
            if ((javaToken.getId() == STRING_LITERAL && kind == Tree.Kind.STRING_LITERAL) || midWordMatched) {

            } else if (newcaretOffset != caretOffset) {
                caretToEndLength = line.length() - lineTextBeforeCaret.length();
                String textAfterCaret = doc.getText(caretOffset, caretToEndLength);
                caretToEndLength = trimTrailingSpaces(textAfterCaret).length();
            }
            JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), newcaretOffset, caretToEndLength, true, false, -1);
            return var;
        }

        boolean done = false;

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            final PreferencesManager pm = PreferencesManager.getInstance();
            final boolean description = pm.isDescriptionEnabled();

            try {
                FileObject fileObject = getFileObjectFromEditor(doc);
                if (fileObject == null) {
                    return;
                }
                if (done) {
                    return;
                }
                done = true;

                final Project project = FileOwnerQuery.getOwner(fileObject);

                this.caretOffset = caretOffset;
                String mimeType = (String) doc.getProperty("mimeType");
                JavaToken javaToken = isJavaContext(component.getDocument(), caretOffset, true);
                if ((COMPLETION_QUERY_TYPE == queryType || -1 == queryType || COMPLETION_ALL_QUERY_TYPE == queryType)
                        && JAVA_MIME.equals(mimeType)
                        && javaToken.isJavaContext()) {

                    JavacTask task = getJavacTask(doc);
                    Iterable<? extends CompilationUnitTree> ast = task.parse();
                    task.analyze();
                    CompilationUnitTree compilationUnit = ast.iterator().next();

                    String line = getLineText(doc, caretOffset);
                    String lineTextBeforeCaret = getLineTextBeforeCaret(doc, caretOffset);

                    final TreePath path = findTreePathAtCaret(compilationUnit, task);

                    LOG.finest(() -> "path: " + path);

                    final Tree.Kind kind = path == null ? null : path.getLeaf().getKind();

                    LOG.finest(() -> "kind: " + kind);

                    final Tree.Kind parentKind = path != null && path.getParentPath() != null ? path.getParentPath().getLeaf().getKind() : null;

                    LOG.finest(() -> "parentKind: " + parentKind);

                    AIClassContext activeClassContext = -1 == queryType ? pm.getClassContextInlineHint() : pm.getClassContext();
                    if (kind == Tree.Kind.VARIABLE || kind == Tree.Kind.METHOD || kind == Tree.Kind.STRING_LITERAL) {
                        activeClassContext = pm.getVarContext();
                    }
                    String classDataContent = getClassDataContent(fileObject, compilationUnit, activeClassContext);
                    if (path == null || kind == Tree.Kind.ERRONEOUS) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                resultSet.addItem(createItem(snippet, line, lineTextBeforeCaret, javaToken, kind, doc));
                            }
                        }
                    } else if (kind == Tree.Kind.COMPILATION_UNIT) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                resultSet.addItem(createItem(snippet, line, lineTextBeforeCaret, javaToken, kind, doc));
                            }
                        }
                    } else if (resultSet != null &&
                            ((trimLeadingSpaces(line).length() > 0
                            && trimLeadingSpaces(line).charAt(0) == '@') || kind == Tree.Kind.ANNOTATION)) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> annotationSuggestions = newJeddictBrain()
                                .suggestAnnotations(project, classDataContent, updateddoc, line, hintContext, queryType == -1, description);
                        for (Snippet annotationSuggestion : annotationSuggestions) {
                            resultSet.addItem(createItem(annotationSuggestion, line, lineTextBeforeCaret, javaToken, kind, doc));
                        }
                    } else if (kind == Tree.Kind.MODIFIERS
                            || kind == Tree.Kind.IDENTIFIER) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                resultSet.addItem(createItem(snippet, line, lineTextBeforeCaret, javaToken, kind, doc));
                            }
                        }
                    } else if (kind == Tree.Kind.CLASS) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    } else if (kind == Tree.Kind.BLOCK) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    } else if (kind == Tree.Kind.EXPRESSION_STATEMENT) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    } else if (kind == Tree.Kind.VARIABLE && resultSet != null) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGESTION}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getGhostwriter().suggestVariableNames(classDataContent, updateddoc, line);
                        for (String snippet : sugs) {
                            JeddictItem var = new JeddictItem(null, null, snippet, "", Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);
                        }
                    } else if (kind == Tree.Kind.METHOD && resultSet != null) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGESTION}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getGhostwriter()
                                .suggestMethodNames(classDataContent, updateddoc, line);
                        for (String snippet : sugs) {
                            JeddictItem var = new JeddictItem(null, null, snippet, "", Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);
                        }
                    } else if (kind == Tree.Kind.METHOD_INVOCATION && resultSet != null) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGESTION}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getGhostwriter()
                                .suggestMethodInvocations(ProjectMetadataInfo.get(project), classDataContent, updateddoc, line);
                        for (String snippet : sugs) {
                            snippet = snippet.replace("<", "&lt;").replace(">", "&gt;");
                            JeddictItem var = new JeddictItem(null, null, snippet, "", Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);
                        }
                    } else if (kind == Tree.Kind.STRING_LITERAL && resultSet != null) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_STRING_LITERAL_LIST}");
                        List<String> sugs = getGhostwriter().suggestStringLiterals(classDataContent, updateddoc, line);
                        for (String snippet : sugs) {
                            resultSet.addItem(createItem(new Snippet(snippet), line, lineTextBeforeCaret, javaToken, kind, doc));
                        }
                    } else if (kind == Tree.Kind.PARENTHESIZED
                            && parentKind != null
                            && parentKind == Tree.Kind.IF) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_IF_CONDITIONS}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    } else if (kind == Tree.Kind.MEMBER_SELECT
                            && parentKind != null
                            && parentKind == Tree.Kind.METHOD_INVOCATION) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    } else {
                        LOG.finest(() -> "Skipped : " + kind + " " + path.getLeaf().toString());
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, classDataContent, updateddoc, line, path, hintContext, queryType == -1, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet.getSnippet(), snippet.getDescription(), snippet.getImports(), caretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    }
                } else if ((COMPLETION_QUERY_TYPE == queryType || COMPLETION_ALL_QUERY_TYPE == queryType) && JAVA_MIME.equals(mimeType)) {
                    String line = getLineText(doc, caretOffset);
                    JavacTask task = getJavacTask(doc);
                    Iterable<? extends CompilationUnitTree> ast = task.parse();
                    task.analyze();
                    CompilationUnitTree compilationUnit = ast.iterator().next();

                    TreePath path = findTreePathAtCaret(compilationUnit, task);
                    List<String> sugs;
                    if (line.trim().startsWith("//")) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_JAVA_COMMENT}");
                        sugs = newJeddictBrain()
                                .suggestJavaComment(project, "", updateddoc, line);
                        for (String varName : sugs) {
                            int newcaretOffset = caretOffset;
                            if (varName.startsWith(line.trim())) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (varName.startsWith("//")) {
                                varName = varName.substring(3);
                            }
                            JeddictItem var = new JeddictItem(null, null, varName, "", Collections.emptyList(), newcaretOffset, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_JAVADOC}");
                        sugs = newJeddictBrain()
                                .suggestJavadocOrComment(project, "", updateddoc, line);
                        for (String snippet : sugs) {
                            int newcaretOffset = caretOffset;
                            if (snippet.trim().startsWith(line.trim())) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (snippet.startsWith("* ")) {
                                snippet = snippet.substring(2);
                            }
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, new Snippet(snippet));
                                break;
                            } else {
                                JeddictItem var = new JeddictItem(null, null, snippet, "", Collections.emptyList(), newcaretOffset, true, false, -1);
                                resultSet.addItem(var);
                            }
                        }
                    }
                } else {
                    String line = getLineText(doc, caretOffset);
                    String lineTextBeforeCaret = getLineTextBeforeCaret(doc, caretOffset);
                     SQLEditorSupport sQLEditorSupport = fileObject.getLookup().lookup(SQLEditorSupport.class);
                    if (sQLEditorSupport != null) {
                        SQLCompletion sqlCompletion = new SQLCompletion(sQLEditorSupport);
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_SQL_QUERY_LIST}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestSQLQuery(sqlCompletion.getMetaData(), updateddoc, description);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = createItem(snippet, line, lineTextBeforeCaret, javaToken, null, doc);
                                resultSet.addItem(var);
                            }
                        }
                    } else {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
                        List<Snippet> sugs = newJeddictBrain()
                                .suggestNextLineCode(project, updateddoc, line, mimeType, hintContext, queryType == -1);
                        for (Snippet snippet : sugs) {
                            if (resultSet == null) {
                                highlightMultiline(component, caretOffset, snippet);
                                break;
                            } else {
                                JeddictItem var = createItem(snippet, line, lineTextBeforeCaret, javaToken, null, doc);
                                resultSet.addItem(var);
                            }
                        }
                    }
//                    } else {
//                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE}");
//                        List<Snippet> sugs = getJeddictChatModel(null)
//                                .suggestNextLineCode(null, updateddoc, line, mimeType, hintContext, queryType == -1);
//                        for (Snippet snippet : sugs) {
//                            if (resultSet == null) {
//                                highlightMultiline(component, caretOffset, snippet);
//                                break;
//                            } else {
//                                JeddictItem var = createItem(snippet, line, lineTextBeforeCaret, javaToken, null, doc);
//                                resultSet.addItem(var);
//                            }
//                        }
//                    }
                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } finally {
                resultSet.finish();
            }
        }

        public void highlightMultiline(JTextComponent component, int caretOffset, Snippet snippet) {
            try {
                Document doc = component.getDocument();
                int startOffset = component.getCaretPosition();
                OffsetsBag preTextBag = new OffsetsBag(doc);
                preTextBag.addHighlight(startOffset, startOffset + 1,
                        AttributesUtilities.createImmutable("virtual-text-prepend", snippet.getSnippet()));
                doc.putProperty(HIGHLIGHTED_TEXT_KEY, snippet);
                doc.putProperty(HIGHLIGHTED_TEXT_LOC_KEY, startOffset);

                getPreTextBag(doc, component).clear();
                getPreTextBag(doc, component).setHighlights(preTextBag);
            } catch (Exception e) {
                e.printStackTrace(); // Handle the exception appropriately
            }
        }

        private JeddictBrain newJeddictBrain() {
            return new JeddictBrain(pm.getModelName(), false, List.of());
        }

        private CodeAdvisor getGhostwriter() {
            return newJeddictBrain().pairProgrammer(PairProgrammer.Specialist.ADVISOR);
        }

        private static boolean isJavaIdentifierPart(String text, boolean allowForDor) {
            for (int i = 0; i < text.length(); i++) {
                if (!(Character.isJavaIdentifierPart(text.charAt(i)) || allowForDor && text.charAt(i) == '.')) {
                    return false;
                }
            }
            return true;
        }
    }

    public static JavaToken isJavaContext(final Document doc, final int offset, final boolean allowInStrings) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument) doc).readLock();
        }
        try {
            if (doc.getLength() == 0 && "text/x-dialog-binding".equals(doc.getProperty("mimeType"))) { //NOI18N
                InputAttributes attributes = (InputAttributes) doc.getProperty(InputAttributes.class);
                LanguagePath path = LanguagePath.get(MimeLookup.getLookup("text/x-dialog-binding").lookup(Language.class)); //NOI18N
                Document d = (Document) attributes.getValue(path, "dialogBinding.document"); //NOI18N
                if (d != null) {
                    return new JavaToken(true);//JAVA_MIME.equals(NbEditorUtilities.getMimeType(d)); //NOI18N
                }
                FileObject fo = (FileObject) attributes.getValue(path, "dialogBinding.fileObject"); //NOI18N
                return new JavaToken(JAVA_MIME.equals(fo.getMIMEType())); //NOI18N
            }
            TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(TokenHierarchy.get(doc), offset);
            if (ts == null) {
                return new JavaToken(false);
            }
            if (!ts.moveNext() && !ts.movePrevious()) {
                return new JavaToken(true, ts.token().id(), ts.offset());
            }
            if (offset == ts.offset()) {
                return new JavaToken(true, ts.token().id(), ts.offset());
            }

            switch (ts.token().id()) {
                case DOUBLE_LITERAL:
                case FLOAT_LITERAL:
                case FLOAT_LITERAL_INVALID:
                case LONG_LITERAL:
                    if (ts.token().text().charAt(0) == '.') {
                        break;
                    }
                case CHAR_LITERAL:
                case INT_LITERAL:
                case INVALID_COMMENT_END:
                case JAVADOC_COMMENT:
                case LINE_COMMENT:
                case BLOCK_COMMENT:
                    return new JavaToken(false, ts.token().id(), ts.offset());
                case STRING_LITERAL:
                    return new JavaToken(allowInStrings, ts.token().id(), ts.offset());
            }
            return new JavaToken(true, ts.token().id(), ts.offset());
        } finally {
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).readUnlock();
            }
        }
    }

}
