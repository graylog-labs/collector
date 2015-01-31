/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.file.splitters;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.graylog.agent.file.compat.Configuration;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.testng.annotations.Test;

import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class PatternChunkSplitterTest {

    @Test
    public void successfulSplit() {
        final PatternChunkSplitter splitter = new PatternChunkSplitter();
        Map<String, Object> values = Maps.newHashMap();
        values.put(PatternChunkSplitter.CK_SPLITTER_PATTERN, "^(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)");
        splitter.configure(new Configuration(values));

        String logLines = "Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\n" +
                "\t\tcom.google.Keystone.Daemon.UpdateEngine: 2 connection(s)\n" +
                "\t\tcom.google.Keystone.Daemon.Administration: 0 connection(s)\n" +
                "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\n";
        final ChannelBuffer buffer = ChannelBuffers.copiedBuffer(logLines, UTF_8);
        final Iterable<String> firstTwoChunks = splitter.split(buffer);
        final Iterable<String> remainingChunk = splitter.splitRemaining(buffer);

        int messageNum = 0;
        for (String chunk : Iterables.concat(firstTwoChunks, remainingChunk)) {
            switch (++messageNum) {
                case 1:
                    assertEquals(chunk, "Feb 20 17:05:18 otter kernel[0]: CODE SIGNING: cs_invalid_page(0x1000): p=32696[GoogleSoftwareUp] final status 0x0, allow (remove VALID)ing page\n");
                    break;
                case 2:
                    assertEquals(chunk, "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KeystoneDaemon logServiceState] GoogleSoftwareUpdate daemon (1.1.0.3659) vending:\n" +
                            "\t\tcom.google.Keystone.Daemon.UpdateEngine: 2 connection(s)\n" +
                            "\t\tcom.google.Keystone.Daemon.Administration: 0 connection(s)\n");
                    break;
                case 3:
                    assertEquals(chunk, "Feb 20 17:05:18 otter GoogleSoftwareUpdateDaemon[32697]: -[KSUpdateEngine updateProductID:] KSUpdateEngine updating product ID: \"com.google.Keystone\"\n");
                    break;
            }
        }

        assertEquals(messageNum, 3, "the last chunk should have triggered a message (no follow mode active)");


    }
}
