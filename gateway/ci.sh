#!/bin/bash

IMAGE="acecoder121/codear-microservices:gateway"

docker build -t $IMAGE .
docker login
docker push $IMAGE
