package com.graylog.agent.inputs;

import java.util.List;

public interface InputConfiguration {
    List<ConfigurationError> validate();

    public static class ConfigurationError {
        private final String messsage;

        public ConfigurationError(String messsage) {
            this.messsage = messsage;
        }

        public String getMesssage() {
            return messsage;
        }
    }
}
