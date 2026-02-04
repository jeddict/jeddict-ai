package io.github.jeddict.ai.agent;

import dev.langchain4j.exception.ToolExecutionException;
import io.github.jeddict.ai.test.TestBase;
import io.github.jeddict.ai.test.DummyTool;
import io.github.jeddict.ai.lang.DummyJeddictBrainListener;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.api.BeforeEach;

public class AbstractToolTest extends TestBase {

    public final static String TESTFILE = "folder/testfile.txt";

    @Test
    public void constructor_sets_instance_variables() throws IOException {
        final DummyTool tool = new DummyTool(projectDir);

        then(tool.basedir).isSameAs(projectDir);
        then(tool.basepath).isEqualTo(projectPath);
    }

    @Test
    public void basedir_can_not_be_null_or_blank() throws IOException {
        for (String S: new String[] {null, "  ", "", "\n", " \t"})
       thenThrownBy(() -> { new DummyTool(null); })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("basedir can not be null or blank");
    }

    @Test
    public void fullPath_returns_the_full_path_of_given_relative_path() throws Exception {
        final DummyTool tool = new DummyTool(projectDir);

        then(tool.fullPath("relative")).isEqualTo(projectPath.resolve("relative"));
    }

    @Test
    public void set_and_get_humanInTheMiddle() throws IOException  {
        final DummyTool tool = new DummyTool();

        final UnaryOperator<String> hitm = (s) -> {return s;};

        tool.withHumanInTheMiddle(hitm);
        then(tool.humanInTheMiddle()).contains(hitm);

        tool.withHumanInTheMiddle(null);
        then(tool.humanInTheMiddle()).isEmpty();
    }

    @Test
    public void fires_onProgress_events() throws IOException {
        //
        // When a tool send an onProgress event, we want to start a new thread.
        // This is because when stremaing, content does not necessarily ends with 
        // a \n and the output of the tool may not go to a new line.
        //
        
        // given
        final DummyTool tool = new DummyTool(projectDir);
        final DummyJeddictBrainListener listener = new DummyJeddictBrainListener();
        tool.addListener(listener);

        // when
        tool.progress("a message");

        // then
        then(listener.collector).hasSize(1);
        then(listener.collector.get(0)).asString().isEqualTo("(onProgress,\na message)");
        
        
    }

    @Test
    public void addListener_does_not_accept_null() throws IOException {
        // given
        final DummyTool tool = new DummyTool(projectDir);

        // when & then
        thenThrownBy(() -> tool.addListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("listener can not be null");
    }

    @Test
    public void removeListener_does_not_accept_null() throws IOException {
        // given
        final DummyTool tool = new DummyTool(projectDir);

        // when & then
        thenThrownBy(() -> tool.removeListener(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("listener can not be null");
    }

    @Test
    public void progress_also_logs_the_message() throws IOException {
        // given
        final DummyTool tool = new DummyTool(projectDir);

        // when
        tool.progress("a message");

        // then
        then(logHandler.getMessages()).contains("a message");
    }

    @Test
    public void checkPath_completes_with_child() throws IOException {
        final DummyTool tool = new DummyTool(projectDir);
        tool.checkPath(projectPath.toString());
        tool.checkPath(TESTFILE);
        tool.checkPath("folder/");
        tool.checkPath(projectPath.resolve(TESTFILE).toAbsolutePath().toString());
    }

    @Test
    public void checkPath_raises_exception_with_not_child_path() throws IOException {
        final DummyTool tool = new DummyTool(projectDir);

        thenThrownBy(() -> tool.checkPath("/nowhere"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageStartingWith("trying to reach a file");

        thenThrownBy(() -> tool.checkPath(projectDir + "/../outside"))
            .isInstanceOf(ToolExecutionException.class)
            .hasMessageStartingWith("trying to reach a file");;
    }

}
