package com.graylog.agent.outputs;

import java.util.Set;

public interface Output {
    String getId();

    Set<String> getInputs();
}
