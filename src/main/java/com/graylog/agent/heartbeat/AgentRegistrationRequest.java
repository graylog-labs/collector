package com.graylog.agent.heartbeat;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@AutoValue
@JsonAutoDetect
public abstract class AgentRegistrationRequest {
    @JsonProperty("node_id")
    @NotNull
    @Size(min = 1)
    public abstract String nodeId();

    @JsonProperty("node_details")
    public abstract AgentNodeDetailsSummary nodeDetails();

    @JsonCreator
    public static AgentRegistrationRequest create(@JsonProperty("node_id") String nodeId,
                                                  @JsonProperty("node_details") @Valid AgentNodeDetailsSummary nodeDetails) {
        return new AutoValue_AgentRegistrationRequest(nodeId, nodeDetails);
    }
}
