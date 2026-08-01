#!/bin/bash

IMAGE="acecoder121/codear-microservices:problem"

docker build -t $IMAGE .
docker push $IMAGE
