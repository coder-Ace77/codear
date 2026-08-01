#!/bin/bash

IMAGE="acecoder121/codear-microservices:engine"

docker build -t $IMAGE .
docker push $IMAGE
