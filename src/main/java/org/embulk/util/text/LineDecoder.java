/*
 * Copyright 2014 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.embulk.spi.FileInput;
import org.embulk.util.file.FileInputInputStream;

/**
 * Decodes {@link org.embulk.spi.FileInput} into iteration of lines.
 *
 * <p>Unlike {@code embulk-core}'s {@code org.embulk.spi.util.LineDecoder}, it does not receive a task-defining
 * interface like {@code DecoderTask}. Use {@link #of(FileInput, Charset, LineDelimiter)} instead.
 *
 * <pre><code>LineDecoder decoder = LineDecoder.of(fileInput, charset, null);</code></pre>
 */
public class LineDecoder implements AutoCloseable, Iterable<String> {
    // TODO optimize

    private LineDecoder(
            final FileInputInputStream inputStream,
            final Charset charset,
            final BufferedReader reader) {
        this.inputStream = inputStream;
        this.charset = charset;
        this.reader = reader;

        this.nextLine = null;
    }

    public static LineDecoder of(final FileInput in, final Charset charset, final LineDelimiter lineDelimiterRecognized) {
        final FileInputInputStream inputStream = new FileInputInputStream(in);
        final CharsetDecoder decoder = charset
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
                .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?

        return new LineDecoder(
                inputStream,
                charset,
                LineReader.of(new InputStreamReader(inputStream, decoder), lineDelimiterRecognized, 256));
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
