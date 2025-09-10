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
package io.github.jeddict.ai.util;

import static org.assertj.core.api.BDDAssertions.then;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.io.File;
import java.io.IOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.Test;

public class FileUtilTest {

    @Test
    public void mime_type_for_various_file_types() throws IOException {
        // Special extensions
        then(FileUtil.mimeType("file.java")).isEqualTo("text/x-java");
        then(FileUtil.mimeType("file.c")).isEqualTo("text/x-c");
        then(FileUtil.mimeType("file.cpp")).isEqualTo("text/x-cpp");
        then(FileUtil.mimeType("file.ruby")).isEqualTo("text/x-ruby");
        then(FileUtil.mimeType("file.php")).isEqualTo("text/x-php");
        then(FileUtil.mimeType("file.php5")).isEqualTo("text/x-php5");

        // Fallback to URLConnection
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        then(FileUtil.mimeType("file.txt")).isEqualTo(fileNameMap.getContentTypeFor("file.txt"));
        then(FileUtil.mimeType("file.json")).isEqualTo(fileNameMap.getContentTypeFor("file.json"));
        then(FileUtil.mimeType("file.html")).isEqualTo(fileNameMap.getContentTypeFor("file.html"));
        then(FileUtil.mimeType("product.png")).isEqualTo(fileNameMap.getContentTypeFor("product.png"));

        // No extension or unknown extension
        then(FileUtil.mimeType("file")).isEqualTo("application/octet-stream");
        then(FileUtil.mimeType("file.unknown")).isEqualTo("application/octet-stream");

        // Test for existing file
        File tempFile = File.createTempFile("test", ".java");
        tempFile.deleteOnExit();
        then(FileUtil.mimeType(tempFile.getAbsolutePath())).isEqualTo("text/x-java");
    }

    @Test
    public void mime_type_throws_exception_for_invalid_filenames() {
        for (String value: new String[] {null, "", "   ", "  \t ", "\n\r" }) {
            assertThatThrownBy(() -> FileUtil.mimeType(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("filename can not be null or empty");
        }
    }
}
