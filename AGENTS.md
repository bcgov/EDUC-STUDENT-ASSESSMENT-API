# AGENTS.md

Guidance for AI agents working in this repo. High-signal, repo-specific facts only.

## Stack

Spring Boot 3.3.3 / Java 21 / Maven single module (`api/`) / PostgreSQL + Flyway / OAuth2
resource server (Keycloak JWT) / NATS Core + JetStream / MapStruct + Lombok /
Jasper Reports. Deployed to BC Government OpenShift.

There is **no** parent aggregator pom at the repo root — run Maven from inside `api/`.
The whole app lives in package `ca.bc.gov.educ.assessment.api`.

## Build & test

All commands run from `api/` (or use `mvn -f api/pom.xml ...`):

- Full build + unit tests (CI uses this): `mvn clean package`
- Skip tests: `mvn clean package -DskipTests`
- One test class: `mvn test -Dtest=ClassName`
- One method: `mvn test -Dtest=ClassName#methodName`

The `coverage` profile is `activeByDefault=true` and pulls in jacoco, failsafe
(integration tests, `*IT.java` — none currently), and the hibernate-enhance-maven-plugin.
No profile flag is needed for normal work.

There is no checkstyle, spotless, or `.editorconfig` — match existing code style.

## Running locally

`api/README.md` says `-Dspring.profiles.active=dev`, but there is **no**
`application-dev.properties`. The real local profile is `local` backed by the
gitignored `application-local.properties`. Prefer `-Dspring.profiles.active=local`.

Requires external services (reach them via VPN as the README notes):
- NATS with JetStream: **port-forward the dev OpenShift NATS service** rather than
  running a local `docker run nats -js` broker — a fresh local NATS lacks the
  pub/sub topics the app subscribes to and will appear broken.
  `oc -n <dev-namespace> port-forward service/nats 4222:4222` (requires an `oc login`
  session in the terminal first). Use your assigned dev namespace.
- PostgreSQL, Keycloak, CHES (email), S3/COMS (object storage), and external APIs
  (Institute, Grad Student, SDC) — values flow through `${...}` placeholders in
  `application.properties`.

## Architecture notes

- Layered contract: endpoint **interfaces** in `endpoint/v1/` declare routes +
  `@PreAuthorize("hasAuthority('SCOPE_...')")` scopes + Swagger; `controller/v1/`
  classes `implements` them and delegate to `service/v1/`. Mirror this split for new APIs.
- Most layers are versioned under a `v1/` subpackage.
- API base path: `/api/v1/student-assessment` (see `constants/v1/URL.java`).
- Saga pattern: DB-persisted saga entities + dual messaging (NATS Core for
  request/reply dispatch, NATS JetStream durable stream for choreographed events).
  `@EnableRetry` for retry; ShedLock (JDBC, table `ASSESSMENT_SHEDLOCK`) guards
  `@Scheduled` jobs across replicas.
- Outbound REST via WebClient (`rest/`): `RestWebClient` (OAuth2 client creds),
  `ComsRestUtils` (S3/COMS), `RestUtils` (CHES email + external calls).

## Testing

- Tests use **Zonky embedded PostgreSQL**, not H2. (H2 dependency in pom.xml is
  stale.) Spring Flyway autoconfig is disabled; `TestDataSourceConfig` starts an
  embedded Postgres and runs Flyway manually against `db/migration`.
- No live NATS broker needed — messaging is mocked via `MockConfiguration`
  (`@Profile("test")`).
- Integration tests extend `BaseAssessmentAPITest` (`@ActiveProfiles("test")`,
  `@AutoConfigureMockMvc`): seeds reference/code-table rows and provides ~30
  entity/DTO factory methods.
- TRAX flat-file test fixtures live under `src/test/resources/`.

## Migrations

Flyway: `src/main/resources/db/migration/` (96 files, `V1.0.0__...` → `V1.97__...`).
Version naming is mixed (early `V1.0.x`, later `Vxx`). One-off SQL in
`src/main/resources/scripts/` is NOT run by Flyway.

## CI/CD & branches

- CI triggers on (`.github/workflows/ci-api.build.and.test.yml`): `master`,
  `feature/**`, `Feature/**`, `fix/*`, `Fix/*`. Note the case-duplicated patterns —
  both cases are intentional.
- Deploy flow: push `master` → build & deploy DEV+TEST automatically. TEST and PROD
  deploys are `workflow_dispatch`; PROD uses the latest git tag. Create a git tag +
  OpenShift imagestream tag via the "Create Tag" workflow (input `version`).
- Deployment template: `tools/openshift/student-assessment-api.yaml`;
  ConfigMap + Keycloak client/scope provisioning: `tools/config/update-configmap.sh`.

## Conventions

- Commit messages: conventional-ish (`feat:`, `fix:`). PRs squash-merged (`#NNN`).
  Jira-style branch prefixes common (`feature/GRAD2-xxxx`, `feature/eac-xx`).

## Gotchas

- `bcsans.jar` is declared as a **local Maven repository** in `pom.xml`
  (`file://${project.basedir}/src/main/resources/bcsans.jar`) but is NOT in the
  checkout → first builds against that path may break. The BCSans TTFs/layout live
  under `src/main/resources/fonts/` instead.
- MapStruct + Lombok must stay wired together (lombok-mapstruct-binding) in the
  `coverage` profile's `annotationProcessorPaths`. Keep them aligned when editing.
- Gitignored local-only files — never commit: `.envrc`, `shell.nix`,
  `application-local.properties`, `*.http`, `api/target/`.
