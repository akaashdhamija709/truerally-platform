# truerally-platform

Backend microservices platform for racquet sports tracking.

## Planning Documents
- [TrueRally Platform Plan](docs/courtvision-architecture.md)
- [TrueRally Role Capabilities and User Flows](docs/role-capabilities.md)
- [TrueRally Delivery Roadmap](docs/roadmap.md)

## Developer Guide

### Local Development
Follow these steps the first time you set up the project:

1. Work through the [environment setup guide](docs/environment-setup.md) to install the JDK, Docker Engine, the Docker Compose plugin, and Gradle, then verify each tool is available on your `PATH`.
2. Clone the repository and open it in your IDE.
3. Today the repository does **not** ship a Compose manifest. If you need one before the platform team commits it, follow the [Compose manifest guidance](docs/docker-compose-guidance.md) to draft `compose.yml` in the repo root. Once the official file lands, replace your local draft with the shared version.
4. After the shared Docker Compose manifest is available, run `docker compose pull` followed by `docker compose up -d` to provision Postgres, Kafka, MinIO, and Keycloak.
5. The repository now ships a skeletal multi-module Gradle build. Until the wrapper is published, use a locally installed Gradle 8.7+ distribution and run `gradle clean build` to compile services and execute tests. Once the wrapper appears, switch to `./gradlew` so everyone runs the same version.
6. Build per-service Docker images with `docker build` or `bootBuildImage` as described in the environment guide, then run them against the compose-managed infrastructure.
7. Export the environment variables documented in the compose manifest so services can connect to the shared stack.

### Continuous Integration

GitHub Actions runs the repository's CI workflow on every push and pull request targeting `main`. The pipeline installs Temurin JDK 21, executes `gradle clean build` (or `./gradlew` when the wrapper is present), and uploads JUnit XML/HTML reports. A follow-on job validates the Docker Compose manifest to catch configuration regressions early.

### Contributing
- Open an issue describing the enhancement or bug prior to starting work.
- Fork the repository or create a feature branch, keeping commits scoped and descriptive.
- Ensure unit/integration tests pass (`./gradlew test`), add new coverage when introducing features, and update documentation as needed.
- Submit a pull request referencing the related issue; include context, testing evidence, and screenshots for UI-affecting changes.
