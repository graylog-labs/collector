package com.graylog.agent.inputs.eventlog;

import com.graylog.agent.Message;
import com.graylog.agent.MessageBuilder;
import com.graylog.agent.MessageFields;
import com.graylog.agent.buffer.Buffer;
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

        final MessageFields fields = new MessageFields();

        fields.put("event_source", record.getSource());
        fields.put("event_category", record.getCategory());
        fields.put("event_category_string", record.getCategoryString());
        fields.put("event_computer_name", record.getComputerName());
        fields.put("event_id", record.getEventId());
        fields.put("event_type", record.getEventType());
        fields.put("event_type_string", record.getEventTypeString());
        fields.put("event_log_name", record.getLogName());
        fields.put("event_record_number", record.getRecordNumber());
        fields.put("event_time_generated", getDateTime(record.getTimeGenerated()).toString());
        fields.put("event_time_written", getDateTime(record.getTimeWritten()).toString());
        fields.put("event_user", isNullOrEmpty(record.getUser()) ? "" : record.getUser());

        final Message message = messageBuilder
                .copy()
                .source(record.getComputerName())
                .message(isNullOrEmpty(record.getMessage()) ? "empty" : record.getMessage().trim())
                .timestamp(getDateTime(record.getTimeGenerated()))
                .level(getMessageLevel(record))
                .fields(fields)
                .build();

        buffer.insert(message);
    }

    private DateTime getDateTime(long seconds) {
        return new DateTime(seconds * 1000).withZone(DateTimeZone.UTC);
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
