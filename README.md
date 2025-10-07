# truerally-platform

Backend microservices platform for racquet sports tracking.

## Planning Documents
- [CourtVision Platform Plan](docs/courtvision-architecture.md)
- [CourtVision Role Capabilities and User Flows](docs/role-capabilities.md)
- [CourtVision Delivery Roadmap](docs/roadmap.md)

## Developer Guide

### Local Development
1. Install Java 17, Docker, and Docker Compose.
2. Clone the repository and run `./gradlew build` to compile shared modules.
3. Start supporting infrastructure and services with `docker compose up` (the compose file provisions Postgres, Kafka, MinIO, and Keycloak).
4. Launch individual Spring Boot services locally using `./gradlew :service-name:bootRun`, pointing them at the compose-provided infrastructure via environment variables.
5. Access the aggregated OpenAPI docs through the API Gateway once services are running.

### Contributing
- Open an issue describing the enhancement or bug prior to starting work.
- Fork the repository or create a feature branch, keeping commits scoped and descriptive.
- Ensure unit/integration tests pass (`./gradlew test`), add new coverage when introducing features, and update documentation as needed.
- Submit a pull request referencing the related issue; include context, testing evidence, and screenshots for UI-affecting changes.
