package org.embulk.util.text;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigInject;
import org.embulk.config.Task;
import org.embulk.spi.BufferAllocator;
import org.embulk.spi.FileOutput;
import org.embulk.spi.util.FileOutputOutputStream;

public class LineEncoder implements AutoCloseable {
    // TODO optimize

    public LineEncoder(final FileOutput out, final EncoderTask task) {
        this.newline = task.getNewline().getString();
        this.underlyingFileOutput = out;
        final CharsetEncoder encoder = task.getCharset()
                .newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)  // TODO configurable?
                .onUnmappableCharacter(CodingErrorAction.REPLACE);  // TODO configurable?
        this.outputStream = new FileOutputOutputStream(
                underlyingFileOutput, task.getBufferAllocator(), FileOutputOutputStream.CloseMode.FLUSH_FINISH);

        this.writer = new BufferedWriter(new OutputStreamWriter(outputStream, encoder), 32 * 1024);
    }

    public interface EncoderTask extends Task {
        @Config("charset")
        @ConfigDefault("\"utf-8\"")
        Charset getCharset();

        @Config("newline")
        @ConfigDefault("\"CRLF\"")
        Newline getNewline();

        @ConfigInject
        BufferAllocator getBufferAllocator();
    }

    public void addNewLine() {
        try {
            this.writer.append(this.newline);
        } catch (final IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public void addLine(final String line) {
        try {
            this.writer.append(line);
        } catch (final IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
        this.addNewLine();
    }

    public void addText(final String text) {
        try {
            this.writer.append(text);
        } catch (final IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
        }
    }

    public void nextFile() {
        try {
            this.writer.flush();
        } catch (final IOException ex) {
            // unexpected
            throw new RuntimeException(ex);
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
            throw new RuntimeException(ex);
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
            throw new RuntimeException(ex);
        }
    }

    private Writer writer;

    private final String newline;
    private final FileOutput underlyingFileOutput;
    private final FileOutputOutputStream outputStream;
}
