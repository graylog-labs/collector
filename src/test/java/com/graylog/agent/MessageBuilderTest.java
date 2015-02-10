package com.graylog.agent;

import com.google.common.collect.Sets;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class MessageBuilderTest {
    @Test
    public void testSuccessfulBuild() throws Exception {
        final DateTime time = DateTime.now();
        final MessageFields fields = new MessageFields();

        fields.put("hello", "world");

        final Message message = new MessageBuilder()
                .message("the message")
                .source("source")
                .timestamp(time)
                .input("input-id")
                .outputs(Sets.newHashSet("output1", "output2"))
                .fields(fields)
                .build();

        assertEquals(message.getMessage(), "the message");
        assertEquals(message.getSource(), "source");
        assertEquals(message.getTimestamp(), time);
        assertEquals(message.getInput(), "input-id");
        assertEquals(message.getOutputs(), Sets.newHashSet("output1", "output2"));
        assertEquals(message.getFields().asMap(), new HashMap<String, Object>(){
            { put("hello", "world"); }
        });
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testEmpty() throws Exception {
        new MessageBuilder().build();
    }

    @Test
    public void testValidations() throws Exception {
        final MessageBuilder builder = new MessageBuilder()
                .message("the message")
                .source("source")
                .timestamp(DateTime.now())
                .input("input-id")
                .outputs(Sets.newHashSet("output1", "output2"))
                .fields(new MessageFields());

        builder.message(null);
        ensureFailure(builder, "message");
        builder.message("the message");

        builder.source(null);
        ensureFailure(builder, "source");
        builder.source("source");

        builder.timestamp(null);
        ensureFailure(builder, "timestamp");
        builder.timestamp(DateTime.now());

        builder.input(null);
        ensureFailure(builder, "input");
        builder.input("input-id");

        builder.outputs(null);
        ensureFailure(builder, "outputs");
        builder.outputs(Sets.newHashSet("output1", "output2"));

        builder.build();
    }

    @Test
    public void testCopy() throws Exception {
        final MessageBuilder builder = new MessageBuilder()
                .message("the message")
                .source("source")
                .timestamp(DateTime.now())
                .input("input-id")
                .outputs(Sets.newHashSet("output1", "output2"))
                .fields(new MessageFields());
        final MessageBuilder copy = builder.copy();

        assertNotEquals(copy, builder);
    }

    @Test
    public void testThreadAwareness() throws Exception {
        // The builder should throw an error if modified by a different thread than the owning one.
        final MessageBuilder builder = new MessageBuilder()
                .message("the message")
                .source("source")
                .timestamp(DateTime.now())
                .input("input-id")
                .outputs(Sets.newHashSet("output1", "output2"))
                .fields(new MessageFields());

        builder.message("modified by owner thread");
        modifyInThread("message", new Runnable() {
            @Override
            public void run() {
                builder.message("modified by another thread");
            }
        });

        builder.source("modified by owner thread");
        modifyInThread("source", new Runnable() {
            @Override
            public void run() {
                builder.source("modified by another thread");
            }
        });

        builder.timestamp(DateTime.now());
        modifyInThread("timestamp", new Runnable() {
            @Override
            public void run() {
                builder.timestamp(DateTime.now());
            }
        });

        builder.input("modified by owner thread");
        modifyInThread("input", new Runnable() {
            @Override
            public void run() {
                builder.input("modified by another thread");
            }
        });

        builder.outputs(Sets.newHashSet("modified by owner thread"));
        modifyInThread("outputs", new Runnable() {
            @Override
            public void run() {
                builder.outputs(Sets.newHashSet("modified by another thread"));
            }
        });

        builder.fields(new MessageFields());
        modifyInThread("fields", new Runnable() {
            @Override
            public void run() {
                builder.fields(new MessageFields());
            }
        });
    }

    @Test
    public void testCopyCanBeModifiedInThread() throws Exception {
        final MessageBuilder builder = new MessageBuilder()
                .message("the message")
                .source("source")
                .timestamp(DateTime.now())
                .input("input-id")
                .outputs(Sets.newHashSet("output1", "output2"))
                .fields(new MessageFields());

        builder.message("modified by owner thread");

        final AtomicBoolean failed = new AtomicBoolean(true);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    builder.copy().message("modified by another thread");
                    failed.set(false);
                } catch (IllegalStateException ignored) {
                }
            }
        });

        thread.start();
        thread.join();

        assertFalse(failed.get(), "Modifying builder copy in another thread should have failed!");
    }

    private void modifyInThread(final String field, final Runnable runnable) throws InterruptedException {
        final AtomicBoolean failed = new AtomicBoolean(true);
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                    failed.set(false);
                } catch (IllegalStateException ignored) {
                }
            }
        });

        thread.start();
        thread.join();

        assertTrue(failed.get(), "Modifying " + field + " in another thread should have failed!");
    }

    private void ensureFailure(MessageBuilder builder, String field) {
        try {
            builder.build();
            fail("Builder should fail with " + field + " == null!");
        } catch (Exception ignored) {
        }
    }
}