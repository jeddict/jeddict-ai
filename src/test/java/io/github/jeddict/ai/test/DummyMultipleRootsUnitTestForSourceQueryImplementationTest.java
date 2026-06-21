/*
 * Copyright 2026 the original author or authors from the LLMTooliy project
 * (https://stefanofornari.github.io/llm-toolify).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.jeddict.ai.test;

import com.github.caciocavallosilano.cacio.ctc.junit.CacioTest;
import static org.assertj.core.api.BDDAssertions.then;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;

@CacioTest
public class DummyMultipleRootsUnitTestForSourceQueryImplementationTest extends TestBase {

    @Test
    public void findUnitTests_maps_main_java_to_test_java_with_Test_suffix() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject root = fs.getRoot().createFolder("project1");
        final FileObject src = root.createFolder("src");
        final FileObject mainJava = src.createFolder("main").createFolder("java")
                .createFolder("com").createFolder("acme");
        final FileObject mainFile = mainJava.createData("Main", "java");
        src.createFolder("test").createFolder("java")
                .createFolder("com").createFolder("acme").createData("MainTest", "java");

        final Project project = new DummyProject(root);
        final DummyMultipleRootsUnitTestForSourceQueryImplementation query =
                new DummyMultipleRootsUnitTestForSourceQueryImplementation(project);

        final URL[] urls = query.findUnitTests(mainFile);

        then(urls).hasSize(1);
        then(urls[0].toExternalForm()).contains("/src/test/java/com/acme/MainTest.java");
    }

    @Test
    public void findSources_maps_test_java_to_main_java() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject root = fs.getRoot().createFolder("project1");
        final FileObject src = root.createFolder("src");
        src.createFolder("main").createFolder("java")
                .createFolder("com").createFolder("acme").createData("Main", "java");
        final FileObject testJava = src.createFolder("test").createFolder("java")
                .createFolder("com").createFolder("acme");
        final FileObject testFile = testJava.createData("MainTest", "java");

        final Project project = new DummyProject(root);
        final DummyMultipleRootsUnitTestForSourceQueryImplementation query =
                new DummyMultipleRootsUnitTestForSourceQueryImplementation(project);

        final URL[] urls = query.findSources(testFile);

        then(urls).hasSize(1);
        then(urls[0].toExternalForm()).contains("/src/main/java/com/acme/Main.java");
    }

    @Test
    public void works_for_directory_fileobjects_with_or_without_trailing_slash() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject root = fs.getRoot().createFolder("project1");
        final FileObject src = root.createFolder("src");
        final FileObject mainPackageDir = src.createFolder("main").createFolder("java")
                .createFolder("com").createFolder("acme");
        src.createFolder("test").createFolder("java")
                .createFolder("com").createFolder("acme");

        final Project project = new DummyProject(root);
        final DummyMultipleRootsUnitTestForSourceQueryImplementation query =
                new DummyMultipleRootsUnitTestForSourceQueryImplementation(project);

        final URL[] urls = query.findUnitTests(mainPackageDir);
        then(urls).hasSize(1);
        then(urls[0].toExternalForm()).contains("/src/test/java/com/acme/");
    }

    @Test
    public void returns_empty_array_for_non_matching_paths() throws IOException {
        final FileSystem fs = FileUtil.createMemoryFileSystem();
        final FileObject root = fs.getRoot().createFolder("project1");
        final FileObject other = root.createFolder("docs").createData("Readme", "txt");

        final Project project = new DummyProject(root);
        final DummyMultipleRootsUnitTestForSourceQueryImplementation query =
                new DummyMultipleRootsUnitTestForSourceQueryImplementation(project);

        then(query.findUnitTests(other)).isEmpty();
        then(query.findSources(other)).isEmpty();
    }
}
