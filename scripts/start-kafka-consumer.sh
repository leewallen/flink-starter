#!/bin/bash


docker exec -i kafka-tools kafka-console-consumer --bootstrap-server broker-1:19092 --topic destination --property "parse.key=true" --property "key.separator=:"
