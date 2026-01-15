# CodeArena

CodeArena is a online judge for coding problems similar to other online judges like Leetcode.It has rich set of problems where users can submit solutions and test their skills. Codearena also comes with integrated codeing assistant which has the access to entire problem and coding envionment which enables fast learning and clears and doubt.

## Key Features

* **Secure Code Execution:** Runs user submissions within isolated Docker containers to prevent system interference.
* **Asynchronous Judging:** Utilizes AWS SQS/Apache kafka to decouple submission intake from code execution, ensuring system stability.
* **High Performance:** The entire application is build on top of microservice architecture to have high performance and scalability. Addtionally the kafka acting as a queue enables the application to work in the peak time.
* **AI assistant:** The code editor is integrated with AI chatbot which helps coder to solve the bugs faster and seemlessly.
* **Robust Security:** Implements JWT-based authentication and a centralized API Gateway to protect internal microservices.

## System Architecture

CodeArena is build on three tier web architecture with backend on Java microservies. Front is built on react while backend has four micorservices.
Identity service which handles all user related things. Problem service which handles problem crud as well as submissions of problems. And execution service which runs the code and judges the correctness of solution.

![Architecture](/projects/arch_diag.svg)

The code is submitted to the problem service. From where it just pushes it into a queue. The code is capable of handling both the kafka or any managed queing service like SQS. In our application deployment we have chosed SQS as its cheaper to employ.At the same time problem service makes an entry into the redis inmemory database with the submission id with status of this submission setting to `PENDING`.

![Code submission](/projects/code-sub-user-flow.svg)

Once the engine receives the request. It runs the code in an isolated dockerise environment. This is done because code submitted by user can be malicious and so it should not mess with the actual running container. The `engine` creates new container and runs the code in this environment. Once it is done. The valdation is done based on the test cases output.After this `engine` provides the verdict back to problem service by putting in `redis` store. User will likely check the code back after some time. And at this time problem service will pass the verdict ot user.

For login and registration we use `bcrypt` and `jwt` based authentication to keep the user safe and secure.

![Sequence diagram](/projects/sequence-diagram.svg)

For now the AI-assistant is currently integrated into the `user` service.But in future it can be separated in a different service.


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

User-submitted code has zero access to the host file system or the network. Each container is ephemeral and destroyed after execution. Each microservice can be scaled independently. If the engine is under heavy load, we can spin up more execution nodes without affecting the login or problem browsing services.The API Gateway acts as a shield, ensuring that internal service ports are never exposed to the public internet.

## The Road Ahead

* **Live Contests:** Supporting scheduled, time-bound competitive events.
* **Observability:** Integrating Prometheus and Grafana for deep system monitoring.
* **Community Features:** Adding user blogs, editorials, and discussion forums for every problem.
