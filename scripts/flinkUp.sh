#!/bin/bash

docker-compose -p flink -f ./docker/flink-job-cluster.yml up -d
