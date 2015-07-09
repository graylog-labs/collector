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
package org.graylog.collector.utils;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.SharedSecrets;
import sun.misc.VM;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MemoryReporterService extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(MemoryReporterService.class);
    private final MemoryReporterServiceConfiguration config;
    private ScheduledExecutorService scheduler = null;
    private ScheduledFuture<?> scheduledJob = null;

    @Inject
    public MemoryReporterService(final MemoryReporterServiceConfiguration config) {
        this.config = config;
    }

    @Override
    protected void doStart() {
        if (config.isEnable()) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(
                    new ThreadFactoryBuilder()
                            .setDaemon(true)
                            .setNameFormat("memory-reporter-thread")
                            .setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                                @Override
                                public void uncaughtException(Thread t, Throwable e) {
                                    log.error("Problem in memory reporter", e);
                                }
                            })
                            .build());

            this.scheduledJob = this.scheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    reportDirectMemory();
                    reportGC();
                    reportMemoryPool();
                    reportMemoryUsage();
                }
            }, config.getInterval(), config.getInterval(), TimeUnit.MILLISECONDS);
        }

        notifyStarted();
    }

    @Override
    protected void doStop() {
        if (scheduledJob != null) {
            scheduledJob.cancel(false);
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        notifyStopped();
    }

    private void reportDirectMemory() {
        log.info("Direct Memory Usage - {} MB (max {} MB)",
                SharedSecrets.getJavaNioAccess().getDirectBufferPool().getMemoryUsed() / 1024.0 / 1024.0,
                VM.maxDirectMemory() / 1024.0 / 1024.0);
    }

    private void reportGC() {
        for (final GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            log.info("GC {} - count={} time={} pools=\"{}\"", gc.getName(), gc.getCollectionCount(),
                    gc.getCollectionTime(), Joiner.on(',').join(gc.getMemoryPoolNames()));
        }
    }

    private void reportMemoryPool() {
        for (final MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            log.info("Memory Pool {} - type=\"{}\" memory-manager=\"{}\"", pool.getName(), pool.getType(), Joiner.on(',').join(pool.getMemoryManagerNames()));
            log.info("Memory Pool {} - Usage            {}", pool.getName(), pool.getUsage());
            log.info("Memory Pool {} - Collection Usage {}", pool.getName(), pool.getCollectionUsage());
            log.info("Memory Pool {} - Peak Usage       {}", pool.getName(), pool.getPeakUsage());
            log.info("Memory Pool {} - Type             {}", pool.getName(), pool.getPeakUsage());
        }
    }

    private void reportMemoryUsage() {
        final MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        final MemoryUsage nonHeapUsage = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage();

        log.info("Memory Usage Heap     - init={} max={} used={} committed={}", usage.getInit(), usage.getMax(),
                usage.getUsed(), usage.getCommitted());
        log.info("Memory Usage Non-Heap - init={} max={} used={} committed={}", nonHeapUsage.getInit(),
                nonHeapUsage.getMax(), nonHeapUsage.getUsed(), nonHeapUsage.getCommitted());
    }
}
