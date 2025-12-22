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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import io.github.jeddict.ai.models.DummyChatModel;
import io.github.jeddict.ai.scanner.MyTreePathScanner;
import io.github.jeddict.ai.test.DummyChatModelListener;
import io.github.jeddict.ai.test.TestBase;
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

/**
 *
 */
public abstract class PairProgrammerTestBase extends TestBase {

    public final static String TEXT = "use mock 'hello world.txt'";

    protected DummyChatModel model;
    protected DummyChatModelListener listener;

    @BeforeEach
    public void beforeEach() throws Exception {
        super.beforeEach();

        model = new DummyChatModel();
        listener = new DummyChatModelListener();

        model.addListener(listener);
    }

    protected void thenMessagesMatch(
        final List<ChatMessage> messages, final String system, final String user
    ) {
        boolean systemOK = false, userOK = false;
        int i = 0;

        while (i<messages.size()) {
            final ChatMessage msg = messages.get(i++);
            LOG.info(() -> String.valueOf(msg));
            if (msg.type() == ChatMessageType.SYSTEM) {
                LOG.info(() -> '\n' + String.valueOf(msg) + '\n' + String.valueOf(new SystemMessage(system)));
                systemOK = systemOK || ((SystemMessage)msg).equals(new SystemMessage(system));
            } else if (msg.type() == ChatMessageType.USER) {
                LOG.info(() -> '\n' + String.valueOf(msg) + '\n' + String.valueOf(new UserMessage(user)));
                userOK = userOK || ((UserMessage)msg).equals(new UserMessage(user));
            }
        }

        LOG.info("systemOK: " + systemOK + ", userOK: " + userOK);

        then(systemOK).isTrue();
        then(userOK).isTrue();
    }

    protected JavacTask parseSayHello() throws IOException {
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

    protected TreePath findTreePathAtCaret(CompilationUnitTree unit, JavacTask task, int offset) throws IOException {
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
