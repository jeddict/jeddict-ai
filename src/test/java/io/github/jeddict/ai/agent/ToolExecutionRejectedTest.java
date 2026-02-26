package io.github.jeddict.ai.agent;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.BDDAssertions.then;

public class ToolExecutionRejectedTest {

    @Test
    void testConstructors() {
        // no-arg
        then(new ToolExecutionRejected()).hasMessage("REJECTED");

        // message
        then(new ToolExecutionRejected("user cancelled action"))
                .hasMessage("REJECTED:user cancelled action");

        // message + cause
        Throwable cause1 = new RuntimeException("connection timeout");
        then(new ToolExecutionRejected("network failure", cause1))
                .hasMessage("REJECTED:network failure (java.lang.RuntimeException: connection timeout)");

        // cause only
        Throwable cause2 = new IllegalStateException("invalid state");
        then(new ToolExecutionRejected(cause2))
                .hasMessage("REJECTED:java.lang.IllegalStateException: invalid state");
    }
}
