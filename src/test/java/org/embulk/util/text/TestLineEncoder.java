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

import java.io.UnsupportedEncodingException;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigSource;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.FileOutput;
import org.embulk.spi.MockFileOutput;
import org.junit.Rule;
import org.junit.Test;

public class TestLineEncoder {
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private LineEncoder newEncoder(String charset, String newline,
            FileOutput output) throws Exception {
        ConfigSource config = Exec.newConfigSource()
                .set("charset", charset)
                .set("newline", newline);
        return new LineEncoder(output, config.loadConfig(LineEncoder.EncoderTask.class));
    }

    @Test
    public void testAddLine() throws Exception {
        try (MockFileOutput output = new MockFileOutput()) {
            LineEncoder encoder = newEncoder("utf-8", "LF", output);
            encoder.nextFile();
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            encoder.finish();
            // TODO
            //Iterator<Buffer> ite = output.getLastBuffers().iterator();
            //assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            //assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            //assertFalse(ite.hasNext());
        }
    }

    @Test
    public void testAddTextAddNewLine() throws Exception {
        try (MockFileOutput output = new MockFileOutput()) {
            LineEncoder encoder = newEncoder("utf-8", "LF", output);
            encoder.nextFile();
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addText(line);
                encoder.addNewLine();
            }
            encoder.finish();
            // TODO
            //Iterator<Buffer> ite = output.getLastBuffers().iterator();
            //assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            //assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\n", bufferToString(ite.next(), "utf-8"));
            //assertFalse(ite.hasNext());
        }
    }

    @Test
    public void testNewLine() throws Exception {
        try (MockFileOutput output = new MockFileOutput()) {
            LineEncoder encoder = newEncoder("utf-8", "CRLF", output);
            encoder.nextFile();
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            encoder.finish();
            // TODO
            //Iterator<Buffer> ite = output.getLastBuffers().iterator();
            //assertEquals("abc", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\r\n", bufferToString(ite.next(), "utf-8"));
            //assertEquals("日本語(Japanese)", bufferToString(ite.next(), "utf-8"));
            //assertEquals("\r\n", bufferToString(ite.next(), "utf-8"));
            //assertFalse(ite.hasNext());
        }
    }

    @Test
    public void testCharset() throws Exception {
        try (MockFileOutput output = new MockFileOutput()) {
            LineEncoder encoder = newEncoder("MS932", "CR", output);
            encoder.nextFile();
            for (String line : new String[] { "abc", "日本語(Japanese)" }) {
                encoder.addLine(line);
            }
            encoder.finish();
            // TODO
            //Iterator<Buffer> ite = output.getLastBuffers().iterator();
            //assertEquals("abc", bufferToString(ite.next(), "MS932"));
            //assertEquals("\r", bufferToString(ite.next(), "MS932"));
            //assertEquals("日本語(Japanese)", bufferToString(ite.next(), "MS932"));
            //assertEquals("\r", bufferToString(ite.next(), "MS932"));
            //assertFalse(ite.hasNext());
        }
    }

    @SuppressWarnings("deprecation")
    private String bufferToString(Buffer buffer, String charset)
            throws UnsupportedEncodingException {
        return new String(buffer.array(), buffer.offset(), buffer.limit(), charset);
    }
}
