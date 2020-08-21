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

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;
import org.embulk.EmbulkTestRuntime;
import org.embulk.spi.Buffer;
import org.embulk.spi.BufferImpl;
import org.embulk.spi.util.ListFileInput;
import org.junit.Rule;
import org.junit.Test;

public class TestLineDecoder {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private static LineDecoder newDecoder(Charset charset, Newline newline, List<Buffer> buffers) {
        return newDecoder(charset, newline, null, buffers);
    }

    private static LineDecoder newDecoder(Charset charset, Newline newline, LineDelimiter lineDelimiter, List<Buffer> buffers) {
        ListFileInput input = new ListFileInput(ImmutableList.of(buffers));
        return LineDecoder.of(input, charset, lineDelimiter);
    }

    private static List<String> doDecode(Charset charset, Newline newline, List<Buffer> buffers) {
        return doDecode(charset, newline, null, buffers);
    }

    private static List<String> doDecode(Charset charset, Newline newline, LineDelimiter lineDelimiter, List<Buffer> buffers) {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        LineDecoder decoder = newDecoder(charset, newline, lineDelimiter, buffers);
        decoder.nextFile();
        while (true) {
            String line = decoder.poll();
            if (line == null) {
                break;
            }
            builder.add(line);
        }
        return builder.build();
    }

    private static List<Buffer> bufferList(Charset charset, String... sources) throws UnsupportedCharsetException {
        List<Buffer> buffers = new ArrayList<Buffer>();
        for (String source : sources) {
            ByteBuffer buffer = charset.encode(source);
            buffers.add(BufferImpl.wrap(buffer.array(), 0, buffer.limit()));
        }

        return buffers;
    }

    @Test
    public void testDecodeBasicAscii() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "test1\ntest2\ntest3\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicAsciiCRLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "test1\r\ntest2\r\ntest3\r\n"));
        assertEquals(ImmutableList.of("test1", "test2", "test3"), decoded);
    }

    @Test
    public void testDecodeBasicAsciiTail() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "test1"));
        assertEquals(ImmutableList.of("test1"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "t", "1", "\n", "t", "2"));
        assertEquals(ImmutableList.of("t1", "t2"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksCRLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "t", "1", "\r\n", "t", "2", "\r", "\n", "t3"));
        assertEquals(ImmutableList.of("t1", "t2", "t3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicUTF8() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicUTF8Tail() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksUTF8LF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.LF,
                bufferList(StandardCharsets.UTF_8, "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksUTF8CRLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                bufferList(StandardCharsets.UTF_8, "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicUTF16LE() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicUTF16LETail() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksUTF16LELF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.LF,
                bufferList(StandardCharsets.UTF_16LE, "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksUTF16LECRLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_16LE, Newline.CRLF,
                bufferList(StandardCharsets.UTF_16LE, "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicMS932() throws Exception {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "てすと1\nテスト2\nてすと3\n"));
        assertEquals(ImmutableList.of("てすと1", "テスト2", "てすと3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeBasicMS932Tail() throws Exception {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "てすと1"));
        assertEquals(ImmutableList.of("てすと1"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksMS932LF() throws Exception {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.LF,
                bufferList(Charset.forName("ms932"), "て", "1", "\n", "す", "2"));
        assertEquals(ImmutableList.of("て1", "す2"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeChunksMS932CRLF() throws Exception {
        List<String> decoded = doDecode(
                Charset.forName("ms932"), Newline.CRLF,
                bufferList(Charset.forName("ms932"), "て", "1", "\r\n", "す", "2", "\r", "\n", "と3"));
        assertEquals(ImmutableList.of("て1", "す2", "と3"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeWithLineDelimiterRecognizedCR() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                LineDelimiter.CR,
                bufferList(StandardCharsets.UTF_8, "test1\r\ntest2\rtest3\ntest4"));
        assertEquals(ImmutableList.of("test1\r\ntest2", "test3\ntest4"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeWithLineDelimiterRecognizedLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                LineDelimiter.LF,
                bufferList(StandardCharsets.UTF_8, "test1\r\ntest2\rtest3\ntest4"));
        assertEquals(ImmutableList.of("test1\r\ntest2\rtest3", "test4"), decoded);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testDecodeWithLineDelimiterRecognizedCRLF() throws Exception {
        List<String> decoded = doDecode(
                StandardCharsets.UTF_8, Newline.CRLF,
                LineDelimiter.CRLF,
                bufferList(StandardCharsets.UTF_8, "test1\r\ntest2\rtest3\ntest4"));
        assertEquals(ImmutableList.of("test1", "test2\rtest3\ntest4"), decoded);
    }
}
