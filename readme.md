# CodeArena

CodeArena is an online judge for coding problems similar to other online judges like LeetCode. It has a rich set of problems where users can submit solutions and test their skills. CodeArena also comes with an integrated chatbot which has access to the entire problem and coding environment, enabling fast learning and clearing any doubts.

## Key Features

* **Secure Code Execution:** Runs user submissions within isolated Docker containers to prevent system interference.
* **Asynchronous Judging:** Utilizes AWS SQS/Apache Kafka to decouple submission intake from code execution, ensuring system stability.
* **High Performance:** The entire application is built on top of a microservices architecture to ensure high performance and scalability. Additionally, Kafka acting as a queue enables the application to function smoothly during peak times.
* **AI Assistant:** The code editor is integrated with an AI chatbot which helps coders solve bugs faster and seamlessly.
* **Robust Security:** Implements JWT-based authentication and a centralized API Gateway to protect internal microservices.

## System Architecture

CodeArena is built on a three-tier web architecture with a backend powered by Java microservices. The frontend is built with React while the backend consists of four microservices: the Identity service, which handles all user-related logic; the Problem service, which handles problem CRUD operations as well as problem submissions; and the Execution service, which runs the code and judges the correctness of solutions.

![Architecture](/images/arch_diag.svg)

The code is submitted to the Problem service, from where it is pushed into a queue. The system is capable of handling both Kafka and managed queuing services like SQS. In our application deployment, we have chosen SQS as it is more cost-effective. At the same time, the Problem service makes an entry into the Redis in-memory database with the submission ID, setting the status of the submission to `PENDING`.

![Code submission](/images/code-sub-user-flow.svg)

Once the engine receives the request, it runs the code in an isolated Dockerized environment. This is done because code submitted by users can be malicious and should not interfere with the actual running container. The engine creates a new container and runs the code in this environment. Once complete, validation is performed based on the test case output. After this, the engine provides the verdict back to the Problem service by updating the Redis store. The user can then check back after a short time, at which point the Problem service passes the verdict to the user.

For login and registration, we use bcrypt and JWT-based authentication to keep user data safe and secure.

![Sequence diagram](/images/sequence-diagram.svg)

Currently, the AI assistant is integrated into the User service, but in the future, it may be separated into a dedicated service.

## Tech Stack

### Backend & Infrastructure

* **Language:** Java 21 (Spring Boot)
* **Database:** PostgreSQL (Primary persistent storage)
* **Message Broker:** SQS/Apache Kafka (Asynchronous task distribution)
* **Caching:** Redis (Speeding up problem metadata and session access)
* **Containerization:** Docker (Isolated runtime environments)

### Frontend

* **Framework:** React + Vite
* **Styling:** Tailwind CSS
* **Language:** TypeScript
* *Frontend Repository:* [Explore the Code](https://github.com/coder-Ace77/codear-front)

## Reliability & Security

User-submitted code has zero access to the host file system or the network. Each container is ephemeral and destroyed after execution. Each microservice can be scaled independently. If the engine is under heavy load, we can spin up more execution nodes without affecting the login or problem browsing services. The API Gateway acts as a shield, ensuring that internal service ports are never exposed to the public internet.

## The Road Ahead

* **Live Contests:** Supporting scheduled, time-bound competitive events.
* **Observability:** Integrating Prometheus and Grafana for deep system monitoring.
* **Community Features:** Adding user blogs, editorials, and discussion forums for every problem.
