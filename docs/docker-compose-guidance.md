# Docker Compose Manifest Guidance

The repository does not yet ship an official `compose.yml`. Until the platform team commits the canonical file, you can create a local draft to unblock experimentation. This guide explains who should own the manifest and how to scaffold one that mirrors the infrastructure described in the architecture plan.

## Ownership and delivery plan

- **Primary owner:** The platform/infrastructure squad is responsible for delivering the shared Compose manifest. It should land alongside the first runnable service modules so all contributors can rely on a single source of truth.
- **When to create a local draft:** If you want to prototype before the official manifest is merged, you may create `compose.yml` (or `docker-compose.yml`) in the repository root. Treat it as disposable—replace it with the shared version as soon as it is published.
- **Contribution expectations:** When the official manifest is delivered it will include environment variables, volumes, health checks, and network aliases that match the service contracts. Local drafts should avoid committing to `main` to prevent drift.

## Recommended structure for a local draft

Place the file in the repository root so `docker compose` automatically discovers it.

```yaml
# compose.yml
name: truerally
services:
  postgres:
    image: postgres:16-alpine
    container_name: truerally-postgres
    environment:
      POSTGRES_DB: truerally
      POSTGRES_USER: truerally
      POSTGRES_PASSWORD: truerally
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  kafka:
    image: bitnami/kafka:3.6
    container_name: truerally-kafka
    environment:
      KAFKA_CFG_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_CFG_LISTENERS: PLAINTEXT://:9092
      KAFKA_CFG_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      ALLOW_PLAINTEXT_LISTENER: "yes"
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"

  zookeeper:
    image: bitnami/zookeeper:3.9
    container_name: truerally-zookeeper
    environment:
      ALLOW_ANONYMOUS_LOGIN: "yes"
    ports:
      - "2181:2181"

  minio:
    image: minio/minio:RELEASE.2024-03-21T23-13-43Z
    container_name: truerally-minio
    environment:
      MINIO_ROOT_USER: truerally
      MINIO_ROOT_PASSWORD: truerally-secret
    command: server /data --console-address :9001
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio_data:/data

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    container_name: truerally-keycloak
    command: start-dev --import-realm
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - "8080:8080"
    volumes:
      - ./local-dev/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json:ro

volumes:
  postgres_data:
  minio_data:
```

### Why these services?
- **Postgres** powers transactional data for the core services.
- **Kafka + ZooKeeper** (or a modern Kafka image that embeds Kraft) enable event streaming.
- **MinIO** emulates S3-compatible object storage for match footage and analytics artifacts.
- **Keycloak** handles identity and access management for admin portals and public APIs.

## Next steps after creating the file

1. Run `docker compose pull` to download the images.
2. Launch the stack with `docker compose up -d`.
3. Export the environment variables expected by your services (see the [environment setup guide](environment-setup.md)).
4. Once the official manifest is merged, delete or overwrite your local draft to stay aligned with the shared configuration.

---

For questions about the shared manifest timeline, reach out to the platform squad in the `#truerally-platform` Slack channel or open a GitHub discussion so contributors can coordinate.
