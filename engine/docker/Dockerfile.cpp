FROM gcc:latest

# Install 'time' package
# gcc:latest is typically Debian based
RUN apt-get update && apt-get install -y time && rm -rf /var/lib/apt/lists/*

WORKDIR /app
