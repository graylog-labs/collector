package com.graylog.agent.outputs.gelf;

import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.Message;
import com.graylog.agent.annotations.AgentOutputFactory;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.outputs.OutputService;
import org.graylog2.gelfclient.GelfConfiguration;
import org.graylog2.gelfclient.GelfMessageBuilder;
import org.graylog2.gelfclient.GelfMessageLevel;
import org.graylog2.gelfclient.GelfTransports;
import org.graylog2.gelfclient.transport.GelfTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class GelfOutput extends OutputService {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    @AgentOutputFactory
    public interface Factory extends OutputService.Factory<GelfOutput, GelfOutputConfiguration> {
        GelfOutput create(GelfOutputConfiguration configuration);
    }

    private final GelfOutputConfiguration configuration;
    private final Buffer buffer;
    private GelfTransport transport;

    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private final CountDownLatch transportInitialized = new CountDownLatch(1);

    @Inject
    public GelfOutput(@Assisted GelfOutputConfiguration configuration, Buffer buffer) {
        this.configuration = configuration;
        this.buffer = buffer;
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    protected void run() throws Exception {
        final GelfConfiguration clientConfig = new GelfConfiguration(configuration.getHost(), configuration.getPort())
                .transport(GelfTransports.TCP)
                .queueSize(configuration.getClientQueueSize())
                .connectTimeout(configuration.getClientConnectTimeout())
                .reconnectDelay(configuration.getClientReconnectDelay())
                .tcpNoDelay(configuration.isClientTcpNoDelay())
                .sendBufferSize(configuration.getClientSendBufferSize());

        LOG.info("Starting GELF transport: {}", clientConfig);
        this.transport = GelfTransports.create(clientConfig);

        transportInitialized.countDown();
        stopLatch.await();

        LOG.debug("Stopping transport {}", transport);
        transport.stop();
    }

    @Override
    public void write(Message message) {
        Uninterruptibles.awaitUninterruptibly(transportInitialized);

        LOG.debug("Sending message: {}", message);

        try {
            final GelfMessageBuilder messageBuilder = new GelfMessageBuilder(message.getMessage(), message.getSource())
                    .level(GelfMessageLevel.INFO);

            transport.send(messageBuilder.build());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getInputs() {
        return configuration.getInputs();
    }
}
