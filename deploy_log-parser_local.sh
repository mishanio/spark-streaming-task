#!/bin/bash

SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

${SPARK_HOME}/bin/spark-submit \
  --class "com.michael.log.parser.SparkLogParserJob" \
  --master "local[2]" \
  --properties-file log-parser.conf \
  ${SCRIPT_HOME}/target/scala-2.11/spark-streaming-task-assembly-0.1.jar \
  "$@"
