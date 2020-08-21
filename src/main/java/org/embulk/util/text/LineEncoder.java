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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileOutput;
import org.embulk.util.file.FileOutputOutputStream;

/**
 * Encodes {@link java.io.BufferedWriter} as iteration of lines into {@link org.embulk.spi.FileOutput}.
 *
 * <p>Unlike {@code embulk-core}'s {@code org.embulk.spi.util.LineEncoder}, it does not receive a task-defining
 * interface like {@code EncoderTask}. Use {@link #of(FileOutput, Newline, Charset, BufferAllocator)} instead.
 *
 * <pre><code>LineEncoder encoder = LineEncoder.of(fileOutput, newline, charset, Exec.getBufferAllocator());</code></pre>
 */
public class LineEncoder implements AutoCloseable {
    // TODO optimize

    private LineEncoder(
            final FileOutput fileOutput,
            final FileOutputOutputStream outputStream,
            final String newline,
            final BufferedWriter writer) {
        this.underlyingFileOutput = fileOutput;
        this.outputStream = outputStream;
        this.newline = newline;
        this.writer = writer;
    }

    public static LineEncoder of(
            final FileOutput fileOutput,
            final Newline newline,
            final Charset charset,
            final BufferAllocator bufferAllocator) {
        final FileOutputOutputStream outputStream = new FileOutputOutputStream(
                fileOutput, bufferAllocator, FileOutputOutputStream.CloseMode.FLUSH_FINISH);

        final CharsetEncoder encoder = charset
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
                .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?

        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, encoder), 32 * 1024);

        return new LineEncoder(
                fileOutput,
                outputStream,
                newline.getString(),
                writer);
    }

    public void addNewLine() {
        try {
            this.writer.append(this.newline);
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
    }

    public void addLine(final String line) {
        try {
            this.writer.append(line);
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
        this.addNewLine();
    }

    public void addText(final String text) {
        try {
            this.writer.append(text);
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
    }

    public void nextFile() {
        try {
            this.writer.flush();
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
        this.outputStream.nextFile();
    }

    public void finish() {
        try {
            if (this.writer != null) {
                this.writer.close();  // FLUSH_FINISH
                this.writer = null;
                // underlyingFileOutput.finish() is already called by close() because CloseMode is FLUSH_FINISH
            }
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void close() {
        try {
            if (this.writer != null) {
                this.writer.close();  // FLUSH_FINISH
                this.writer = null;
            }
            this.underlyingFileOutput.close();  // this is necessary because CloseMode is not FLUSH_FINISH_CLOSE
        } catch (final IOException ex) {
            // unexpected
            throw new UncheckedIOException(ex);
        }
    }

    private Writer writer;

    private final String newline;
    private final FileOutput underlyingFileOutput;
    private final FileOutputOutputStream outputStream;
}
