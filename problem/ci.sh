#!/bin/bash
set -e

# Load environment variables from .env file if present
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi

# Required environment variables
DOCKER_USERNAME="${DOCKER_USERNAME:-acecoder121}"
DOCKER_PASSWORD="${DOCKER_PASSWORD:-$DOCKER_HUB_TOKEN}"
REPO_NAME="codear"
IMAGE_TAG="problem"
FULL_IMAGE_NAME="${DOCKER_USERNAME}/${REPO_NAME}:${IMAGE_TAG}"

# Safety checks
if [ -z "$DOCKER_USERNAME" ] || [ -z "$DOCKER_PASSWORD" ]; then
  echo "Missing Docker credentials."
  echo "Set DOCKER_USERNAME and DOCKER_PASSWORD (or DOCKER_HUB_TOKEN)"
  exit 1
fi

echo "Building Java application..."
if ./mvnw clean package -DskipTests; then
  echo "Java build successful."
else
  echo "Maven build failed."
  exit 1
fi

echo "Building Docker image: ${FULL_IMAGE_NAME}"
if docker build -t "${FULL_IMAGE_NAME}" .; then
  echo "Docker image build successful."
else
  echo "Docker image build failed."
  exit 1
fi

echo "Logging into Docker Hub..."
echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
echo "Login successful."

echo "Pushing ${FULL_IMAGE_NAME} to Docker Hub..."
if docker push "${FULL_IMAGE_NAME}"; then
  echo "Successfully pushed ${FULL_IMAGE_NAME}!"
else
  echo "Docker push failed."
  exit 1
fi
