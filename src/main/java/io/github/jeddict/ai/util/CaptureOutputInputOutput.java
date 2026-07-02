package io.github.jeddict.ai.util;

import java.io.Reader;
import java.io.StringWriter;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

/**
 * Wrapper around a real {@link InputOutput} that captures everything written
 * to {@link #getOut()} and {@link #getErr()} so it can be retrieved later.
 */
public final class CaptureOutputInputOutput implements InputOutput {

    private final InputOutput delegate;
    private final CapturingOutputWriter outWriter;
    private final CapturingOutputWriter errWriter;

    public CaptureOutputInputOutput(InputOutput delegate) {
        this.delegate = delegate;
        this.outWriter = new CapturingOutputWriter(delegate.getOut());
        this.errWriter = new CapturingOutputWriter(delegate.getErr());
    }

    /**
     * Returns the captured standard output.
     */
    public String out() {
        return outWriter.content();
    }

    /**
     * Returns the captured error output.
     */
    public String err() {
        return errWriter.content();
    }

    /**
     * Clears both captured buffers.
     */
    public void reset() {
        outWriter.clearBuffer();
        errWriter.clearBuffer();
    }

    @Override
    public OutputWriter getOut() {
        return outWriter;
    }

    @Override
    public Reader getIn() {
        return delegate.getIn();
    }

    @Override
    public OutputWriter getErr() {
        return errWriter;
    }

    @Override
    public void closeInputOutput() {
        delegate.closeInputOutput();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public void setOutputVisible(boolean value) {
        delegate.setOutputVisible(value);
    }

    @Override
    public void setErrVisible(boolean value) {
        delegate.setErrVisible(value);
    }

    @Override
    public void setInputVisible(boolean value) {
        delegate.setInputVisible(value);
    }

    @Override
    public void select() {
        delegate.select();
    }

    @Override
    public boolean isErrSeparated() {
        return delegate.isErrSeparated();
    }

    @Override
    public void setErrSeparated(boolean value) {
        delegate.setErrSeparated(value);
    }

    @Override
    public boolean isFocusTaken() {
        return delegate.isFocusTaken();
    }

    @Override
    public void setFocusTaken(boolean value) {
        delegate.setFocusTaken(value);
    }

    @Override
    @Deprecated
    public Reader flushReader() {
        return delegate.flushReader();
    }

    private static final class CapturingOutputWriter extends OutputWriter {

        private final StringWriter buffer = new StringWriter();
        private final OutputWriter delegate;

        CapturingOutputWriter(OutputWriter delegate) {
            super(new StringWriter());
            this.delegate = delegate;
        }

        String content() {
            return buffer.toString();
        }

        void clearBuffer() {
            buffer.getBuffer().setLength(0);
        }

        @Override
        public void println(String s, OutputListener l) throws java.io.IOException {
            buffer.write(s);
            buffer.write(System.lineSeparator());
            delegate.println(s, l);
        }

        @Override
        public void println(String s) {
            buffer.write(s);
            buffer.write(System.lineSeparator());
            delegate.println(s);
        }

        @Override
        public void write(String s) {
            buffer.write(s);
            delegate.write(s);
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            buffer.write(cbuf, off, len);
            delegate.write(cbuf, off, len);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void reset() throws java.io.IOException {
            buffer.getBuffer().setLength(0);
            delegate.reset();
        }
    }
}
