#!/bin/bash

IMAGE="acecoder121/codear-microservices:problem"

docker build -t $IMAGE .
docker login
docker push $IMAGE
