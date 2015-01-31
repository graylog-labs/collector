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
package com.graylog.agent.file;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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

    @BeforeMethod
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

    @AfterMethod
    public void checkThreadAssertions() {
        assertTrue(setupDone.get(), "Thread " + Thread.currentThread().getName());
        log.info("checking background assertions");
        assertEquals(assertionErrors.get().size(), 0, "Background threads should not fail assertions");
    }
}
