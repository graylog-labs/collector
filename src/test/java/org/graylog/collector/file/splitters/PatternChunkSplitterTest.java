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
import org.junit.Test;

import java.util.Iterator;

import static com.google.common.base.Charsets.ISO_8859_1;
import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PatternChunkSplitterTest {
    @Test
    public void successfulSplit() {
        final PatternChunkSplitter splitter = new PatternChunkSplitter("^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)");

        String logLines = "Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\n" +
                "\t\tcom.google.Keystone.Daemon.UpdateEngine: 2 connection(s)\n" +
                "\t\tcom.google.Keystone.Daemon.Administration: 0 connection(s)\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\n";
        final ByteBuf buffer = Unpooled.copiedBuffer(logLines, UTF_8);
        final Iterable<String> firstTwoChunks = splitter.split(buffer, UTF_8);
        final Iterable<String> remainingChunk = splitter.splitRemaining(buffer, UTF_8);

        int messageNum = 0;
        for (String chunk : Iterables.concat(firstTwoChunks, remainingChunk)) {
            switch (++messageNum) {
                case 1:
                    assertEquals("Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\n", chunk);
                    break;
                case 2:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\n" +
                                    "\t\tcom.google.Keystone.Daemon.UpdateEngine: 2 connection(s)\n" +
                                    "\t\tcom.google.Keystone.Daemon.Administration: 0 connection(s)\n",
                            chunk);
                    break;
                case 3:
                    assertEquals("Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\n", chunk);
                    break;
            }
        }

        assertEquals("the last chunk should have triggered a message (no follow mode active)", 3, messageNum);
    }

    @Test
    public void testEncodings() throws Exception {
        final PatternChunkSplitter splitter = new PatternChunkSplitter("^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)");

        // "Feb 20 17:05:18 Hällö Wörld\nFeb 20 17:05:18 Büe\n" in ISO-8859-1 encoding
        final byte[] bytes = new byte[]{
                0x46, 0x65, 0x62, 0x20, 0x32, 0x30, 0x20, 0x31, 0x37, 0x3a, 0x30, 0x35, 0x3a, 0x31, 0x38, 0x20,
                0x48, (byte) 0xe4, 0x6c, 0x6c, (byte) 0xf6, 0x20, 0x57, (byte) 0xf6, 0x72, 0x6c, 0x64, 0x0a,
                0x46, 0x65, 0x62, 0x20, 0x32, 0x30, 0x20, 0x31, 0x37, 0x3a, 0x30, 0x35, 0x3a, 0x31, 0x38, 0x20,
                0x42, (byte) 0xfc, 0x65, 0x0a
        };

        // With correct encoding
        final ByteBuf buffer = Unpooled.copiedBuffer(bytes);
        final Iterator<String> iterator = splitter.splitRemaining(buffer, ISO_8859_1).iterator();

        assertEquals("Feb 20 17:05:18 Hällö Wörld\n", iterator.next());
        assertEquals("Feb 20 17:05:18 Büe\n", iterator.next());

        // With wrong encoding
        final ByteBuf buffer2 = Unpooled.copiedBuffer(bytes);
        final Iterator<String> iterator2 = splitter.splitRemaining(buffer2, UTF_8).iterator();

        assertNotEquals("Feb 20 17:05:18 Hällö Wörld\n", iterator2.next());
        assertNotEquals("Feb 20 17:05:18 Büe\n", iterator2.next());
    }
}
