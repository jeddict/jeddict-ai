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
package io.github.jeddict.ai.test;

import java.io.File;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

/**
 *
 */
public class DummyProject implements Project {

    private final FileObject projectDir;

    public DummyProject(final File projectDir) {
        if (projectDir == null) {
            throw new IllegalArgumentException("project directory cannot be null");
        }
        FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(projectDir));
        if (fo == null) {
            throw new IllegalArgumentException("project directory cannot be null or invalid");
        }
        this.projectDir = fo;
    }

    public DummyProject(final FileObject projectDir) {
        if (projectDir == null) {
            throw new IllegalArgumentException("projectDir cannot be null");
        }
        this.projectDir = projectDir;
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        return Lookup.getDefault();
    }



}
