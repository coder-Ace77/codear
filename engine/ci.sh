#!/bin/bash

IMAGE="acecoder121/codear-microservices:engine"

docker build -t $IMAGE .
docker login
docker push $IMAGE
