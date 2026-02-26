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

import java.io.IOException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public abstract class AbstractBuildTool extends AbstractTool {

    private final String buildFile;

    public AbstractBuildTool(final String basedir, final String buildFile) throws IOException {
        super(basedir);
        this.buildFile = buildFile;
    }

    protected FileObject buildFile() throws Exception {
        //
        // Note that this cannot be put in the constructor because we accept
        // the project pom is not there at instatiation time; what's important
        // is that he pom is there when the tool is invoked
        //
        final FileObject projectDir = FileUtil.toFileObject(basepath);
        final FileObject pomFile = projectDir.getFileObject(buildFile);
        if (pomFile == null || !pomFile.isValid()) {
            throw new Exception("pom.xml not found in project directory");
        }

        return pomFile;
    }
}