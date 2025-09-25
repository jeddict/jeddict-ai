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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import org.netbeans.api.diff.Difference;
import org.netbeans.api.diff.StreamSource;
import org.netbeans.api.queries.FileEncodingQuery;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 * A StreamSource on top of a FileObject. This class provides basic implementation
 * of the StreamSource which uses a FileObject to read the content.
 *
 * TODO: allow to provide a title (i.e. file path relative to the project root
 *
 */
public class FileStreamSource extends StreamSource {

    public final FileObject fileObject;

    /**
     * Constructs a FileStreamSource for a given FileObject.
     */
    public FileStreamSource(FileObject fileObject) {
        if (fileObject == null) {
            throw new IllegalArgumentException("fileObject can not be null");
        }
        this.fileObject = fileObject;
    }

    /**
     * Checks if the underlying file is editable.
     *
     * @return true if the file is writable, false otherwise.
     */
    @Override
    public boolean isEditable() {
        return fileObject.canWrite();
    }

    /**
     * Provides a Lookup instance for the FileObject.
     *
     * @return a Lookup instance containing the FileObject.
     */
    @Override
    public Lookup getLookup() {
        return Lookups.fixed(fileObject);
    }

    /**
     * Retrieves the display name with its extension.
     *
     * @return the name and extension of the file.
     */
    @Override
    public String getName() {
        return fileObject.getNameExt();
    }

    /**
     * Retrieves the title for the stream source which for now, defaults to file name and extension.
     * 
     *
     * @return the title of the stream source.
     */
    @Override
    public String getTitle() {
        // TODO: Update to include relative file path.
        return fileObject.getNameExt();
    }

    /**
     * Gets the MIME type of the file.
     *
     * @return the MIME type as a String.
     */
    @Override
    public String getMIMEType() {
        return fileObject.getMIMEType();
    }

    /**
     * Creates a Reader to read the contents of the file.
     *
     * @return a Reader for the file content.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public Reader createReader() throws IOException {
        return new InputStreamReader(fileObject.getInputStream(), FileEncodingQuery.getEncoding(fileObject));
    }

    /**
     * Creates a Writer to write the content to the file. Currently not implemented.
     *
     * @param differences an array of differences if applicable, unused.
     * @return always returns null as writing is not supported.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public Writer createWriter(Difference[] differences) throws IOException {
        // Not supported yet
        return null;
    }

    /**
     * Provides a string representation of this FileStreamSource.
     *
     * @return a string representation containing the class name and file path.
     */
    @Override
    public String toString() {
        return getClass().getCanonicalName() + "[" + fileObject.getPath() + "]";
    }
}
