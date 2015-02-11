#!/bin/bash

AGENT_BIN=$(readlink -f $0)
AGENT_ROOT="$(dirname $(dirname $AGENT_BIN))"
AGENT_DEFAULT_JAR="$AGENT_ROOT/graylog-agent.jar"

JAVA_DEFAULT_OPTS="${agent.jvm-opts}"

if [ -f "${agent.script-config}" ]; then
    source "${agent.script-config}"
fi

AGENT_JAR=${AGENT_JAR:="$AGENT_DEFAULT_JAR"}

JAVA_CMD=${JAVA_CMD:=$(which java)}
JAVA_OPTS="${JAVA_OPTS:="$JAVA_DEFAULT_OPTS"}"

die() {
    echo $*
    exit 1
}

if [ -n "$JAVA_HOME" ]; then
    # Try to use $JAVA_HOME
    if [ -x "$JAVA_HOME"/bin/java ]; then
        JAVA_CMD="$JAVA_HOME"/bin/java
    else
        die "$JAVA_HOME"/bin/java is not executable
    fi
fi

exec $JAVA_CMD $JAVA_OPTS -jar $AGENT_JAR "$@"
