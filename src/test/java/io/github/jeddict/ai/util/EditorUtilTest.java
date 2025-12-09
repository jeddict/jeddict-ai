package io.github.jeddict.ai.util;

import io.github.jeddict.ai.test.TestBase;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class EditorUtilTest extends TestBase {

    @Test
    public void wrapClassNamesWithAnchor_should_handle_special_characters_in_code_blocks() {
        String input = "Here is some code: <code>$variable</code> and <code>C:\\path</code>";
        String result = EditorUtil.wrapClassNamesWithAnchor(input);
        
        assertThat(result).contains("<code>$variable</code>");
        assertThat(result).contains("<code>C:\\path</code>");
    }
}