#!/bin/bash
docker exec -it kafka-cluster kafka-topics --create --topic log-parser-input-topic --replication-factor 1 --partitions 3  --zookeeper localhost:2181

docker exec -it kafka-cluster kafka-topics --create --topic log-parser-output-aggregation-topic --replication-factor 1 --partitions 3  --zookeeper localhost:2181

docker exec -it kafka-cluster kafka-topics --create --topic log-parser-output-alert-topic --replication-factor 1 --partitions 3  --zookeeper localhost:2181