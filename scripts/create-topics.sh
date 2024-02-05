#!/bin/bash

    docker exec -i kafka-tools kafka-topics --delete --bootstrap-server broker-1:19092 --topic "source";
    docker exec -i kafka-tools kafka-topics --create --bootstrap-server broker-1:19092 --partitions 1 --replication-factor 1 --topic "source";
    docker exec -i kafka-tools kafka-topics --delete --bootstrap-server broker-1:19092 --topic "destination";
    docker exec -i kafka-tools kafka-topics --create --bootstrap-server broker-1:19092 --partitions 1 --replication-factor 1 --topic "destination";
    docker exec -i kafka-tools kafka-topics --delete --bootstrap-server broker-1:19092 --topic "missing";
    docker exec -i kafka-tools kafka-topics --create --bootstrap-server broker-1:19092 --partitions 1 --replication-factor 1 --topic "missing";
