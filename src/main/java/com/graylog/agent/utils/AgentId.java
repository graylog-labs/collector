package com.graylog.agent.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Singleton
public class AgentId {
    private static final Logger LOG = LoggerFactory.getLogger(AgentId.class);

    private final String id;

    @Inject
    public AgentId(AgentIdConfiguration config) {
        final String configuredAgentId = config.getAgentId();
        if (configuredAgentId.startsWith("file:")) {
            final String[] splittedConfig = configuredAgentId.split("^file:");
            if (splittedConfig.length < 2)
                throw new RuntimeException("Invalid specified file location for agent id: " + configuredAgentId);
            this.id = readOrGenerate(splittedConfig[1]);
        } else {
            this.id = configuredAgentId;
        }
    }

    private String readOrGenerate(String filename) {
        try {
            String read = read(filename);

            if (read == null || read.isEmpty()) {
                return generate(filename);
            }

            LOG.info("Agent ID: {}", read);
            return read;
        } catch (FileNotFoundException | NoSuchFileException e) {
            return generate(filename);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read node id from file: ", e);
        }
    }

    private String read(String filename) throws IOException {
        final List<String> lines = Files.readAllLines(Paths.get(filename), StandardCharsets.UTF_8);

        return lines.size() > 0 ? lines.get(0) : "";
    }

    private String generate(String filename) {
        String generated = this.randomId();
        LOG.info("No node ID file found. Generated: {}", generated);

        try {
            persist(generated, filename);
        } catch (IOException e1) {
            LOG.debug("Could not persist node ID: ", e1);
            throw new RuntimeException("Unable to persist node ID", e1);
        }

        return generated;
    }

    private void persist(String nodeId, String filename) throws IOException {
        final Path path = new File(filename).getAbsoluteFile().toPath();

        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        Files.write(path, nodeId.getBytes());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return id;
    }


    private String randomId() {
        return UUID.randomUUID().toString();
    }
}
