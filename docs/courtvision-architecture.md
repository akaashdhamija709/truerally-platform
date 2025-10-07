# CourtVision Platform Plan

## Overview
CourtVision is a modular Spring Boot platform that orchestrates player management, tournament operations, match tracking, and ranking workflows for multiple racquet sports. The platform delivers RESTful APIs exclusively for all clients (mobile, web, admin) while maintaining a Kafka-centered event backbone for asynchronous workflows.

## High-Level Architecture
- **API Gateway Layer**
  - Spring Cloud Gateway (or Spring MVC edge service) that terminates TLS, validates JWTs, applies rate limiting, and routes REST requests to downstream services.
  - API documentation via aggregated OpenAPI specs, exposed through the gateway for client consumption.
- **Core Microservices (Spring Boot + JPA)**
  - Player, Organizer, Coach, Tournament, Match, Ranking, Stats, Notification services.
  - Each service exposes REST APIs, publishes/consumes Kafka events, and persists to an isolated schema in PostgreSQL. Redis is leveraged for caching leaderboards and session data where needed.
- **Shared Services**
  - Auth Service (Spring Authorization Server) issues role-based JWTs (Player, Coach, Organizer, Admin).
  - File Service (MinIO/S3-compatible) handles organizer verification documents and coach certifications.
  - Integration Service ingests professional player data from external APIs and publishes normalized records to Kafka.
- **Event Backbone**
  - Apache Kafka topics (e.g., `player.registered`, `organizer.verified`, `coach.onboarded`, `match.result.recorded`, `ranking.update.requested`).
  - Outbox pattern in write services ensures reliable event emission.
- **Observability & Ops**
  - Centralized logging (ELK/OpenSearch), metrics via Prometheus + Grafana.
  - Feature flags (FF4J/LaunchDarkly-compatible) for staged release of live scoring or coaching tools.

## REST-First Strategy & GraphQL Considerations
- REST APIs provide clear resource modeling aligned with microservice boundaries and simplify client integration with existing HTTP tooling.
- Consistency is achieved with shared standards: pagination, filtering, sorting, error payloads (RFC 7807), and versioning via URI or header.
- **Why GraphQL could be beneficial (even if not adopted now):**
  1. **Efficient Data Fetching:** GraphQL allows clients to fetch multiple related resources in a single request, reducing over-fetching and under-fetching compared to REST when composing dashboards or mobile screens.
  2. **Rapid Iteration for Clients:** Clients can evolve queries without backend changes as long as the schema fields exist, enabling faster UI experimentation.
  3. **Strong Typing & Tooling:** GraphQL schemas are strongly typed, empowering client-side code generation, validation, and documentation tooling.
  4. **Aggregated Views Across Services:** A GraphQL BFF can stitch data from multiple microservices into cohesive responses, reducing orchestration logic in clients.

Although CourtVision will launch with REST-only interfaces, understanding these potential advantages keeps the door open for future adoption if aggregation complexity grows.

## Service Responsibilities & Data Highlights
| Service | Responsibilities | Key Tables / Collections | Kafka Topics (Pub/Sub) |
| --- | --- | --- | --- |
| **Auth** | User registration, JWT issuance, role management, password reset | `users`, `roles`, `user_roles`, `refresh_tokens` | Pub: `auth.user.created`; Sub: — |
| **Player** | CRUD players, multi-sport preferences, demographics, intent | `players`, `player_sports`, `player_stats_summary` | Pub: `player.registered`, `player.updated`; Sub: `auth.user.created`, `ranking.level.updated` |
| **Coach** | Manage coach profiles, certifications, player assignments, training plans | `coaches`, `coach_documents`, `coach_player_links`, `training_sessions` | Pub: `coach.onboarded`, `coach.assigned`; Sub: `auth.user.created`, `player.registered` |
| **Organizer** | Organizer onboarding, document management, admin approvals | `organizers`, `organizer_documents`, `organizer_status_audit` | Pub: `organizer.submitted`, `organizer.approved`; Sub: `auth.user.created` |
| **Tournament** | Tournament lifecycle, registrations, draw generation metadata | `tournaments`, `tournament_events`, `tournament_registrations`, `draws`, `time_slots` (future) | Pub: `tournament.created`, `tournament.registration.received`, `tournament.draw.published`; Sub: `organizer.approved`, `player.registered`, `coach.assigned` |
| **Match** | Friendly/tournament match creation, status updates, score ingestion | `matches`, `match_participants`, `match_scores`, `friendly_matches` | Pub: `match.scheduled`, `match.result.recorded`; Sub: `tournament.draw.published`, `training.session.completed` |
| **Ranking** | Elo/TrueSkill computations per sport, ladder updates, level tracking | `rankings`, `ranking_history`, `player_levels` | Pub: `ranking.updated`, `ranking.level.updated`; Sub: `match.result.recorded` |
| **Stats & Reporting** | Aggregate player/tournament stats, dashboards, performance tags | `player_performance_tags`, `tournament_metrics`, `match_stats` | Pub: `stats.player.updated`; Sub: `match.result.recorded`, `tournament.completed` |
| **Notification** | Email/push templates for registrations, approvals, schedules | `notifications`, `templates`, `subscriptions` | Pub: `notification.sent`; Sub: domain events needing alerts |
| **Integration** | Sync external pro players, scheduled updates | `external_players`, `sync_jobs` | Pub: `player.pro.imported`; Sub: `ranking.updated` |

- Additional role-specific responsibilities and user flows are documented in [`docs/role-capabilities.md`](./role-capabilities.md).

## Domain Modeling Notes
- **Players & Coaches**: Many-to-many relationship through `coach_player_links`, capturing association status, start/end dates, and primary coach flag.
- **Training Sessions**: Track schedule, location, focus area, and post-session evaluations; publish completion events for Stats service.
- **Tournaments & Coaches**: Maintain mapping of coaches attending tournaments for accreditation and logistics.
- **Matches**: Extend match records with optional coach presence and coaching notes for analytics.

## Key Event Flows (Updated)
1. **Coach Onboarding**
   - Coach Service receives registration → stores pending status → emits `coach.onboarded` for admin review.
   - Admin approves via Admin Panel → status updated, Notification Service informs coach.
2. **Coach-Player Association**
   - Coach requests association → player accepts → Coach Service emits `coach.assigned`.
   - Player Service updates player profile; Notification service alerts relevant users.
3. **Training Session Lifecycle**
   - Coach schedules session → reminder notifications dispatched.
   - After completion, results logged → `training.session.completed` event updates Stats dashboards.
4. **Match Result Recording**
   - Match Service captures results → triggers Ranking/Stats recalculations → coaches receive insights for associated players.

## Security & Access Control
- JWTs contain role claims (`PLAYER`, `COACH`, `ORGANIZER`, `ADMIN`) with sport-level permissions.
- Spring Security policies enforce that coaches cannot modify tournament structures but can manage training data for associated players.
- Admin Panel extends to coach verification workflow.

## Storage Strategy
- PostgreSQL schemas per service with Liquibase/Flyway migrations.
- Redis caches leaderboards and personalized dashboards (e.g., coach view of player stats).
- MinIO/S3 for document storage (organizer proofs, coach certifications, training resources).
- Optional Elasticsearch for cross-entity search (players/coaches/tournaments) in future phases.

## REST API Surface Planning
- REST endpoints grouped per service (`/players`, `/coaches`, `/tournaments/{id}/matches`, `/coaches/{id}/training-sessions`).
- Standardized pagination, filtering (sport, level, location), sorting, and error handling.
- Use OAuth2 scopes or custom claims to gate endpoints (e.g., `SCOPE_coach.training.write`).
- API clients use REST to compose dashboards; if cross-resource aggregation becomes heavy, consider introducing a read-optimized service while keeping REST exposure.

## Admin & Organizer Workflows
- **Admin Panel**: manage organizer and coach applications, approve tournaments, monitor rankings, and audit training activity.
- **Organizer Dashboard**: manage tournaments, view registrants, run draw generation, coordinate with coaches for practice schedules.
- **Coach Workspace**: manage player roster, training plans, analytics dashboards, and communication with players.
- **Player App**: register, join tournaments, manage coach relationships, review training feedback, view leaderboards.

## CI/CD & Dev Environment
- Monorepo with a multi-module Gradle build to keep shared domain models and dependencies aligned while making it easy for developers to run the platform locally.
- Docker Compose for local setup (services + Postgres + Kafka + MinIO + Keycloak).
- GitHub Actions pipeline: build, unit/integration tests, static analysis (SpotBugs, Checkstyle), container builds, deploy to staging Kubernetes/ECS.
- Feature environments via ephemeral namespaces using Helm or Kustomize.

## Roadmap
- See [`docs/roadmap.md`](./roadmap.md) for phased delivery details spanning MVP through monetization features.

## Testing Strategy
- **Unit tests** in each service validating domain logic, request validation, and security policies.
- **Contract tests** (e.g., Spring Cloud Contract) to guarantee REST interface compatibility across services and client SDKs.
- **Integration tests** leveraging Testcontainers to spin up Postgres, Kafka, and Redis for verifying critical workflows such as tournament registration, coach-player association, and ranking recalculation.
- **End-to-end smoke tests** executed post-deployment via GitHub Actions against staging, covering user journeys (player registration, organizer approvals, match reporting).
