package org.embulk.util.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * A {@link BufferedReader} that can specify line delimiter character from any one of CR, LF and CRLF.
 * If not specified, use original {@link BufferedReader}.
 *
 * This class is not thread-safe.
 */
class LineReader extends BufferedReader {
    private LineReader(final Reader reader, final LineDelimiter lineDelimiter, final int bufferSize) {
        super(reader);

        this.lineDelimiter = lineDelimiter;
        this.buffer = new char[bufferSize];

        this.offset = UNREAD;
        this.charsRead = 0;
    }

    static BufferedReader of(final Reader reader, final LineDelimiter lineDelimiter, final int bufferSize) {
        if (lineDelimiter == null) {
            return new BufferedReader(reader);
        }
        return new LineReader(reader, lineDelimiter, bufferSize);
    }

    @Override
    public String readLine() throws IOException {
        StringBuilder line = null;
        char prevChar = Character.MIN_VALUE;

        bufferLoop:
        while (this.offset != UNREAD || (charsRead = this.read(buffer)) != -1) {
            if (this.offset == UNREAD) {
                // Initialize offset after read chars to buffer
                this.offset = 0;
            }
            if (line == null) {
                // Initialize line's buffer for the first loop
                line = new StringBuilder();
            }
            for (int i = offset; i < charsRead; i++) {
                final char c = buffer[i];
                boolean isEol = false;
                switch (lineDelimiter) {
                    case CR:
                        if (c == '\r') {
                            final Character next = this.readNext();
                            if (next == null || next != '\n') {
                                isEol = true;
                            }
                        }
                        break;
                    case LF:
                        if (prevChar != '\r' && c == '\n') {
                            isEol = true;
                        }
                        break;
                    case CRLF:
                        if (prevChar == '\r' && c == '\n') {
                            // Delete unnecessary CR
                            line.deleteCharAt(line.length() - 1);
                            isEol = true;
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unsupported line delimiter " + this.lineDelimiter);
                }
                this.offset++;
                if (isEol) {
                    break bufferLoop;
                }
                line.append(c);
                prevChar = c;
            }
            // Set "UNREAD" to read next chars
            this.offset = UNREAD;
        }

        if (line != null) {
            return line.toString();
        }
        return null;
    }

    private Character readNext() throws IOException {
        if (this.offset < this.charsRead - 1) {
            // From buffer
            return this.buffer[this.offset + 1];
        }
        // From reader
        this.mark(1);
        final char[] tmp = new char[1];
        final int read = this.read(tmp);
        this.reset();
        if (read == -1) {
            return null;
        }
        return tmp[0];
    }

    private static final int UNREAD = -1;

    private int offset;
    private int charsRead;

    private final LineDelimiter lineDelimiter;
    private final char[] buffer;
}
