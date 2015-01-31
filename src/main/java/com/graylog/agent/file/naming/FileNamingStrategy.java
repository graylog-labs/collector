package com.graylog.agent.file.naming;

import java.nio.file.Path;

public interface FileNamingStrategy {

    boolean pathMatches(Path path);

}
