/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.file.splitters;

import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.graylog.collector.file.FileChunkBuffer;
import org.graylog.collector.file.FileChunkMessage;
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.base.Charsets.ISO_8859_1;
import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NewlineChunkSplitterTest {
    @Test
    public void successfulSplitLF() throws Exception {
        final NewlineChunkSplitter splitter = new NewlineChunkSplitter();

        final String logLines = "Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\n";

        final ByteBuf buffer = Unpooled.copiedBuffer(logLines, UTF_8);
        final FileChunkBuffer fileChunkBuffer = new FileChunkBuffer(null, buffer, 0);
        final Iterable<FileChunkMessage> firstTwoChunks = splitter.split(fileChunkBuffer, UTF_8);
        final Iterable<FileChunkMessage> remainingChunk = splitter.splitRemaining(fileChunkBuffer, UTF_8);

        int messageNum = 0;
        for (FileChunkMessage chunk : Iterables.concat(firstTwoChunks, remainingChunk)) {
            switch (++messageNum) {
                case 1:
                    assertEquals("Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page", chunk.getMessageString());
                    assertEquals(0, chunk.getFileOffset());
                    assertEquals(145, chunk.getRawMessageLength());
                    break;
                case 2:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:", chunk.getMessageString());
                    assertEquals(145, chunk.getFileOffset());
                    assertEquals(141, chunk.getRawMessageLength());
                    break;
                case 3:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"", chunk.getMessageString());
                    assertEquals(286, chunk.getFileOffset());
                    assertEquals(150, chunk.getRawMessageLength());
                    break;
            }
        }

        assertEquals("the last chunk should have triggered a message (no follow mode active)", 3, messageNum);
    }

    @Test
    public void successfulSplitCRLF() throws Exception {
        final NewlineChunkSplitter splitter = new NewlineChunkSplitter();

        final String logLines = "Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\r\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\r\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\r\n";

        final ByteBuf buffer = Unpooled.copiedBuffer(logLines, UTF_8);
        final FileChunkBuffer fileChunkBuffer = new FileChunkBuffer(null, buffer, 0);
        final Iterable<FileChunkMessage> firstTwoChunks = splitter.split(fileChunkBuffer, UTF_8);
        final Iterable<FileChunkMessage> remainingChunk = splitter.splitRemaining(fileChunkBuffer, UTF_8);

        int messageNum = 0;
        for (FileChunkMessage chunk : Iterables.concat(firstTwoChunks, remainingChunk)) {
            switch (++messageNum) {
                case 1:
                    assertEquals("Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page", chunk.getMessageString());
                    break;
                case 2:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:", chunk.getMessageString());
                    break;
                case 3:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"", chunk.getMessageString());
                    break;
            }
        }

        assertEquals("the last chunk should have triggered a message (no follow mode active)", 3, messageNum);
    }

    @Test
    public void testEncodings() throws Exception {
        final NewlineChunkSplitter splitter = new NewlineChunkSplitter();

        // "Hällo Wörld\nBüe\n" in ISO-8859-1
        final byte[] bytes = {
                0x48, (byte) 0xe4, 0x6c, 0x6c, 0x6f, 0x20, 0x57, (byte) 0xf6, 0x72, 0x6c, 0x64, 0x0a,
                0x42, (byte) 0xfc, 0x65, 0x0a
        };

        // With correct encoding
        final ByteBuf isoBuffer = Unpooled.copiedBuffer(bytes);
        final FileChunkBuffer fileChunkBuffer = new FileChunkBuffer(null, isoBuffer, 0);
        final Iterator<FileChunkMessage> isoIterator = splitter.split(fileChunkBuffer, ISO_8859_1).iterator();

        assertEquals("Hällo Wörld", isoIterator.next().getMessageString());
        assertEquals("Büe", isoIterator.next().getMessageString());

        // With wrong encoding
        final ByteBuf isoBuffer2 = Unpooled.copiedBuffer(bytes);
        final FileChunkBuffer fileChunkBuffer2 = new FileChunkBuffer(null, isoBuffer2, 0);
        final Iterator<FileChunkMessage> wrongIterator = splitter.split(fileChunkBuffer2, UTF_8).iterator();

        assertNotEquals("Hällo Wörld", wrongIterator.next().getMessageString());
        assertNotEquals("Büe", wrongIterator.next().getMessageString());
    }
}