# CLAUDE.md — billing-hub

Pet project: B2B subscription billing on Java 25 / Spring Boot 4. Plan and business rules: docs/PLAN.md (BR-01…BR-18). Talk to me in Russian; code, comments, commits and docs in English.

## Commands
- Build and all checks: `./gradlew check`
- Run locally: `docker compose up -d` then `./gradlew :billing-service:bootRun`
- Run with Testcontainers instead of compose: `./gradlew :billing-service:bootTestRun`
- One test class: `./gradlew :billing-service:test --tests "*ProrationCalculatorTest"`
- Format: `./gradlew spotlessApply`

## Architecture
- Modular monolith in billing-service; packages by feature: customers, contracts, invoicing, payments, penalties, dunning, reporting, integration, shared.
- Inside a module: api (public interface + record DTOs), web, domain (pure Java, no Spring), app (transactional services), infra.
- A module may use another module only through its `api` package or events. ArchUnit tests enforce this.
- Satellites: accounting-stub (Kafka consumer app), billing-mcp (Spring AI MCP server, read-only via REST).

## Rules
- Money: BigDecimal only, numeric(19,2), HALF_UP at line level; never call setScale without a RoundingMode.
- Schema changes only via a NEW Liquibase changeset; never edit an applied changeset.
- Every business rule BR-xx has a test that names it.
- Domain logic first as a pure class with a unit test, then wiring.
- Kafka events are written to outbox_event in the same transaction; never call KafkaTemplate from domain services.
- No Lombok. Records for DTOs and events.
- Spring Boot 4: use spring-boot-starter-webmvc (not -web), Jackson 3 (tools.jackson, JsonMapper), technology-specific test starters, Testcontainers 2 artifact/package names.
- Before adding a dependency: ask me, and check Boot 4 / Jackson 3 compatibility.
- No secrets in the repo; config via env vars and .env (gitignored).
- No real personal or company data; demo data only via Datafaker.

## Workflow
1. For a task: restate it, list assumptions, propose a plan and test cases. Wait for my OK.
2. Write tests first for domain logic.
3. Implement in small steps; run `./gradlew check` before saying "done".
4. Keep the diff focused: do not reformat or touch unrelated files, do not delete comments.
5. Summarize: what changed, how to verify, decisions made (for the PR description).
6. Algorithms I write myself (you review only): proration, FIFO allocation, penalty calculation, outbox relay, owner check.
