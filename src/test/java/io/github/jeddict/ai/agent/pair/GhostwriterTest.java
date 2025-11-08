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
package io.github.jeddict.ai.agent.pair;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import static io.github.jeddict.ai.agent.pair.Ghostwriter.LANGUAGE_JAVA;
import io.github.jeddict.ai.lang.Snippet;
import io.github.jeddict.ai.scanner.MyTreePathScanner;
import static io.github.jeddict.ai.util.MimeUtil.MIME_JS;
import static io.github.jeddict.ai.util.MimeUtil.MIME_TYPE_DESCRIPTIONS;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class GhostwriterTest extends PairProgrammerTestBase {

    private static final String LINE = "String name=\"this is the line of code\";";
    private static final String CODE1 = "use mock 'suggest code.txt'";
    private static final String CODE2 = "use mock 'suggest comments.txt'";
    private static final String CLASSES = "classes data";
    private static final String PROJECT = "JDK 17";
    private static final String HINT = "this is an hint";

    private Ghostwriter pair;

    /**
     * Represents different prompt types for the Ghostwriter agent. It associates
     * a Tree.Kind type to the offset in the source code of io.github.jeddict.ai.test.SayHeelo
     */
    enum SayHelloPrompt {
        PROMPT_DEFAULT(-1, Ghostwriter.USER_MESSAGE_DEFAULT),                    // simulate null tree
        PROMPT_COMPILATION_UNIT(151, Ghostwriter.USER_MESSAGE_COMPILATION_UNIT),  // at the end of the package declaration
        PROMPT_CLASS_MODIFIERS(240, Ghostwriter.USER_MESSAGE_CLASS_MODIFIERS),   // at the beginning of the class declaration
        PROMPT_CLASS_NAME(253, Ghostwriter.USER_MESSAGE_CLASS_NAME),             // at the beginning of the class name
        PROMPT_METHOD_MODIFIERS(268, Ghostwriter.USER_MESSAGE_METHOD_MODIFIERS), // at the beginning of the sayHello() method declaration
        PROMPT_LINES(302, Ghostwriter.USER_MESSAGE_LINES),                       // at the very beginning of the sayHello() body, just after '{'
        PROMPT_IF_CONDITION(327, Ghostwriter.USER_MESSAGE_IF_CONDITION),         // at the end of an if condition, just before the ')'
        PROMPT_INNER_CLASS(488, Ghostwriter.USER_MESSAGE_INNER_CLASS);           // at the beginning of the name of the inner class (just before InnerClass)

        final int offset;
        final String prompt;

        SayHelloPrompt(final int offset, final String prompt) {
            this.offset = offset;
            this.prompt = prompt;
        }
    }

    @BeforeEach
    @Override
    public void beforeEach() throws Exception {
        super.beforeEach();

        pair = AgenticServices.agentBuilder(Ghostwriter.class)
            .chatModel(model)
            .build();

    }

    @Test
    public void suggestNextLineCode_with_hint_returns_AI_provided_response() {
        final String expectedSystem = Ghostwriter.SYSTEM_MESSAGE
            .replace("{{format}}", Ghostwriter.OUTPUT_JSON_OBJECT);
        final String expectedUser = Ghostwriter.USER_MESSAGE
                .replace("{{message}}", "")
                .replace("{{language}}", "Java")
                .replace("{{classes}}", CLASSES)
                .replace("{{code}}", CODE1)
                .replace("{{line}}", LINE)
                .replace("{{project}}", PROJECT)
                .replace("{{hint}}", HINT);

        for (boolean description: new boolean[] { true, false} ) {
            final List<Snippet> snippets =
                pair.suggestNextLineCode(CLASSES, LANGUAGE_JAVA, CODE1, LINE, PROJECT, HINT, null, description);

            final ChatModelRequestContext request = listener.lastRequestContext.get();
            thenMessagesMatch(
                request.chatRequest().messages(), expectedSystem, expectedUser
            );

            then(snippets).hasSize(1);
            then(snippets.get(0).getImports()).containsExactlyInAnyOrder(
                "java.io.File",
                "java.util.List",
                "io.github.jeddict.ai.test.SayHello"
            );
            then(snippets.get(0).getSnippet()).isEqualTo(
                "System.out.println(\"Hello World!\");"
            );
        }
    }

    @Test
    public void suggestNextLineCode_without_hint_without_tree_returns_AI_provided_response() {
        boolean withDescription = true;
        for(String output: new String[] { Ghostwriter.OUTPUT_SNIPPET_JSON_ARRAY_WITH_DESCRIPTION, Ghostwriter.OUTPUT_SNIPPET_JSON_ARRAY}) {
            LOG.info(() -> "output: " + output);

            final String expectedSystem = Ghostwriter.SYSTEM_MESSAGE
                .replace("{{format}}", output);
            final String expectedUser = Ghostwriter.USER_MESSAGE
                    .replace("{{message}}", pair.userMessage(null))
                    .replace("{{language}}", "Java")
                    .replace("{{classes}}", CLASSES)
                    .replace("{{code}}", CODE1)
                    .replace("{{line}}", LINE)
                    .replace("{{project}}", PROJECT)
                    .replace("{{hint}}", "");

            final List<Snippet> snippets =
                pair.suggestNextLineCode(CLASSES, LANGUAGE_JAVA, CODE1, LINE, PROJECT, null, null, withDescription);

            final ChatModelRequestContext request = listener.lastRequestContext.get();
            thenMessagesMatch(
                request.chatRequest().messages(), expectedSystem, expectedUser
            );

            then(snippets).hasSize(1);
            then(snippets.get(0).getImports()).containsExactlyInAnyOrder(
                "java.io.File",
                "java.util.List",
                "io.github.jeddict.ai.test.SayHello"
            );
            then(snippets.get(0).getSnippet()).isEqualTo(
                "System.out.println(\"Hello World!\");"
            );

            withDescription = false;
        }
    }

    @Test
    public void suggestNextLineCode_with_language_returns_AI_provided_response() throws Exception {
        final String expectedSystem = Ghostwriter.SYSTEM_MESSAGE
            .replace("{{format}}", Ghostwriter.OUTPUT_JSON_OBJECT);
        final String expectedUser = Ghostwriter.USER_MESSAGE
            .replace("{{message}}", "")
            .replace("{{language}}", MIME_TYPE_DESCRIPTIONS.get(MIME_JS))
            .replace("{{classes}}", CLASSES)
            .replace("{{code}}", CODE1)
            .replace("{{line}}", LINE)
            .replace("{{project}}", PROJECT)
            .replace("{{hint}}", HINT);

        final List<Snippet> snippets =
                pair.suggestNextLineCode(CLASSES, MIME_TYPE_DESCRIPTIONS.get(MIME_JS), CODE1, LINE, PROJECT, HINT, null, false);

        final ChatModelRequestContext request = listener.lastRequestContext.get();
        thenMessagesMatch(
            request.chatRequest().messages(), expectedSystem, expectedUser
        );

        then(snippets).hasSize(1);
        then(snippets.get(0).getImports()).containsExactlyInAnyOrder(
            "java.io.File",
            "java.util.List",
            "io.github.jeddict.ai.test.SayHello"
        );
        then(snippets.get(0).getSnippet()).isEqualTo(
            "System.out.println(\"Hello World!\");"
        );

    }

    @Test
    public void suggestNextLineCode_returns_AI_provided_response() throws Exception {
        final JavacTask task = parseSayHello();
        final Iterable<? extends CompilationUnitTree> ast = task.parse();
        task.analyze();
        final CompilationUnitTree unit = ast.iterator().next();

        for (SayHelloPrompt prompt: SayHelloPrompt.values()) {
            final TreePath tree = findTreePathAtCaret(unit, task, prompt.offset);
            LOG.info(() -> "tree: " + tree + ", kind: " + ((tree != null) ? String.valueOf(tree.getLeaf().getKind()) : "-"));

            boolean withDescription = true;
            for(String output: new String[] { Ghostwriter.OUTPUT_SNIPPET_JSON_ARRAY_WITH_DESCRIPTION, Ghostwriter.OUTPUT_SNIPPET_JSON_ARRAY }) {
                final String expectedSystem = Ghostwriter.SYSTEM_MESSAGE
                    .replace("{{format}}", output);
                final String expectedUser = Ghostwriter.USER_MESSAGE
                        .replace("{{message}}", prompt.prompt)
                        .replace("{{language}}", "Java")
                        .replace("{{classes}}", CLASSES)
                        .replace("{{code}}", CODE1)
                        .replace("{{line}}", LINE)
                        .replace("{{project}}", PROJECT)
                        .replace("{{hint}}", "");

                final List<Snippet> snippets =
                    pair.suggestNextLineCode(CLASSES, LANGUAGE_JAVA, CODE1, LINE, PROJECT, null, tree, withDescription);

                final ChatModelRequestContext request = listener.lastRequestContext.get();
                thenMessagesMatch(
                    request.chatRequest().messages(), expectedSystem, expectedUser
                );

                then(snippets).hasSize(1);
                then(snippets.get(0).getImports()).containsExactlyInAnyOrder(
                    "java.io.File",
                    "java.util.List",
                    "io.github.jeddict.ai.test.SayHello"
                );
                then(snippets.get(0).getSnippet()).isEqualTo(
                    "System.out.println(\"Hello World!\");"
                );

                withDescription = false;
            }
        }
    }

    @Test
    public void suggestJavaComment_returns_AI_provided_response() throws Exception {
        final String expectedSystem = Ghostwriter.SYSTEM_MESSAGE
                .replace("{{format}}", Ghostwriter.OUTPUT_STRING_JSON_ARRAY);
        final String expectedUser = Ghostwriter.USER_MESSAGE
                .replace("{{message}}", Ghostwriter.USER_MESSAGE_COMMENT)
                .replace("{{language}}", "Java")
                .replace("{{classes}}", CLASSES)
                .replace("{{code}}", CODE2)
                .replace("{{line}}", LINE)
                .replace("{{project}}", PROJECT)
                .replace("{{hint}}", "");

        final List<String> comments =
                pair.suggestJavaComment(CLASSES, CODE2, LINE, PROJECT);

            final ChatModelRequestContext request = listener.lastRequestContext.get();
            thenMessagesMatch(
                request.chatRequest().messages(), expectedSystem, expectedUser
            );

            then(comments).containsExactlyInAnyOrder(
                "comment one","comment two", "comment three"
            );
    }

    // --------------------------------------------------------- private methods

    private JavacTask parseSayHello() throws IOException {
        final File sayHelloFile = new File("src/test/java/io/github/jeddict/ai/test/SayHello.java");

        JavaFileObject fileObject = new SimpleJavaFileObject(
            sayHelloFile.toURI(), JavaFileObject.Kind.SOURCE
        ) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                try {
                    return FileUtils.readFileToString(
                        sayHelloFile.getAbsoluteFile(), "UTF8"
                    ).replaceAll("\r\n", "\n");
                } catch (IOException x) {
                    x.printStackTrace();
                    return null;
                }
            }
        };
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // Redirecting output and error streams to suppress logs
        PrintWriter nullWriter = new PrintWriter(new OutputStream() {
            @Override
            public void write(int b) {
                // No-op, discard output
            }
        });
        JavacTask task = (JavacTask) compiler.getTask(nullWriter, null, nullWriter::print, null, null, Collections.singletonList(fileObject));
        return task;
    }

    private  TreePath findTreePathAtCaret(CompilationUnitTree unit, JavacTask task, int offset) throws IOException {
        if (offset < 0) {
            return null;
        }

        Trees trees = Trees.instance(task);
        DocTrees docTrees = DocTrees.instance(task);  // Get the instance of DocTrees
        MyTreePathScanner treePathScanner = new MyTreePathScanner(trees, docTrees, offset, unit);
        treePathScanner.scan(unit, null);
        TreePath resultPath = treePathScanner.getTargetPath();

        return resultPath;
    }

}
