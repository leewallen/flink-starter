#!/bin/bash

docker-compose -p kafka -f ./docker/kafka-cluster.yml up -d
