Graylog Agent
=============

[![Build Status](https://travis-ci.org/Graylog2/agent.svg?branch=master)](https://travis-ci.org/Graylog2/agent)

This is the Graylog Agent.

## Usage


### Configuration

```
message-buffer-size = 128

inputs {
  local-syslog {
    type = "file"
    path = "/var/log/syslog"
  }
  apache-access {
    type = "file"
    path = "/var/log/apache2/access.log"
    outputs = "gelf-tcp,console"
  }
  test-log {
    type = "file"
    path = "logs/file.log"
  }
}

outputs {
  gelf-tcp {
    type = "gelf"
    protocol = "tcp"
    host = "127.0.0.1"
    port = 12201
    client-queue-size = 512
    client-connect-timeout = 5000
    client-reconnect-delay = 1000
    client-tcp-no-delay = true
    client-send-buffer-size = 32768
    inputs = "test-log"
  }
  console {
    type = "stdout"
  }
}
```

### Running The Agent

#### Linux

The agent needs a configuration file and can be started with the following command.

```
$ bin/graylog-agent server -f agent.conf
2015-02-09T18:30:30.233+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Running org.graylog.collector.cli.commands.Server
2015-02-09T18:30:30.610+0100 INFO  [GelfOutput] c.g.agent.outputs.gelf.GelfOutput - Starting GELF transport: org.graylog2.gelfclient.GelfConfiguration@53847a91
2015-02-09T18:30:30.619+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Service RUNNING: BufferProcessor [RUNNING]
2015-02-09T18:30:30.621+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Service RUNNING: StdoutOutput{id='console', inputs=''}
2015-02-09T18:30:30.623+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Service RUNNING: GelfOutput{port='12201', id='gelf-tcp', protocol='tcp', client-send-buffer-size='32768', host='127.0.0.1', inputs='local-syslog,test-log', client-reconnect-delay='1000', client-connect-timeout='5000', client-tcp-no-delay='true', client-queue-size='512'}
2015-02-09T18:30:30.623+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Service RUNNING: FileInput{id='local-syslog', path='/var/log/syslog', outputs=''}
2015-02-09T18:30:30.624+0100 INFO  [main] c.graylog.agent.cli.commands.Server - Service RUNNING: FileInput{id='test-log', path='logs/file.log', outputs=''}
```

## Building

The following command can be used to build the fat JAR and tarball/zip packages.

```
$ mvn package assembly:single
```

Find the artifacts in the following places.

* JAR `target/graylog-agent-*.jar`
* TAR `target/assembly/graylog-agent-*.tar.gz`
* TAR `target/assembly/graylog-agent-*.zip`
