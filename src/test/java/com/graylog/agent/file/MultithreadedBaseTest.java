package com.graylog.agent.file;

import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultithreadedBaseTest {
    private static final Logger log = LoggerFactory.getLogger(MultithreadedBaseTest.class);
    ThreadLocal<Boolean> setupDone = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private ThreadLocal<Set<AssertionError>> assertionErrors = new ThreadLocal<Set<AssertionError>>() {
        @Override
        protected Set<AssertionError> initialValue() {
            return Sets.newHashSet();
        }
    };

    @Before
    public void assertErrorTrap() {
        log.info("clearing background assertions {}", Thread.currentThread().getName());
        assertionErrors.get().clear();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                if (e instanceof AssertionError) {
                    log.info("assertion failed on thread " + t.getName(), e);
                    assertionErrors.get().add((AssertionError) e);
                }
            }
        });
        setupDone.set(true);
    }

    @After
    public void checkThreadAssertions() {
        assertTrue("Thread " + Thread.currentThread().getName(), setupDone.get());
        log.info("checking background assertions");
        assertEquals("Background threads should not fail assertions", 0, assertionErrors.get().size());
    }
}
