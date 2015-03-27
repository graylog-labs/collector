package com.graylog.agent.file.splitters;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.util.Map;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;

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
        final ByteBuf buffer = Unpooled.copiedBuffer(logLines, UTF_8);
        final Iterable<String> firstTwoChunks = splitter.split(buffer);
        final Iterable<String> remainingChunk = splitter.splitRemaining(buffer);

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
}
