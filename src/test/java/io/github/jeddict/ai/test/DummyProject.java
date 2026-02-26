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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 */
public class DummyProject implements Project {

    private final FileObject projectDirectory;
    private final Lookup lookup;

    private String name, type;

    public final InstanceContent instances;
    public final String realProjectDirectory;

    public DummyProject(final File projectDir) {
        if (projectDir == null) {
            throw new IllegalArgumentException("projectDir can not be null");
        }
        FileObject fo = FileUtil.toFileObject(FileUtil.normalizeFile(projectDir));
        if (fo == null) {
            throw new IllegalArgumentException("project directory can not be null or invalid");
        }
        this.projectDirectory = fo;
        
        try {
            this.realProjectDirectory = Paths.get(fo.getPath()).toRealPath().toString();
        } catch (IOException x) {
            x.printStackTrace();
            throw new IllegalArgumentException("unexpected error in getting the real path: " + x);
        }
        this.instances = new InstanceContent();
        this.lookup = new AbstractLookup(instances);
    }

    public DummyProject(final FileObject projectDir) {
        if (projectDir == null) {
            throw new IllegalArgumentException("projectDir can not be null");
        }
        
        //
        // We need to deal with different file systems here...
        // - on Windows long paths have a "link" to a short path; toRealPath()
        //   makes sure we have always the one provided (i.e. long)
        // - on Mac, temporary files are created in a directory which is simlinked
        //   from /private; toRealPath() makes sure we have always the real 
        //   path under /private
        // - on Linux, all is pretty much as expected; toRealPath() is basically 
        //   transparent
        // - on a memory file system toRealPath() fails ... :|
        //
        try {
            this.realProjectDirectory = (projectDir.getFileSystem().isDefault())
                ? Paths.get(projectDir.getPath()).toRealPath().toString()
                : projectDir.getPath();
        } catch (IOException x) {
            x.printStackTrace();
            throw new IllegalArgumentException("unexpected error in getting the real path: " + x);
        }
        this.projectDirectory = projectDir;
        this.instances = new InstanceContent();
        this.lookup = new AbstractLookup(instances);
    }

    public DummyProject(final Path projectDir) {
        this((projectDir == null) ? (File)null : projectDir.toFile());
    }

    public DummyProject(final String projectDir) {
        this((projectDir == null) ? (File)null : new File(projectDir));
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    public void name(final String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    public void type(final String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}