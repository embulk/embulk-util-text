/*
 * Copyright 2018 The Embulk project
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class TestLineReader {
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithoutDelimiter() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", null, 256);
        assertEquals(Arrays.asList("test1", "test2", "test3", "test4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCR() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CR, 256);
        assertEquals(Arrays.asList("test1", "test2\ntest3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterLF() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.LF, 256);
        assertEquals(Arrays.asList("test1\rtest2", "test3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRLF() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CRLF, 256);
        assertEquals(Arrays.asList("test1\rtest2\ntest3", "test4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterAndSmallBuffer() throws IOException {
        List<String> lines = readLines("test1\rtest2\ntest3\r\ntest4", LineDelimiter.CR, 1);
        assertEquals(Arrays.asList("test1", "test2\ntest3\r\ntest4"), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\r\rtest2\r", LineDelimiter.CR, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterLFWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\n\ntest2\n", LineDelimiter.LF, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterCRLFWithEmptyLine() throws IOException {
        List<String> lines = readLines("test1\r\n\r\ntest2\r\n", LineDelimiter.CRLF, 256);
        assertEquals(Arrays.asList("test1", "", "test2", ""), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithoutDelimiterAndEmptyString() throws IOException {
        List<String> lines = readLines("", null, 256);
        assertEquals(Collections.emptyList(), lines);
    }

    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    @Test
    public void testReadLineWithDelimiterAndEmptyString() throws IOException {
        List<String> lines = readLines("", LineDelimiter.CR, 256);
        assertEquals(Collections.emptyList(), lines);
    }

    private static List<String> readLines(String text, LineDelimiter lineDelimiter, int bufferSize) throws IOException {
        BufferedReader reader = LineReader.of(new StringReader(text), lineDelimiter, bufferSize);
        List<String> result = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line);
        }
        return result;
    }
}
