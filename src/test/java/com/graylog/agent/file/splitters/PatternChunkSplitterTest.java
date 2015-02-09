package com.graylog.agent.file.splitters;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
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
        splitter.configure(new ContentSplitterConfiguration(values));

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
