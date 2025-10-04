package io.github.jeddict.ai.response;

import io.github.jeddict.ai.test.BaseTest;
import java.io.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openide.filesystems.FileUtil;


//
// TODO: remove setBlocks
// TODO: make ir a record
//
public class ResponseTest extends BaseTest {

    private final String query = "What is Java?";
    private Set<FileObject> messageContext;

    @BeforeEach
    void before() throws Exception {
        super.beforeEach();
        messageContext = new HashSet<>();
        messageContext.add(FileUtil.toFileObject(new File(projectDir, "folder/testfile.txt")));
    }

    @Test
    void response_initializes_query_and_message_context() {
        final String responseText = "Java is a programming language.";

        final Response response = new Response(query, responseText, messageContext);

        then(response.getQuery()).isEqualTo(query);
        then(response.getMessageContext()).isSameAs(messageContext);
        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo(responseText);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void constructor_throws_exception_for_invalid_query(String invalidQuery) {
        thenThrownBy(() -> new Response(invalidQuery, "valid response", messageContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("query can not be null or empty");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    void null_or_empty_response_produce_no_blocks(final String invalidResponse) {
        final Response response = new Response(query, invalidResponse, messageContext);

        then(response.getBlocks()).isEmpty();
    }

    @Test
    void constructor_throws_exception_for_null_message_context() {
        thenThrownBy(() -> new Response(query, "valid response", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("messageContext can not be null");
    }

    @Test
    void get_query_returns_correct_query() {
        then(
            new Response(query, "Test response.", messageContext).getQuery()
        ).isEqualTo(query);
    }

    @Test
    void get_blocks_returns_correct_blocks() {
        then(
            new Response(query, "Some text.", messageContext).getBlocks()
        ).usingRecursiveComparison().isEqualTo(List.of(new Block("text", "Some text.")));
    }

    /**
    @Test
    void set_blocks_updates_blocks() {
        String responseText = "Initial text.";
        Response response = new Response(query, responseText, messageContext);
        List<Block> newBlocks = Arrays.asList(new Block("text", "New content."));
        response.setBlocks(newBlocks);
        then(response.getBlocks())
                .usingRecursiveComparison()
                .isEqualTo(newBlocks);
    }

    @Test
    void get_message_context_returns_correct_context() {
        String responseText = "Context test.";
        Response response = new Response(query, responseText, messageContext);
        then(response.getMessageContext()).isEqualTo(messageContext);
    }
    */

    @Test
    void parse_markdown_with_single_text_block() {
        final String TEXT = "This is a plain text paragraph.";

        final Response response = new Response(query, TEXT, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo(TEXT);
    }

    @Test
    void parse_markdown_with_single_code_block() {
        final String MARKDOWN =
            "```\n" +
            "System.out.println(\"Hello\");\n" +
            "```";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("code");
        then(response.getBlocks().get(0).getContent()).isEqualTo("System.out.println(\"Hello\");\n");
    }

    @Test
    void parse_markdown_with_code_block_and_language() {
        final String MARKDOWN =
            "```java\n" +
            "    public class MyClass {}\n" +
            "```";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("java");
        then(response.getBlocks().get(0).getContent()).isEqualTo("    public class MyClass {}\n");
    }

    @Test
    void parse_markdown_with_mixed_text_and_code() {
        final String MARKDOWN =
            "First paragraph.\n" +
            "```java\n" +
            "    int x = 10;\n" +
            "```\n" +
            "Second paragraph.";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(3);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo("First paragraph.");
        then(response.getBlocks().get(1).getType()).isEqualTo("java");
        then(response.getBlocks().get(1).getContent()).isEqualTo("    int x = 10;\n");
        then(response.getBlocks().get(2).getType()).isEqualTo("text");
        then(response.getBlocks().get(2).getContent()).isEqualTo("Second paragraph.");
    }

    @Test
    void parse_markdown_with_multiple_code_blocks() {
        final String MARKDOWN =
            "```java\n" +
            "// Java code\n" +
            "```\n" +
            "Some text.\n" +
            "```python\n" +
            "# Python code\n" +
            "```\n";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(3);
        then(response.getBlocks().get(0).getType()).isEqualTo("java");
        then(response.getBlocks().get(0).getContent()).isEqualTo("// Java code\n");
        then(response.getBlocks().get(1).getType()).isEqualTo("text");
        then(response.getBlocks().get(1).getContent()).isEqualTo("Some text.");
        then(response.getBlocks().get(2).getType()).isEqualTo("python");
        then(response.getBlocks().get(2).getContent()).isEqualTo("# Python code\n");
    }

    @Test
    void parse_markdown_with_code_block_at_start_and_end() {
        final String MARKDOWN =
            "```bash\n" +
            "echo hello\n" +
            "```\n" +
            "Middle text.\n" +
            "```\n" +
            "final code\n" +
            "```\n";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(3);
        then(response.getBlocks().get(0).getType()).isEqualTo("bash");
        then(response.getBlocks().get(0).getContent()).isEqualTo("echo hello\n");
        then(response.getBlocks().get(1).getType()).isEqualTo("text");
        then(response.getBlocks().get(1).getContent()).isEqualTo("Middle text.");
        then(response.getBlocks().get(2).getType()).isEqualTo("code");
        then(response.getBlocks().get(2).getContent()).isEqualTo("final code\n");
    }

    @Test
    void parse_markdown_with_empty_lines_in_code_block() {
        final String MARKDOWN =
            "```\n" +
            "Line 1\n" +
            "\n" +
            "Line 3\n" +
            "```";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("code");
        then(response.getBlocks().get(0).getContent()).isEqualTo("Line 1\n\nLine 3\n");
    }

    @Test
    void parse_markdown_with_different_fence_lengths() {
        final String MARKDOWN =
            "````java\n" +
            "// Four backticks\n" +
            "````";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("java");
        then(response.getBlocks().get(0).getContent()).isEqualTo("// Four backticks\n");
    }

    @Test
    void to_string_reconstructs_plain_text() {
        final String MARKDOWN = "Just some plain text.";

        then(
            new Response(query, MARKDOWN, messageContext).toString()
        ).isEqualTo("Just some plain text.");
    }

    @Test
    void to_string_reconstructs_code_block() {
        final String MARKDOWN =
            "```java\n" +
            "public void method() {}\n" +
            "```";

        then(
            new Response(query, MARKDOWN, messageContext).toString()
        ).isEqualTo(MARKDOWN);
    }

    @Test
    void to_string_reconstructs_mixed_content() {
        final String MARKDOWN =
            "Hello.\n" +
            "```bash\n" +
            "echo 'world'\n" +
            "```\n" +
            "Goodbye.";

        then(
            new Response(query, MARKDOWN, messageContext).toString()
        ).isEqualTo(MARKDOWN);
    }

    @Test
    void to_string_handles_empty_blocks() {
        final Response response = new Response(query, "", messageContext);
        response.setBlocks(Collections.emptyList());
        then(response.toString()).isEmpty();
    }

    @Test
    void parse_markdown_with_code_block_containing_fence() {
        final String MARKDOWN =
            "```\n" +
            "Code with ``` inside\n" +
            "```\n";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(1);
        then(response.getBlocks().get(0).getType()).isEqualTo("code");
        then(response.getBlocks().get(0).getContent()).isEqualTo("Code with ``` inside\n");
    }

    @Test
    void parse_markdown_with_unclosed_code_block() {
        final String MARKDOWN =
            "Text before.\n" +
            "```java\n" +
            "Unclosed code block";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(2);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo("Text before.");
        then(response.getBlocks().get(1).getType()).isEqualTo("java");
        then(response.getBlocks().get(1).getContent()).isEqualTo("Unclosed code block");
    }

    @Test
    void parse_markdown_with_multiple_empty_lines_between_blocks() {
        final String MARKDOWN =
            "  Text 1\n" +
            "\n" +
            "\n" +
            "```html\n" +
            "<html>html</html>\n" +
            "```\n" +
            "\n" +
            "Text 2\n";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(3);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo("Text 1"); // normal text is trimmed
        then(response.getBlocks().get(1).getType()).isEqualTo("html");
        then(response.getBlocks().get(1).getContent()).isEqualTo("<html>html</html>\n");
        then(response.getBlocks().get(2).getType()).isEqualTo("text");
        then(response.getBlocks().get(2).getContent()).isEqualTo("Text 2");
    }

    @Test
    void parse_markdown_with_leading_and_trailing_whitespace_in_text_blocks() {
        final String MARKDOWN =
            "Leading and trailing\n" +
            "```\n" +
            "  code\n" +
            "```\n" +
            "More text";

        final Response response = new Response(query, MARKDOWN, messageContext);

        then(response.getBlocks()).hasSize(3);
        then(response.getBlocks().get(0).getType()).isEqualTo("text");
        then(response.getBlocks().get(0).getContent()).isEqualTo("Leading and trailing"); // trim() should remove leading/trailing whitespace
        then(response.getBlocks().get(1).getType()).isEqualTo("code");
        then(response.getBlocks().get(1).getContent()).isEqualTo("  code\n"); // Content inside code block should not be trimmed
        then(response.getBlocks().get(2).getType()).isEqualTo("text");
        then(response.getBlocks().get(2).getContent()).isEqualTo("More text");
    }

}
