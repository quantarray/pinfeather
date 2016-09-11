#!/bin/bash

source ./pinfeather-monitor-version.sh

if [ -z $1 ]; then
  echo "Supply name of configuration resource name as the first parameter, e.g. xyz."
  exit 1
fi

CONFIG_RESOURCE_NAME=$1

JAVA_OPTS="-Dconfig.resource=${CONFIG_RESOURCE_NAME}.application.conf"

JAVA_CLASSPATH=".:pinfeather-monitor/target/scala-${SCALA_VERSION}/pinfeather-monitor-assembly-${ASSEMBLY_VERSION}.jar"

java ${JAVA_OPTS} -cp ${JAVA_CLASSPATH} com.quantarray.pinfeather.monitor.PinFeatherMonitor

RESULT=$?

exit ${RESULT}