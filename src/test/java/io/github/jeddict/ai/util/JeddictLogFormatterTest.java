/**
 * Copyright 2025-2026 the original author or authors from the Jeddict project
 * (https://jeddict.github.io/).
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

package io.github.jeddict.ai.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import static org.assertj.core.api.BDDAssertions.then;

public class JeddictLogFormatterTest {
    private JeddictLogFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new JeddictLogFormatter();
    }

    @Test
    public void format_should_include_all_elements() {
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setLoggerName("TestLogger");
        record.setInstant(Instant.parse("2023-01-02T01:02:03Z"));
        String result = formatter.format(record);

        then(result).isEqualTo(
            "20230102 01:02:03.000 INFO %s TestLogger Test message\n".formatted(Thread.currentThread().getName())
        );

        record = new LogRecord(Level.FINE, "Test message two");
        record.setLoggerName("TestLogger2");
        record.setInstant(Instant.parse("2023-02-02T01:02:03Z"));

        then(formatter.format(record)).isEqualTo(
            "20230202 01:02:03.000 FINE %s TestLogger2 Test message two\n".formatted(Thread.currentThread().getName())
        );
    }

    @Test
    public void format_should_handle_null_values() {
        final LogRecord record = new LogRecord(Level.INFO, null);
        record.setLoggerName(null);
        record.setInstant(Instant.parse("2023-01-02T01:02:03Z"));

        then(formatter.format(record)).isEqualTo(
            "20230102 01:02:03.000 INFO %s - -\n".formatted(Thread.currentThread().getName())
        );
    }

    @Test
    public void format_should_handle_empty_values() {
        final LogRecord record = new LogRecord(Level.INFO, "");
        record.setLoggerName("");
        record.setInstant(Instant.parse("2023-01-02T01:02:03Z"));

        then(formatter.format(record)).isEqualTo(
            "20230102 01:02:03.000 INFO %s - -\n".formatted(Thread.currentThread().getName())
        );
    }
}