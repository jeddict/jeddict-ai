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
package io.github.jeddict.ai.components.diff;

import java.io.IOException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;
import org.junit.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 */
public class FileStreamSourceTest {

    @Test
    public void create_from_file_object() throws Exception {
        FileObject FILE = FileUtil.getSystemConfigRoot();  // just to get a valid file object;
        FileStreamSource S = new FileStreamSource(FILE);

        thenAllValuesMatch(S, FILE);

        FILE = FILE.createData("test");
        S = new FileStreamSource(FILE);

        thenAllValuesMatch(S, FILE);
    }

    @Test
    public void constructor_sanity_check() {
        thenThrownBy(() -> new FileStreamSource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("fileObject can not be null");
    }

    private void thenAllValuesMatch(final FileStreamSource S, final FileObject F)
    throws IOException {
        then(S.fileObject).isSameAs(F);
        then(S.getName()).isEqualTo(F.getNameExt());
        then(S.getTitle()).isEqualTo(F.getNameExt());
        then(S.getMIMEType()).isEqualTo(F.getMIMEType());
        then(S.isEditable()).isEqualTo(F.canWrite());
        then(S.getLookup()).isNotNull();
        then(S.createReader()).isNotNull();
        then(S.createWriter(null)).isNull();
        then(S.toString()).isEqualTo(FileStreamSource.class.getCanonicalName() + "[" + F.getPath() + "]");
    }
}
