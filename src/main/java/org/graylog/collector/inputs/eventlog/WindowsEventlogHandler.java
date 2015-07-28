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

import org.graylog.collector.Message;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.hyperic.sigar.win32.EventLogNotification;
import org.hyperic.sigar.win32.EventLogRecord;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Strings.isNullOrEmpty;

public class WindowsEventlogHandler implements EventLogNotification {
    private static final Logger LOG = LoggerFactory.getLogger(WindowsEventlogHandler.class);

    private final MessageBuilder messageBuilder;
    private final Buffer buffer;

    public WindowsEventlogHandler(MessageBuilder messageBuilder, Buffer buffer) {
        this.messageBuilder = messageBuilder;
        this.buffer = buffer;
    }

    @Override
    public boolean matches(EventLogRecord eventLogRecord) {
        return true;
    }

    @Override
    public void handleNotification(EventLogRecord record) {
        LOG.debug("EventLogRecord: {}", record);

        final MessageBuilder builder = messageBuilder
                .copy()
                .source(record.getComputerName())
                .message(isNullOrEmpty(record.getMessage()) ? "empty" : record.getMessage().trim())
                .timestamp(getDateTime(record.getTimeGenerated()))
                .level(getMessageLevel(record));

        builder.addField("event_source", record.getSource());
        builder.addField("event_category", record.getCategory());
        builder.addField("event_category_string", record.getCategoryString());
        builder.addField("event_computer_name", record.getComputerName());
        builder.addField("event_id", record.getEventId());
        builder.addField("event_type", record.getEventType());
        builder.addField("event_type_string", record.getEventTypeString());
        builder.addField("event_log_name", record.getLogName());
        builder.addField("event_record_number", record.getRecordNumber());
        builder.addField("event_time_generated", getDateTime(record.getTimeGenerated()).toString());
        builder.addField("event_time_written", getDateTime(record.getTimeWritten()).toString());
        builder.addField("event_user", isNullOrEmpty(record.getUser()) ? "" : record.getUser());

        buffer.insert(builder.build());
    }

    private DateTime getDateTime(long seconds) {
        return new DateTime(seconds * 1000, DateTimeZone.UTC);
    }

    /**
     * Returns the @{code Message.Level} for the given {@code EventLogRecord}.
     *
     * See: https://msdn.microsoft.com/en-us/library/windows/desktop/aa363646(v=vs.85).aspx
     *
     * @param record the eventlog record
     * @return the mapped message level
     */
    private Message.Level getMessageLevel(EventLogRecord record) {
        switch (record.getEventType()) {
            case 1: // EVENTLOG_ERROR_TYPE
                return Message.Level.ERROR;
            case 2: // EVENTLOG_WARNING_TYPE
                return Message.Level.WARNING;
            case 4: // EVENTLOG_INFORMATION_TYPE
                return Message.Level.INFO;
            case 8: // EVENTLOG_AUDIT_SUCCESS
                return Message.Level.INFO;
            case 16: // EVENTLOG_AUDIT_FAILURE
                return Message.Level.ERROR;
            default:
                return Message.Level.INFO;
        }
    }
}
