#!/bin/bash

COLLECTOR_BIN=$(readlink -f $0)
COLLECTOR_ROOT="$(dirname $(dirname $COLLECTOR_BIN))"
COLLECTOR_DEFAULT_JAR="$COLLECTOR_ROOT/graylog-collector.jar"

JAVA_DEFAULT_OPTS="${collector.jvm-opts}"

if [ -f "${collector.script-config}" ]; then
    source "${collector.script-config}"
fi

COLLECTOR_JAR=${COLLECTOR_JAR:="$COLLECTOR_DEFAULT_JAR"}

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

exec $JAVA_CMD $JAVA_OPTS -jar $COLLECTOR_JAR "$@"
