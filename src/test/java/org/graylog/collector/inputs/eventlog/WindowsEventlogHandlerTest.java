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
package org.graylog.collector.inputs.eventlog;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.graylog.collector.Message;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.hyperic.sigar.win32.EventLogRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WindowsEventlogHandlerTest {
    private static class TestBuffer implements Buffer {
        private final List<Message> messages = Lists.newArrayList();

        @Override
        public void insert(final Message message) {
            messages.add(message);
        }

        @Override
        public Message remove() {
            return null;
        }

        public void clear() {
            messages.clear();
        }

        public List<Message> getMessages() {
            return messages;
        }
    }

    private TestBuffer buffer;
    private MessageBuilder messageBuilder;
    private EventLogRecord record;


    @Before
    public void setUp() {
        this.buffer = new TestBuffer();
        this.messageBuilder = buildMessageBuilder();
        this.record = buildEventLogRecord();
    }

    public MessageBuilder buildMessageBuilder() {
        final MessageBuilder messageBuilder = new MessageBuilder();

        messageBuilder.input("input-id");
        messageBuilder.outputs(Sets.newHashSet("output-id"));

        return messageBuilder;
    }

    public EventLogRecord buildEventLogRecord() {
        final EventLogRecord record = mock(EventLogRecord.class);

        when(record.getMessage()).thenReturn("The log message");
        when(record.getSource()).thenReturn("Service Control Manager");
        when(record.getCategory()).thenReturn((short) 0);
        when(record.getCategoryString()).thenReturn("None");
        when(record.getComputerName()).thenReturn("IE10Win7");
        when(record.getEventId()).thenReturn(4567L);
        when(record.getEventType()).thenReturn((short) 4);
        when(record.getEventTypeString()).thenReturn("Information");
        when(record.getLogName()).thenReturn("System");
        when(record.getRecordNumber()).thenReturn(1234L);
        when(record.getTimeGenerated()).thenReturn(0L);
        when(record.getTimeWritten()).thenReturn(1L);
        when(record.getUser()).thenReturn(null);

        return record;
    }

    @Test
    public void test() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        handler.handleNotification(record);

        assertEquals(1, buffer.getMessages().size());

        final Message message = buffer.getMessages().get(0);

        assertEquals("The log message", message.getMessage());
        assertEquals("IE10Win7", message.getSource());
        assertEquals(Message.Level.INFO, message.getLevel());
        assertEquals(new DateTime(0L).withZone(DateTimeZone.UTC), message.getTimestamp());

        final Map<String, Object> fields = message.getFields().asMap();

        assertEquals("Service Control Manager", fields.get("event_source"));
        assertEquals(0, fields.get("event_category"));
        assertEquals("None", fields.get("event_category_string"));
        assertEquals("IE10Win7", fields.get("event_computer_name"));
        assertEquals(4567L, fields.get("event_id"));
        assertEquals(4, fields.get("event_type"));
        assertEquals("Information", fields.get("event_type_string"));
        assertEquals("System", fields.get("event_log_name"));
        assertEquals(1234L, fields.get("event_record_number"));
        assertEquals("1970-01-01T00:00:00.000Z", fields.get("event_time_generated"));
        assertEquals("1970-01-01T00:00:01.000Z", fields.get("event_time_written"));
        assertEquals("", fields.get("event_user"));
    }

    @Test
    public void testWithEmptyLogMessage() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        when(record.getMessage()).thenReturn("");

        handler.handleNotification(record);

        final Message message = buffer.getMessages().get(0);

        assertEquals("empty", message.getMessage());
    }

    @Test
    public void testWithNullLogMessage() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        when(record.getMessage()).thenReturn(null);

        handler.handleNotification(record);

        final Message message = buffer.getMessages().get(0);

        assertEquals("empty", message.getMessage());
    }

    @Test
    public void testWithEmptyUser() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        when(record.getUser()).thenReturn("");

        handler.handleNotification(record);

        final Message message = buffer.getMessages().get(0);

        assertEquals("", message.getFields().asMap().get("event_user"));
    }

    @Test
    public void testWithNullUser() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        when(record.getUser()).thenReturn(null);

        handler.handleNotification(record);

        final Message message = buffer.getMessages().get(0);

        assertEquals("", message.getFields().asMap().get("event_user"));
    }

    @Test
    public void testMessageLevelMapping() throws Exception {
        final WindowsEventlogHandler handler = new WindowsEventlogHandler(messageBuilder, buffer);

        when(record.getEventType()).thenReturn((short) 1);
        handler.handleNotification(record);

        when(record.getEventType()).thenReturn((short) 2);
        handler.handleNotification(record);

        when(record.getEventType()).thenReturn((short) 4);
        handler.handleNotification(record);

        when(record.getEventType()).thenReturn((short) 8);
        handler.handleNotification(record);

        when(record.getEventType()).thenReturn((short) 16);
        handler.handleNotification(record);

        final Message message = buffer.getMessages().get(0);

        assertEquals(Message.Level.ERROR, buffer.getMessages().get(0).getLevel());
        assertEquals(Message.Level.WARNING, buffer.getMessages().get(1).getLevel());
        assertEquals(Message.Level.INFO, buffer.getMessages().get(2).getLevel());
        assertEquals(Message.Level.INFO, buffer.getMessages().get(3).getLevel());
        assertEquals(Message.Level.ERROR, buffer.getMessages().get(4).getLevel());
    }
}