# CodeAR  - A Problem Solving Platform

CodeAR is an online coding platform that enables users to solve programming problems, run their code securely inside isolated Docker environments, and view submission results in real time. Users can register, log in, browse coding challenges, and submit their solutions seamlessly.

The platform is built using a highly scalable microservice architecture, ensuring reliability, performance, and smooth handling of increasing traffic and workload.

## Features

User authentication (register/login via JWT)

Browse and solve coding problems

Real-time code execution in isolated Docker containers

Secure runtime with resource limits

Fully async judging using Kafka

Live submission updates via long polling

Highly scalable microservice architecture

Caching using Redis (cache-aside strategy)

## Architecture Overview

CodeAR is designed to be highly available and scalable using microservice architecture.
It has following microserices -

1. User service
1. Problem service
1. Engine service

![Architecture diagram](./images/arch_diag.svg)

The User Service handles user registration, login, and all user-related CRUD operations.

The Problem Service manages problem creation, updates, and retrieval of coding challenges. It also accepts code submissions from users and places them into the processing pipeline.

The Engine Service is responsible for executing the submitted code. It uses the Docker API to run each submission inside a fully isolated environment, captures the output, and performs the necessary checks before updating the results.

A Java-based API Gateway serves as the unified entry point for all backend microservices, ensuring secure, controlled, and efficient routing of requests.

Additionally, the platform uses Redis with a cache-aside strategy to store frequently accessed data, significantly improving performance and reducing load on the database.

### Components

1. User service
1. Problem service
1. Engine service
1. API gateway
1. Front end(React)
1. Kafka
1. Redis
1. Postgres database
1. Docker deamon(Running so that code can be executed)

### Code judging flow

The user submits a problem along with their code, which is first received by the Problem Service. This service handles the necessary CRUD operations on the database and then pushes the code submission into a Kafka queue. The Engine Service consumes submissions from the queue and executes them one by one. After execution, it updates the database with the appropriate status and results. Meanwhile, the frontend uses long polling to check for updates, allowing the user to receive the execution results as soon as they become available.

![alt text](./images/sequence-diagram.svg)


# Database Design

Users Table — Stores user credentials, profile info, solved problem count, etc.

Problems Table — Problem metadata, difficulty, and test cases.

Submissions Table — Stores submission ID, problem ID, code, status, language, and output.

## Set up 

### User service

Following environment variables should be present to run the user microservice

```text
DB_HOST
DB_PORT
DB_NAME
USER
PASSWORD
SERVER_PORT

JWT_SECRET
JWT_EXPIRY
```

To run application run using the maven spring boot

```bash
./mvnw spring-boot:run
```

### Problem Service

```

DB_HOST
DB_PORT
DB_NAME
USER
PASSWORD
SERVER_PORT

REDIS_HOST
REDIS_PORT

JWT_SECRET
JWT_EXPIRY

```

```bash
./mvnw spring-boot:run
```

### API gateway

```text
PROBLEM_URL
USER_URL
ALLOWED_ORIGIN
SERVER_PORT
```

```bash
./mvnw spring-boot:run
```

### Engine service

```text
DB_HOST
DB_PORT
DB_NAME
USER
PASSWORD
SERVER_PORT
KAFKA_BOOTSTRAP_SERVERS

REDIS_HOST
REDIS_PORT
```

```bash
./mvnw spring-boot:run
```

## Tech Stack

### Backend

Spring Boot (Java 21)

PostgreSQL — Persistent storage for users, problems, and submissions.

Redis — Caching layer for problem metadata and active submissions.

Kafka — Message queue for asynchronous communication between Problem and Engine services.

Docker — Used by the Engine service to run user code safely in isolated containers.

JWT (JSON Web Tokens) — Authentication and authorization mechanism.

### Front end

React+Vite with typescript and tailwind.

[front end repository link](https://github.com/coder-Ace77/codear-front)

## Security 🔒

The API Gateway sits in front of all microservices to ensure controlled access and keep the internal services protected. It also handles JWT authentication so only authorized users can reach the intended endpoints.

User code executes inside an isolated Docker container with no access to the host filesystem or external environment, ensuring a secure and sandboxed deployment.

## System design 📈

Horizontal microservice scaling allows the system to handle increased traffic by running multiple instances of individual services. This ensures higher availability, improved fault tolerance, and consistently smooth performance under varying loads.

Kafka is used for asynchronous processing, enabling services to communicate efficiently without blocking. In the context of a problem-submission workflow, Kafka plays a critical role: the Problem Service submits user code to a Kafka topic, and the Engine Service consumes submissions from the queue at its own pace. This decoupled architecture ensures stable operation even when execution times vary or code runs for longer durations. It also smooths out traffic spikes throughout the day allowing the system to continue functioning even when resources are limited because incoming submissions no longer depend on immediate engine availability.

Redis is integrated for caching, providing rapid in-memory data access and significantly boosting system performance. By storing frequently accessed metadata, problem data, or user information, it reduces database load and helps maintain fast response times across services.

Load balancing is managed through the API Gateway, which distributes incoming traffic evenly across all microservices. This prevents bottlenecks, ensures optimal resource utilization, and improves system reliability by routing requests intelligently based on service health and availability.

## Future enhancements 🛣️

1. Real time contests
1. Logging and monitoring 
1. User blogs and editorials