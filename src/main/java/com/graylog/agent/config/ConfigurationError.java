package com.graylog.agent.config;

public class ConfigurationError {
    private final String messsage;

    public ConfigurationError(String messsage) {
        this.messsage = messsage;
    }

    public String getMesssage() {
        return messsage;
    }
}