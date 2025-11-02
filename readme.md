.
# CodeAR — Online Code Execution & Problem Solving Platform

CodeAR is a scalable, microservice-based online coding platform that allows users to solve programming problems, execute code securely in Docker containers, and view submission results in real-time.
The platform follows a distributed, event-driven architecture using Spring Boot, Kafka, and Redis, with React + TypeScript for the frontend.

# ⚡ Features

✅ JWT Authentication via Gateway
✅ Problem search and filtering
✅ Code execution in multiple languages (C++, Java, Python)
✅ Run and Submit functionality
✅ View all past submissions and results
✅ User profile with problem-solving statistics
✅ Redis caching for high performance
✅ Kafka-based async code execution pipeline
✅ Docker-based sandboxing for safe execution

.

# Architecture Overview

CodeAR is composed of multiple microservices that communicate via REST and Kafka message queues.

# Microservices
## Service	Description
## Gateway Service	Acts as the API gateway, handling authentication (JWT), routing, and request forwarding.
## User Service	Manages user registration, login, profiles, and stats (e.g., problems solved, submissions made).
## Problem Service	Manages problem creation, metadata, filtering, and submission handling. Publishes execution jobs to Kafka and caches problem data in Redis.
## Engine Service (Worker)	Consumes execution jobs from Kafka, runs user code securely inside Docker containers, polls Redis for submission status, and updates results back to the database.

                ┌────────────────┐
                │   Frontend     │
                │ (React + TS)   │
                └──────┬─────────┘
                       │
                       ▼
               ┌─────────────────┐
               │   Gateway API   │
               │ (JWT + Routing) │
               └──────┬──────────┘
                      │
    ┌─────────────────┼───────────────────┐
    ▼                 ▼                   ▼
┌─────────┐     ┌────────────┐      ┌───────────────┐
│ UserSvc │     │ ProblemSvc │ ---> │  Redis Cache  │
└─────────┘     └──────┬─────┘      └───────────────┘
                       │
                       ▼
                ┌─────────────┐
                │   Kafka MQ  │
                └──────┬──────┘
                       │
                       ▼
                 ┌──────────────┐
                 │ EngineSvc 🧠 │
                 │ (Docker Run) │
                 └──────────────┘


# System Flow
1. User Submits Code

The frontend sends a request (code, language, inputs) to the Problem Service through the Gateway.

The Problem Service:

Caches submission info in Redis.

Publishes a job message to Kafka.

2. Code Execution (Engine Service)

Runs code in an isolated Docker container.

Collects logs/output.

Updates submission status in Redis and DB.

Publishes the final result.

3. Result Delivery

The frontend polls or subscribes to the submission status.

The Gateway routes the request to the Problem Service, which fetches results from Redis or DB.

.

# Database Design

Users Table — Stores user credentials, profile info, solved problem count, etc.

Problems Table — Problem metadata, difficulty, and test cases.

Submissions Table — Stores submission ID, problem ID, code, status, language, and output.

(All microservices share the same PostgreSQL instance for relational consistency.)


# Environment file
## for user microservice

export DB_HOST=
export DB_PORT=
export DB_NAME=
export USER=
export PASSWORD=
export SERVER_PORT=

export JWT_SECRET=
export JWT_EXPIRY=

### run the application
./mvnw spring-boot:run

## for problem service

export DB_HOST=
export DB_PORT=
export DB_NAME=
export USER=
export PASSWORD=
export SERVER_PORT=

export REDIS_HOST=
export REDIS_PORT=

export JWT_SECRET=
export JWT_EXPIRY=


### run the application
./mvnw spring-boot:run


# for gateway service

export PROBLEM_URL=
export USER_URL=
export ALLOWED_ORIGIN=

export SERVER_PORT=

### run the application
./mvnw spring-boot:run


# for engine service

export DB_HOST=
export DB_PORT=
export DB_NAME=
export USER=
export PASSWORD=
export SERVER_PORT=
export KAFKA_BOOTSTRAP_SERVERS=

export REDIS_HOST=
export REDIS_PORT=



### run the application
./mvnw spring-boot:run


# Tech Stack
## Backend

Spring Boot (Java)

PostgreSQL — Persistent storage for users, problems, and submissions.

Redis — Caching layer for problem metadata and active submissions.

Kafka — Message queue for asynchronous communication between Problem and Engine services.

Docker — Used by the Engine service to run user code safely in isolated containers.

JWT (JSON Web Tokens) — Authentication and authorization mechanism.