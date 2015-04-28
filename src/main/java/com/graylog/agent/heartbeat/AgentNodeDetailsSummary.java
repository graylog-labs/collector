package com.graylog.agent.heartbeat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@AutoValue
@JsonAutoDetect
public abstract class AgentNodeDetailsSummary {

    @JsonProperty("operating_system")
    @NotNull
    @Size(min = 1)
    public abstract String operatingSystem();

    @JsonCreator
    public static AgentNodeDetailsSummary create(@JsonProperty("operating_system") String operatingSystem) {
        return new AutoValue_AgentNodeDetailsSummary(operatingSystem);
    }
}
