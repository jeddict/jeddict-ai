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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.logging.Logger;

public abstract class AbstractTool {

    /**
     * property notified for changes: status, progress or any other log
     */
    public static final String PROPERTY_MESSAGE = "toolMessage";

    protected final String basedir;
    protected final Path basepath;
    protected final Logger log;
    private final PropertyChangeSupport toolListener = new PropertyChangeSupport(this);

    public AbstractTool(final String basedir) {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir can not be null or blank");
        }
        this.basedir = basedir;
        this.basepath = Paths.get(basedir);
        this.log = Logger.getLogger(this.getClass().getCanonicalName()); // this will be the concrete class name
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        toolListener.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        toolListener.removePropertyChangeListener(listener);
    }

    public Path fullPath(final String path) {
        return basepath.resolve(path);
    }

    public void log(Supplier<String> supplier) {
        log.info(supplier);
    }

    public void progress(String message) {
        log(() -> message);
        toolListener.firePropertyChange(PROPERTY_MESSAGE, null, message);
    }
}