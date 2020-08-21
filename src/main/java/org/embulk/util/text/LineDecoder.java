package org.embulk.util.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.Task;
import org.embulk.spi.FileInput;
import org.embulk.spi.util.FileInputInputStream;

public class LineDecoder implements AutoCloseable, Iterable<String> {
    // TODO optimize

    public LineDecoder(final FileInput in, final DecoderTask task) {
        this.charset = task.getCharset();
        this.inputStream = new FileInputInputStream(in);
        final CharsetDecoder decoder = charset
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
                .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.reader = LineReader.of(
                new InputStreamReader(inputStream, decoder), task.getLineDelimiterRecognized().orElse(null), 256);

        this.nextLine = null;
    }

    public static interface DecoderTask extends Task {
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        Charset getCharset();

        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        Newline getNewline();

        @Config("line_delimiter_recognized")
        @ConfigDefault("null")
        Optional<LineDelimiter> getLineDelimiterRecognized();
    }

    public boolean nextFile() {
        final boolean has = this.inputStream.nextFile();
        if (has && this.charset.equals(StandardCharsets.UTF_8)) {
            this.skipBom();
        }
        return has;
    }

    public String poll() {
        try {
            return this.reader.readLine();
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void close() {
        try {
            this.reader.close();
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public Iterator<String> iterator() {
        return new Ite(this);
    }

    private static class Ite implements Iterator<String> {
        public Ite(final LineDecoder self) {
            // TODO non-static inner class causes a problem with JRuby
            this.self = self;
        }

        @Override
        public boolean hasNext() {
            if (this.self.nextLine != null) {
                return true;
            } else {
                this.self.nextLine = this.self.poll();
                return this.self.nextLine != null;
            }
        }

        @Override
        public String next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            final String l = this.self.nextLine;
            this.self.nextLine = null;
            return l;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        private LineDecoder self;
    }

    private void skipBom() {
        boolean skip = false;
        try {
            if (this.charset.equals(StandardCharsets.UTF_8)) {
                this.reader.mark(3);
                final int firstChar = this.reader.read();
                if (firstChar == 0xFEFF) {
                    // skip BOM bytes
                    skip = true;
                }
            }
        } catch (final IOException ex) {
            // Passing through intentionally.
        } finally {
            if (skip) {
                // firstChar is skipped
            } else {
                // rollback to the marked position
                try {
                    this.reader.reset();
                } catch (final IOException ex) {
                    // unexpected
                    throw new UncheckedIOException(ex);
                }
            }
        }
    }

    private String nextLine;

    private final Charset charset;
    private final FileInputInputStream inputStream;
    private final BufferedReader reader;
}
