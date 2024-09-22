/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.github.jeddict.ai;

import static com.github.javaparser.utils.Utils.trimTrailingSpaces;
import io.github.jeddict.ai.scanner.MyTreePathScanner;
import com.sun.source.tree.ClassTree;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.text.JTextComponent;

import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.editor.ext.ToolTipSupport;
import org.netbeans.spi.editor.completion.*;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;

import org.openide.util.Exceptions;

import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import java.net.URI;
import java.util.Collections;
import java.io.IOException;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.source.util.JavacTask;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTrees;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getFileObjectFromEditor;
import io.github.jeddict.ai.scanner.ClassData;
import java.util.stream.Collectors;
import javax.swing.text.AbstractDocument;
import org.netbeans.api.editor.mimelookup.MimeLookup;
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
import org.netbeans.api.java.source.SourceUtils;
import org.netbeans.api.lexer.InputAttributes;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.LanguagePath;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
//import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.filesystems.FileObject;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getClassData;
import static io.github.jeddict.ai.scanner.ProjectClassScanner.getJeddictChatModel;
import io.github.jeddict.ai.settings.AIClassContext;
import io.github.jeddict.ai.settings.PreferencesManager;
import static io.github.jeddict.ai.util.StringUtil.removeAllSpaces;
import static io.github.jeddict.ai.util.StringUtil.trimLeadingSpaces;
import java.io.OutputStream;
import java.io.PrintWriter;
import org.netbeans.api.editor.completion.Completion;
import static org.netbeans.spi.editor.completion.CompletionProvider.COMPLETION_QUERY_TYPE;

/**
 *
 * @author Dusan Balek
 */
@MimeRegistration(mimeType = "text/x-java", service = CompletionProvider.class, position = 100) //NOI18N
public class JavaCompletionProvider implements CompletionProvider {

    private static final PreferencesManager prefsManager = PreferencesManager.getInstance();

    @Override
    public int getAutoQueryTypes(JTextComponent component, String typedText) {
        System.out.println("getAutoQueryTypes " + typedText);
        return 0;
    }

    @Override
    public CompletionTask createTask(int type, JTextComponent component) {
        if (!prefsManager.isAiAssistantActivated()) {
            return null;
        }
        if (!prefsManager.isSmartCodeEnabled()) {
            return null;
        }
        System.out.println("createTask " + type);
        if (type == COMPLETION_QUERY_TYPE) {

            System.out.println("createTask AsyncCompletionTask " + type);
            return new AsyncCompletionTask(new JavaCompletionQuery(type, component.getSelectionStart()), component);
        }
        return null;
    }

    static final class JavaCompletionQuery extends AsyncCompletionQuery {

        static final AtomicReference<CompletionDocumentation> outerDocumentation = new AtomicReference<>();
        private JTextComponent component;
        private final int queryType;
        private int caretOffset;

        private JavaCompletionQuery(int queryType, int caretOffset) {
            this.queryType = queryType;
            this.caretOffset = caretOffset;
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

        public Set<String> getReferencedClasses(CompilationUnitTree compilationUnit) throws IOException {
            Set<String> referencedClasses = findReferencedClasses(compilationUnit);
            for (String className : referencedClasses) {
//                String classSource = loadClassSource(className);
//                System.out.println("Referenced Class: " + className + "\nSource:\n" + classSource);
            }

            return referencedClasses;
        }

        private Set<String> findReferencedClasses(CompilationUnitTree compilationUnit) {
            Set<String> referencedClasses = new HashSet<>();

            for (Tree tree : compilationUnit.getTypeDecls()) {
                if (tree instanceof ClassTree) {
                    ClassTree classTree = (ClassTree) tree;
                    for (Tree member : classTree.getMembers()) {
                        if (member instanceof VariableTree) {
                            VariableTree variable = (VariableTree) member;
                            referencedClasses.add(variable.getType().toString());
                        } else if (member instanceof MethodTree) {
                            MethodTree method = (MethodTree) member;
                            if (method.getReturnType() != null) {
                                referencedClasses.add(method.getReturnType().toString());
                            }
                        }
                    }
                }
            }

            return referencedClasses;
        }

        boolean done = false;

        @Override
        protected void query(CompletionResultSet resultSet, Document doc, int caretOffset) {
            try {
                if (done) {
                    return;
                }
                done = true;
                this.caretOffset = caretOffset;
                if (COMPLETION_QUERY_TYPE == queryType && isJavaContext(component.getDocument(), caretOffset, true)) {
                    System.out.println("query " + queryType);
                    JavacTask task = getJavacTask(doc);
                    Iterable<? extends CompilationUnitTree> ast = task.parse();
                    task.analyze();
                    CompilationUnitTree compilationUnit = ast.iterator().next();

                    String line = getLineText(doc, caretOffset);
                    String lineTextBeforeCaret = getLineTextBeforeCaret(doc, caretOffset);
                    TreePath path = findTreePathAtCaret(compilationUnit, task);
                    FileObject fileObject = getFileObjectFromEditor(doc);
                    AIClassContext activeClassContext = prefsManager.getClassContext();
                    Set<String> findReferencedClasses = findReferencedClasses(compilationUnit);
                    List<ClassData> classDatas = getClassData(fileObject, findReferencedClasses, activeClassContext);
                    String classDataContent = classDatas.stream()
                            //                                .peek(s -> System.out.println("Sending: " + s.getClassSignature()))
                            .map(cd -> cd.toString())
                            .collect(Collectors.joining("\n--------------------\n"));
                    if (path != null) {
                        System.out.println("path.getLeaf().getKind() " + path.getLeaf().getKind());
                        if (path.getParentPath() != null) {
                            System.out.println("path.getParentPath().getLeaf().getKind() " + path.getParentPath().getLeaf().getKind());
                        }
                    }

                    if (path == null
                            || path.getLeaf().getKind() == Tree.Kind.ERRONEOUS) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<Snippet> sugs = getJeddictChatModel(fileObject).suggestNextLineCode(classDataContent, updateddoc, line, null);
                        for (Snippet varName : sugs) {
                            int newcaretOffset = caretOffset;
                            String snippetWOSpace = removeAllSpaces(varName.getSnippet());
                            if (snippetWOSpace.startsWith(removeAllSpaces(line))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (snippetWOSpace.startsWith(removeAllSpaces(lineTextBeforeCaret))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(lineTextBeforeCaret).length();
                            }
                            int caretToEndLength = -1;
                            if (newcaretOffset != caretOffset) {
                                caretToEndLength = line.length() - lineTextBeforeCaret.length();
                                String textAfterCaret = doc.getText(caretOffset, caretToEndLength);
                                caretToEndLength = trimTrailingSpaces(textAfterCaret).length();
                            }

                            JeddictItem var = new JeddictItem(null, null, varName.getSnippet(), varName.getImports(), newcaretOffset, caretToEndLength, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.COMPILATION_UNIT) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<Snippet> sugs = getJeddictChatModel(fileObject).suggestNextLineCode(classDataContent, updateddoc, line, path);
                        for (Snippet varName : sugs) {
                            int newcaretOffset = caretOffset;
                            String snippetWOSpace = removeAllSpaces(varName.getSnippet());
                            if (snippetWOSpace.startsWith(removeAllSpaces(line))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (snippetWOSpace.startsWith(removeAllSpaces(lineTextBeforeCaret))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(lineTextBeforeCaret).length();
                            }
                            int caretToEndLength = -1;
                            if (newcaretOffset != caretOffset) {
                                caretToEndLength = line.length() - lineTextBeforeCaret.length();
                                String textAfterCaret = doc.getText(caretOffset, caretToEndLength);
                                caretToEndLength = trimTrailingSpaces(textAfterCaret).length();
                            }

                            JeddictItem var = new JeddictItem(null, null, varName.getSnippet(), varName.getImports(), newcaretOffset, caretToEndLength, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.MODIFIERS
                            || path.getLeaf().getKind() == Tree.Kind.IDENTIFIER) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<Snippet> sugs = getJeddictChatModel(fileObject).suggestNextLineCode(classDataContent, updateddoc, line, path);
                        for (Snippet varName : sugs) {

                            int newcaretOffset = caretOffset;
                            String snippetWOSpace = removeAllSpaces(varName.getSnippet());
                            if (snippetWOSpace.startsWith(removeAllSpaces(line))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (snippetWOSpace.startsWith(removeAllSpaces(lineTextBeforeCaret))) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(lineTextBeforeCaret).length();
                            }
                            int caretToEndLength = -1;
                            if (newcaretOffset != caretOffset) {
                                caretToEndLength = line.length() - lineTextBeforeCaret.length();
                                String textAfterCaret = doc.getText(caretOffset, caretToEndLength);
                                caretToEndLength = trimTrailingSpaces(textAfterCaret).length();
                            }
                            JeddictItem var = new JeddictItem(null, null, varName.getSnippet(), varName.getImports(), newcaretOffset, caretToEndLength, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.ANNOTATION) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getJeddictChatModel(fileObject).suggestAnnotations(classDataContent, updateddoc, line);
                        for (String varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), caretOffset - 1, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.CLASS) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<Snippet> sugs = getJeddictChatModel(fileObject).suggestNextLineCode(classDataContent, updateddoc, line, path);
                        for (Snippet varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName.getSnippet(), varName.getImports(), caretOffset, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.BLOCK) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_CODE_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<Snippet> sugs = getJeddictChatModel(fileObject).suggestNextLineCode(classDataContent, updateddoc, line, path);
                        for (Snippet varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName.getSnippet(), varName.getImports(), caretOffset, true, false, -1);

                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.VARIABLE) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_VAR_NAMES_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getJeddictChatModel(fileObject).suggestVariableNames(classDataContent, updateddoc, line);
                        for (String varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.METHOD) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_METHOD_NAMES_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getJeddictChatModel(fileObject).suggestMethodNames(classDataContent, updateddoc, line);
                        for (String varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.METHOD_INVOCATION) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_METHOD_INVOCATION}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getJeddictChatModel(fileObject).suggestMethodInvocations(classDataContent, updateddoc, line);
                        for (String varName : sugs) {
                            varName = varName.replace("<", "&lt;").replace(">", "&gt;");
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), caretOffset - currentVarName.length(), true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else if (path.getLeaf().getKind() == Tree.Kind.STRING_LITERAL) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_STRING_LITERAL_LIST}");
                        String currentVarName = getVariableNameAtCaret(doc, caretOffset);
                        List<String> sugs = getJeddictChatModel(fileObject).suggestStringLiterals(classDataContent, updateddoc, line);
                        for (String varName : sugs) {
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), caretOffset, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else {
                        System.out.println(path.getLeaf().getKind() + " " + path.getLeaf().toString());
                    }
                } else {
                    System.out.println("else    fddddddddddd");
                    String line = getLineText(doc, caretOffset);
                    JavacTask task = getJavacTask(doc);
                    Iterable<? extends CompilationUnitTree> ast = task.parse();
                    task.analyze();
                    CompilationUnitTree compilationUnit = ast.iterator().next();

                    FileObject fileObject = getFileObjectFromEditor(doc);
                    TreePath path = findTreePathAtCaret(compilationUnit, task);
                    if (path != null) {
                        System.out.println(" path.getLeaf().getKind() " + path.getLeaf().getKind());
                    }
                    List<String> sugs;
                    if (line.trim().startsWith("//")) {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_JAVA_COMMENT}");
                        sugs = getJeddictChatModel(fileObject).suggestJavaComment("", updateddoc, line);
                        for (String varName : sugs) {
                            int newcaretOffset = caretOffset;
                            if (varName.startsWith(line.trim())) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (varName.startsWith("//")) {
                                varName = varName.substring(3);
                            }
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), newcaretOffset, true, false, -1);
                            resultSet.addItem(var);

                        }
                    } else {
                        String updateddoc = insertPlaceholderAtCaret(doc, caretOffset, "${SUGGEST_JAVADOC}");
                        sugs = getJeddictChatModel(fileObject).suggestJavadocOrComment("", updateddoc, line);
                        for (String varName : sugs) {
                            int newcaretOffset = caretOffset;
                            if (varName.trim().startsWith(line.trim())) {
                                newcaretOffset = newcaretOffset - trimLeadingSpaces(line).length();
                            } else if (varName.startsWith("* ")) {
                                varName = varName.substring(2);
                            }
                            JeddictItem var = new JeddictItem(null, null, varName, Collections.emptyList(), newcaretOffset, true, false, -1);
                            resultSet.addItem(var);

                        }
                    }

                }
            } catch (Exception e) {
                Exceptions.printStackTrace(e);
            } finally {
                resultSet.finish();
            }
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

    public static boolean isJavaContext(final Document doc, final int offset, final boolean allowInStrings) {
        if (doc instanceof AbstractDocument) {
            ((AbstractDocument) doc).readLock();
        }
        try {
            if (doc.getLength() == 0 && "text/x-dialog-binding".equals(doc.getProperty("mimeType"))) { //NOI18N
                InputAttributes attributes = (InputAttributes) doc.getProperty(InputAttributes.class);
                LanguagePath path = LanguagePath.get(MimeLookup.getLookup("text/x-dialog-binding").lookup(Language.class)); //NOI18N
                Document d = (Document) attributes.getValue(path, "dialogBinding.document"); //NOI18N
                if (d != null) {
                    return true;//"text/x-java".equals(NbEditorUtilities.getMimeType(d)); //NOI18N
                }
                FileObject fo = (FileObject) attributes.getValue(path, "dialogBinding.fileObject"); //NOI18N
                return "text/x-java".equals(fo.getMIMEType()); //NOI18N
            }
            TokenSequence<JavaTokenId> ts = SourceUtils.getJavaTokenSequence(TokenHierarchy.get(doc), offset);
            if (ts == null) {
                return false;
            }
            if (!ts.moveNext() && !ts.movePrevious()) {
                return true;
            }
            if (offset == ts.offset()) {
                return true;
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
                    return false;
                case STRING_LITERAL:
                    return allowInStrings;
            }
            return true;
        } finally {
            if (doc instanceof AbstractDocument) {
                ((AbstractDocument) doc).readUnlock();
            }
        }
    }
}