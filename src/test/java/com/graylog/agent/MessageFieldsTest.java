package com.graylog.agent;

import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

public class MessageFieldsTest {
    @Test
    public void testFields() {
        final MessageFields fields = new MessageFields();

        fields.put("bool", true);
        fields.put("int", 123);
        fields.put("long", 1500L);
        fields.put("string", "string");

        final HashMap<String, Object> map = new HashMap<String, Object>() {
            {
                put("bool", true);
                put("int", 123);
                put("long", 1500L);
                put("string", "string");
            }
        };

        assertEquals(map, fields.asMap());
    }
}