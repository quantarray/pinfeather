#!/bin/bash

source ./pinfeather-monitor-version.sh

sbt pinfeather-monitor/assembly
tar cvf pinfeather-monitor.tar \
    pinfeather-monitor/target/scala-${SCALA_VERSION}/pinfeather-monitor-assembly-${ASSEMBLY_VERSION}.jar \
    application.conf.template \
    .httpAuth.template \
    pinfeather-monitor-version.sh \
    pinfeather-monitor.sh
gzip -f pinfeather-monitor.tar
