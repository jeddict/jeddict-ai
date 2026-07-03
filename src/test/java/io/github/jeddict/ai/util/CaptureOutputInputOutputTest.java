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
package io.github.jeddict.ai.util;

import io.github.jeddict.ai.test.TestBase;
import java.io.Reader;
import java.io.StringWriter;
import static org.assertj.core.api.BDDAssertions.then;
import org.junit.jupiter.api.Test;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

public class CaptureOutputInputOutputTest extends TestBase {

    @Test
    public void captures_out_and_err_and_can_reset() throws Exception {
        InputOutput delegate = InputOutput.NULL;
        CaptureOutputInputOutput io = new CaptureOutputInputOutput(delegate);

        OutputWriter out = io.getOut();
        out.write("hello");
        out.println(" world");

        OutputWriter err = io.getErr();
        err.write("boom");
        err.println("!");

        then(io.out()).contains("hello");
        then(io.out()).contains(" world");
        then(io.err()).contains("boom");
        then(io.err()).contains("!");

        io.reset();

        then(io.out()).isEmpty();
        then(io.err()).isEmpty();
    }

    @Test
    public void out_and_err_are_passed_through_to_delegate() throws Exception {
        SpyInputOutput spy = new SpyInputOutput();
        CaptureOutputInputOutput io = new CaptureOutputInputOutput(spy);

        io.getOut().write("hello");
        io.getOut().println(" world");
        io.getErr().write("boom");
        io.getErr().println("!");

        // captured
        then(io.out()).contains("hello").contains(" world");
        then(io.err()).contains("boom").contains("!");

        // also forwarded to the delegate
        then(spy.outCapture.toString()).contains("hello").contains(" world");
        then(spy.errCapture.toString()).contains("boom").contains("!");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Minimal InputOutput spy whose OutputWriters record everything written. */
    private static class SpyInputOutput implements InputOutput {

        final StringWriter outCapture = new StringWriter();
        final StringWriter errCapture = new StringWriter();

        private final OutputWriter out = new RecordingOutputWriter(outCapture);
        private final OutputWriter err = new RecordingOutputWriter(errCapture);

        @Override public OutputWriter getOut() { return out; }
        @Override public OutputWriter getErr() { return err; }
        @Override public Reader getIn()        { return Reader.nullReader(); }
        @Override public void closeInputOutput() {}
        @Override public boolean isClosed()    { return false; }
        @Override public void setOutputVisible(boolean v) {}
        @Override public void setErrVisible(boolean v)    {}
        @Override public void setInputVisible(boolean v)  {}
        @Override public void select()                    {}
        @Override public boolean isErrSeparated()         { return false; }
        @Override public void setErrSeparated(boolean v)  {}
        @Override public boolean isFocusTaken()           { return false; }
        @Override public void setFocusTaken(boolean v)    {}
        @Override @Deprecated public Reader flushReader() { return Reader.nullReader(); }
    }

    private static class RecordingOutputWriter extends OutputWriter {

        private final StringWriter sink;

        RecordingOutputWriter(StringWriter sink) {
            super(sink);
            this.sink = sink;
        }

        @Override
        public void println(String s, OutputListener l) {
            sink.write(s);
            sink.write(System.lineSeparator());
        }

        @Override
        public void println(String s) {
            sink.write(s);
            sink.write(System.lineSeparator());
        }

        @Override public void write(String s)                           { sink.write(s); }
        @Override public void write(char[] buf, int off, int len)       { sink.write(buf, off, len); }
        @Override public void flush()                                   {}
        @Override public void close()                                   {}
        @Override public void reset()                                   { sink.getBuffer().setLength(0); }
    }
}
