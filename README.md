Graylog Colletor
================

[![Build Status](https://travis-ci.org/Graylog2/collector.svg?branch=master)](https://travis-ci.org/Graylog2/collector)

This is the Graylog Collector.

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

### Running The Collector

#### Linux

The collector needs a configuration file and can be started with the following command.

```
$ bin/graylog-collector run -f collector.conf
2015-05-12T16:00:10.841+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Starting Collector v0.2.0-SNAPSHOT (commit a2ad8c8)
2015-05-12T16:00:11.489+0200 INFO  [main] o.g.collector.utils.CollectorId - Collector ID: cf4734f7-01d6-4974-a957-cb71bbd826b7
2015-05-12T16:00:11.505+0200 INFO  [GelfOutput] o.g.c.outputs.gelf.GelfOutput - Starting GELF transport: org.graylog2.gelfclient.GelfConfiguration@3952e37e
2015-05-12T16:00:11.512+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: BufferProcessor [RUNNING]
2015-05-12T16:00:11.513+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: MetricService [RUNNING]
2015-05-12T16:00:11.515+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: FileInput{id='local-syslog', path='/var/log/syslog', charset='UTF-8', outputs='', content-splitter='NEWLINE'}
2015-05-12T16:00:11.516+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: GelfOutput{port='12201', id='gelf-tcp', client-send-buffer-size='32768', host='127.0.0.1', inputs='', client-reconnect-delay='1000', client-connect-timeout='5000', client-tcp-no-delay='true', client-queue-size='512'}
2015-05-12T16:00:11.516+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: HeartbeatService [RUNNING]
2015-05-12T16:00:11.516+0200 INFO  [main] o.graylog.collector.cli.commands.Run - Service RUNNING: StdoutOutput{id='console', inputs=''}
```

## Building

The following command can be used to build the fat JAR and tarball/zip packages.

```
$ mvn package assembly:single
```

Find the artifacts in the following places.

* JAR `target/graylog-collector-*.jar`
* TAR `target/assembly/graylog-collector-*.tar.gz`
* TAR `target/assembly/graylog-collector-*.zip`
