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

import io.github.jeddict.ai.lang.JeddictBrainListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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

    //
    // TODO: extend it to multiple listeners
    //
    protected Optional<JeddictBrainListener> listener = Optional.empty();

    private final List<JeddictBrainListener> listeners = new CopyOnWriteArrayList<>();

    // TODO: add comment
    private Optional<UnaryOperator<String>> humanInTheMiddle = Optional.empty();

    public AbstractTool(final String basedir) {
        if (basedir == null) {
            throw new IllegalArgumentException("basedir can not be null or blank");
        }
        this.basedir = basedir;
        this.basepath = Paths.get(basedir);
        this.log = Logger.getLogger(this.getClass().getCanonicalName()); // this will be the concrete class name
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

    public Path fullPath(final String path) {
        return basepath.resolve(path);
    }

    public void log(Supplier<String> supplier) {
        log.info(supplier.get());
    }

    public void progress(final String message) {
        log(() -> message);
        on(listeners).loop((l) -> l.onProgress(message));
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

    public String basedir() {
        return basedir;
    }
}