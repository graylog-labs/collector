#!/bin/bash

JAVA_DEFAULT_OPTS="${agent.jvm-opts}"
AGENT_DEFAULT_JAR="${agent.jar-path}"

# For Debian/Ubuntu based systems.
if [ -f "/etc/default/graylog-agent" ]; then
    source "/etc/default/graylog-agent"
fi

# For RedHat/Fedora based systems.
if [ -f "/etc/sysconfig/graylog-agent" ]; then
    source "/etc/sysconfig/graylog-agent"
fi
