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

import dev.langchain4j.exception.ToolExecutionException;
import io.github.jeddict.ai.lang.JeddictBrainListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import static ste.lloop.Loop.on;

public abstract class AbstractTool {

    /**
     * property notified for changes: status, progress or any other log
     */
    public static final String PROPERTY_MESSAGE = "toolMessage";

    protected final String basedir;
    protected final Path basepath;
    protected final Logger log;

    private final List<JeddictBrainListener> listeners = new CopyOnWriteArrayList<>();

    // TODO: add comment
    private Optional<UnaryOperator<String>> humanInTheMiddle = Optional.empty();

    public AbstractTool(final String basedir) throws IOException {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir can not be null or blank");
        }
        this.basedir = basedir;
        this.basepath = Paths.get(basedir).toAbsolutePath().toRealPath();
        this.log = Logger.getLogger(this.getClass().getName()); // this will be the concrete class name
    }

    public void addListener(final JeddictBrainListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        listeners.add(listener);
    }

    public void removeListener(final JeddictBrainListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        listeners.remove(listener);
    }

    public void checkPath(final String path) throws ToolExecutionException {
        //
        // NOTE: we can not use toRealPath here because we want to check even
        // if a path does not exists yet (toRealPath throws an exceptin if the
        // path is not valid
        //
        final Path absolutePath = (path.startsWith(File.separator)
                                ? Paths.get(path).normalize()
                                : basepath.resolve(path).toAbsolutePath().normalize());

        if (!absolutePath.startsWith(basepath)) {
            progress("❌ Trying to reach a file outside the project folder");
            throw new ToolExecutionException(
                "trying to reach a file outside the project folder");
        }
    }
    
    public Path fullPath(final String path) {
        return basepath.resolve(path).normalize();
    }

    public void log(Supplier<String> supplier) {
        log.logp(Level.INFO, log.getName(), "progress", supplier);
    }

    public void progress(final String message) {
        log(() -> message);
        on(listeners).loop((l) -> l.onProgress(message + "\n", true));
    }

    public String basedir() {
        return basedir;
    }

    public Optional<UnaryOperator<String>> humanInTheMiddle() {
        return humanInTheMiddle;
    }

    public void withHumanInTheMiddle(final UnaryOperator<String> hitm) {
        humanInTheMiddle = (hitm == null) ? Optional.empty() : Optional.of(hitm);
    }
}