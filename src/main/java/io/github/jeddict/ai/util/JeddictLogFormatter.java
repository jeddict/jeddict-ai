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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import org.apache.commons.lang3.StringUtils;
import static ste.lloop.Loop._break_;
import static ste.lloop.Loop.on;

public class JeddictLogFormatter extends Formatter {
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss.SSS");

    @Override
    public String format(LogRecord record) {
        final String timestamp = dateFormat.format(
            Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.of("UTC"))
        );

        return String.format(
            "%s %s %s %s %s %s%n",
            StringUtils.defaultIfBlank(timestamp, "-"),
            StringUtils.defaultIfBlank(String.valueOf(record.getLevel()), "-"),
            StringUtils.defaultIfBlank(threadName(record.getLongThreadID()), "-"),
            StringUtils.defaultIfBlank(record.getLoggerName(), "-"),
            StringUtils.defaultIfBlank(record.getSourceMethodName(), "-"),
            StringUtils.defaultIfBlank(record.getMessage(), "-")
        );
    }

    protected String threadName(final long id) {
        return on(Thread.getAllStackTraces()).loop((thread, stack) -> {
            if (thread.getId() == id) {
               _break_(((Thread)thread).getName());
            }
        });
    }
}