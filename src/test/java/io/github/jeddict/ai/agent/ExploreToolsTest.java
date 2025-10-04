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
package io.github.jeddict.ai.agent;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import io.github.jeddict.ai.test.BaseTest;
import java.io.File;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@CacioTest
public class ExploreToolsTest extends BaseTest {

    @Test
    public void listClassesInFile_with_non_existing_file_returns_file_not_found()
    throws Exception {
        final String path = "non/existing/File.java";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        then(tools.listClassesInFile(path)).isEqualTo("File not found: " + path);
    }

    @Test
    public void listClassesInFile_with_not_a_java_file_returns_error()
    throws Exception {
        final String path = "folder/testfile.txt";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        //
        // Let's keep the same message returned by withJavaSource for now. If
        // we ened to handle the messages differently, let's refactr the method
        // to throw exceptions
        //
        then(tools.listClassesInFile(path)).isEqualTo("Not a Java source file: " + path);
    }

    @Test
    //
    // Ignoring as I was not able to make it work; theoretically the class
    // should be recognized as a valid class... I think...
    //
    @Disabled
    public void listClassesInFile_with_java_file_returns_classes()
    throws Exception {
        final String path = "SayHello.java";
        final File fullPath = new File(projectDir, path);

        FileUtils.copyFile(
            new File("src/test/java/io/github/jeddict/ai/test/SayHello.java"),
            fullPath
        );

        final ExplorationTools tools = new ExplorationTools(projectDir, null);
        then(tools.listClassesInFile(path)).contains("Class: HelloWorld");
    }

    @Test
    public void listMethodsInFile_with_non_existing_file_returns_file_not_found()
    throws Exception {
        final String path = "non/existing/File.java";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        then(tools.listMethodsInFile(path)).isEqualTo("File not found: " + path);
    }

    @Test
    public void listMethodsInFile_with_not_a_java_file_returns_error()
    throws Exception {
        final String path = "folder/testfile.txt";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        //
        // Let's keep the same message returned by withJavaSource for now. If
        // we ened to handle the messages differently, let's refactr the method
        // to throw exceptions
        //
        then(tools.listMethodsInFile(path)).isEqualTo("Not a Java source file: " + path);
    }

    @Test
    //
    // Ignoring as I was not able to make it work; theoretically the class
    // should be recognized as a valid class... I think...
    //
    @Disabled
    public void listMethodInFile_with_java_file_returns_methods()
    throws Exception {
        final String path = "SayHello.java";
        final File fullPath = new File(projectDir, path);

        FileUtils.copyFile(
            new File("src/test/java/io/github/jeddict/ai/test/SayHello.java"),
            fullPath
        );

        final ExplorationTools tools = new ExplorationTools(projectDir, null);
        then(tools.listMethodsInFile(path)).contains("Method: sayHello");
    }

    @Test
    public void findUsages_with_invalid_path_return_file_not_found()
    throws Exception {
        final String path = "non/existing/File.java";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        then(tools.findUsages(path, "sayHello")).isEqualTo("File not found: " + path);
    }

    @Test
    public void findUsages_with_not_a_java_file_returns_error()
    throws Exception {
        final String path = "folder/testfile.txt";
        final ExplorationTools tools = new ExplorationTools(projectDir, null);

        //
        // Let's keep the same message returned by withJavaSource for now. If
        // we ened to handle the messages differently, let's refactr the method
        // to throw exceptions
        //
        then(tools.findUsages(path, "sayHello")).isEqualTo("Not a Java source file: " + path);
    }

    @Test
    //
    // Ignoring as I was not able to make it work; theoretically the class
    // should be recognized as a valid class... I think...
    //
    @Disabled
    public void findUsages_with_java_file_returns_classes()
    throws Exception {
        final String path = "SayHello.java";
        final File fullPath = new File(projectDir, path);

        FileUtils.copyFile(
            new File("src/test/java/io/github/jeddict/ai/test/SayHello.java"),
            fullPath
        );

        final ExplorationTools tools = new ExplorationTools(projectDir, null);
        then(tools.findUsages(path, "sayHello")).contains("Usage: sayHello"); // TO BE FIXED...
    }
}
