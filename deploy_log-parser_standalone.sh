#!/bin/bash

SCRIPT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

${SPARK_HOME}/bin/spark-submit \
  --class com.michael.log.parser.SparkLogParserJob \
  --name log-parser-aggregator\
  --master spark://localhost:7077\
  --properties-file log-parser.conf \
  --conf spark.executor.extraJavaOptions=-Dlog4j.configuration=log4j.xml \
  ${SCRIPT_HOME}/target/scala-2.11/spark-streaming-task-assembly-0.1.jar \
  "$@"