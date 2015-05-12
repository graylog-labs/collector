#!/bin/bash

JAVA_DEFAULT_OPTS="${collector.jvm-opts}"
COLLECTOR_DEFAULT_JAR="${collector.jar-path}"

# For Debian/Ubuntu based systems.
if [ -f "/etc/default/graylog-collector" ]; then
    source "/etc/default/graylog-collector"
fi

# For RedHat/Fedora based systems.
if [ -f "/etc/sysconfig/graylog-collector" ]; then
    source "/etc/sysconfig/graylog-collector"
fi
