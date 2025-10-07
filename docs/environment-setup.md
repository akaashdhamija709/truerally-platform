# Environment Setup Guide

This walkthrough explains, step by step, how to prepare your workstation for the TrueRally platform and what to do once the service code and Docker manifests arrive.

## Step 1: Install the required tooling

| Tool | Version | Why it is needed | Installation Notes |
| --- | --- | --- | --- |
| **Java Development Kit** | 21 LTS | Compiles and runs the Spring Boot services. | Use [Adoptium Temurin](https://adoptium.net/) or any distribution that ships a full JDK. Ensure `JAVA_HOME` points to the installation directory. |
| **Docker Engine** | 24.x or newer | Builds and runs container images locally. | Follow the [Docker Desktop](https://www.docker.com/products/docker-desktop/) installer on macOS/Windows or the [Linux Engine instructions](https://docs.docker.com/engine/install/). |
| **Docker Compose plugin** | 2.x | Orchestrates the shared infrastructure containers (Postgres, Kafka, MinIO, Keycloak). | Included with modern Docker Desktop/Engine installations. On older setups install the legacy `docker-compose` binary and ensure it is available on your `PATH`. |
| **Gradle (local install or wrapper)** | 8.7+ | Builds services and runs tests. | A skeletal multi-module `build.gradle` and `settings.gradle` now live in the repository. Install Gradle 8.7+ locally (or use your IDE's bundled Gradle) until the `./gradlew` wrapper script is published with the first service module. |

> **Why call out the Docker Compose plugin?** Docker Compose used to be a separate Python tool invoked via `docker-compose`. Modern Docker releases bundle it as a CLI plugin accessed with `docker compose`. Confirm it works by running `docker compose version`.

## Step 2: Verify everything is on the PATH

Open a terminal and run the commands below. Each one should print a version string:

```bash
java -version
javac -version
docker version
docker compose version
gradle --version
```

If any command is missing, revisit Step 1. On Linux, a `permission denied` error from Docker usually means your user is not in the `docker` group yet; log out and back in after adding it. On macOS/Windows ensure Docker Desktop is running.

## Step 3: Clone the repository

```bash
git clone git@github.com:TrueRally/truerally-platform.git
cd truerally-platform
```

You can now open the workspace in your IDE of choice.

## Step 4: Prepare (or draft) the shared infrastructure stack

The platform team will deliver a shared `compose.yml` that provisions Postgres, Kafka, MinIO, and Keycloak. Until it is merged:

1. Decide whether you need a temporary manifest. If yes, follow the [Compose manifest guidance](docker-compose-guidance.md) to create a local draft in the repository root. Do **not** commit the draft to `main`.
2. Pull the exact image versions referenced in whichever manifest you are using:
   ```bash
   docker compose pull
   ```
3. Launch the stack in the background:
   ```bash
   docker compose up -d
   ```
4. Confirm the containers are healthy:
   ```bash
   docker compose ps
   ```

Once the official manifest lands, replace any local draft so the entire team works from the same configuration.

## Step 5: Build the services with Gradle

The repository already includes a shared `build.gradle` and `settings.gradle` with Spring Boot and dependency management plugins preconfigured for future service modules. Until the wrapper is published, install Gradle 8.7+ locally (or use your IDE's integrated Gradle) and run:

```bash
gradle clean build
```

Once `./gradlew` lands, swap to the wrapper so every contributor uses the same Gradle version. Either command compiles the code, executes unit tests, and assembles runnable JARs—rerun it whenever you pull new changes or before opening a pull request.

## Step 6: Build Docker images for services

Each Spring Boot service will include a `Dockerfile`. After implementing a service you can build its container image locally:

```bash
docker build -t truerally/<service-name>:local -f services/<service-name>/Dockerfile .
```

Alternatively, if the service uses Spring Boot's buildpacks support you can run:

```bash
gradle :services:<service-name>:bootBuildImage --imageName=truerally/<service-name>:local
```

Swap `gradle` for `./gradlew` once the wrapper is available. Use whichever approach the service module documents. After building the image, run it alongside the compose-managed infrastructure using `docker run` or a service-specific compose override file.

## Step 7: Export environment variables for local runs

Compose-managed infrastructure typically exposes connection details (JDBC URLs, Kafka bootstrap servers, S3 endpoints, Keycloak realms). Create a `.env` file or export environment variables in your shell so the services can consume them. Example variables you might need:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/truerally
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export MINIO_ENDPOINT=http://localhost:9000
export KEYCLOAK_URL=http://localhost:8080
```

The exact values will be documented alongside the compose manifest.

---

Once these steps are complete you are ready to implement the services outlined in the architecture and roadmap. Keep this guide handy as the project evolves—the compose manifest and Gradle modules will update it with concrete commands and environment variables.
