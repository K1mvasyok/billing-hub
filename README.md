# billing-hub

Pet project: B2B subscription billing on Java 25 and Spring Boot 4.

Clients (organisations and sole traders) hold contracts. A contract has items: a plan, a quantity and its own validity period. Plan prices change over time. The service invoices contracts month by month, pro-rated by days and by price changes inside the month.

The business rules are numbered BR-01 … BR-18 in [docs/PLAN.md](docs/PLAN.md). Each implemented rule is named in the tests that check it.

## Status

| Stage | Scope | State |
|---|---|---|
| M0 | Skeleton: Gradle, Liquibase, Testcontainers, CI, health check | done |
| M1 | Customers, plans and prices, contracts and items, Envers audit | done |
| M2 | Invoicing: pro-rating, billing runs, period closing, cancellation, corrections | in progress |
| M3+ | Payments and allocation, penalties, Kafka outbox, security, metrics, MCP | planned |

M2 is still open: XLSX export and the demo data generator (both need new libraries, not yet approved), and performance measurements that depend on the demo data.

## Stack

Java 25 (Gradle toolchain), Spring Boot 4.1, Spring Web MVC, Spring Data JPA with Hibernate 7, PostgreSQL 17, Liquibase, Hibernate Envers, MapStruct, springdoc-openapi, Testcontainers 2, JUnit 5, AssertJ, Gradle 9 (Kotlin DSL), GitHub Actions.

## Run locally

Requirements: JDK 25, Docker.

```bash
cp .env.example .env          # set POSTGRES_PASSWORD for local use
docker compose up -d          # PostgreSQL on localhost:5432
./gradlew :billing-service:bootRun
```

The service listens on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`. Ready-made requests are in [docs/api-examples.http](docs/api-examples.http).

Without a local database, `./gradlew :billing-service:bootTestRun` starts the service on Testcontainers.

## Build and checks

```bash
./gradlew check                                          # all tests, JaCoCo report
./gradlew :billing-service:test --tests "*ProrationCalculatorTest"
```

Tests run against a real PostgreSQL in Testcontainers, so Docker must be running. An H2 database does not support the exclusion constraint and the partial indexes used here.

## Modules

The code is a modular monolith in `billing-service`, with packages by feature. Inside a module: `api` (public interface and DTOs), `web` (controllers), `domain` (entities and pure logic), `app` (transactional services), `infra` (repositories). A module uses another module only through its `api` package. Shared value types (`ValidityPeriod`, `PriceCoverage`) live in `shared`.

| Module | Responsibility |
|---|---|
| `customers` | Clients, tax ID rules, search |
| `contracts` | Plans, prices by period, contracts, items, lifecycle; `ContractDirectory` for invoicing |
| `invoicing` | Billing periods, billing runs, invoices, pro-rating, corrections |
| `shared` | Errors (RFC 9457 ProblemDetail), id generator, persistence base, period types |

See [ADR 0001](docs/adr/0001-modular-monolith.md). The boundary rule is currently enforced by code review only; an ArchUnit test is planned.

## API overview

All endpoints are under `/api/v1`. Money is sent as a string (`"1500.00"`). Errors use ProblemDetail, and each business error has its own code in `type`, for example `urn:billing-hub:problem:period-closed`.

| Area | Endpoints |
|---|---|
| Customers | `POST /customers`, `GET /customers`, `GET/PATCH /customers/{id}` |
| Plans | `POST /plans`, `GET /plans`, `POST /plans/{id}/prices` |
| Contracts | `POST /contracts`, `GET /contracts`, `GET /contracts/{id}`, `POST /contracts/{id}/activate`, `POST /contracts/{id}/terminate`, `POST /contracts/{id}/items`, `PATCH /contracts/{id}/items/{itemId}` |
| Billing periods | `GET /billing-periods`, `POST /billing-periods/{yyyy-MM}/close` |
| Billing runs | `POST /billing-runs` (202, runs in the background), `GET /billing-runs/{id}` |
| Invoices | `GET /invoices` (filters: `customerId`, `contractId`, `status`, `period`), `GET /invoices/{id}`, `POST /invoices/{id}/cancel`, `POST /invoices/corrections` |

## Business rules implemented

| Rule | What it means here | Where it is checked |
|---|---|---|
| BR-01 | Money is `BigDecimal` / `numeric(19,2)`; each invoice line is rounded HALF_UP to cents | `ProrationCalculatorTest` |
| BR-02 | Line = monthly price × quantity × days / days in month | `ProrationCalculatorTest`, `BillingRunLifecycleTest` |
| BR-03 | A price change inside the month splits the month into lines | `ProrationCalculatorTest` |
| BR-04 | One regular invoice per contract and period, unless cancelled; re-runs skip such contracts | `InvoiceSchemaTest`, `BillingRunLifecycleTest` |
| BR-05 | A closed period and all earlier periods are locked; corrections go into an open period | `BillingPeriodLockTest`, `BillingRunLifecycleTest` |
| BR-06 | Only `ISSUED → CANCELLED` is possible now; `PARTIALLY_PAID` and `PAID` come with payments in M3 | `BillingRunLifecycleTest` |

Other rules from the plan (payments, FIFO allocation, penalties, dunning, events, security, XLSX export) are not implemented yet.

## Known limitations

- **Termination does not cut items.** A terminated contract is still billed for the full validity of its items. This needs a decision before the first real billing cycle.
- **Cancellation ignores allocations.** There are no payments yet. When M3 adds them, cancellation must be refused for invoices that have payments.
- **Concurrent runs for one period.** The check "no running run for this period" and the insert are not atomic. Two simultaneous requests can start two runs. The unique index on invoices still prevents duplicate regular invoices, but the run statistics would be wrong.
- **Failed runs are not resumed.** A run that was still `RUNNING` when the service stopped is marked `FAILED` at startup. Re-run the period to finish it; contracts already invoiced are skipped.
- **Invoice numbers can have gaps.** The number is taken from a sequence before the invoice is saved, so a rolled-back transaction leaves a gap.
- **`closed_by` is empty** until authentication (M6).
- **No overdue filter** on `GET /invoices` yet; it needs payments.

## Tests

- Unit tests for pure logic: pro-rating, price coverage, validity periods, tax ID, status transitions.
- Slice tests: JPA constraints, web layer with mocks.
- Integration tests through HTTP on PostgreSQL: contract lifecycle, billing runs, period locking, corrections.

Some tests close billing periods. Such a test uses `@DirtiesContext`, so its locked periods do not leak into other test classes.

## How AI was used

The code was written with Claude Code, step by step against [docs/PLAN.md](docs/PLAN.md). The author set the plan, the business rules and the module boundaries, and reviewed each change. `ProrationCalculator` and its design (cutting by price periods, rounding per line, failing on a price gap) were written by the author; the assistant only reviewed it. `./gradlew check` runs before each commit.

## Русское резюме

billing-hub — учебный B2B-биллинг подписок на Java 25 и Spring Boot 4. Клиенты заключают договоры с позициями (тарифный план, количество, свой период). Система ежемесячно выставляет счета пропорционально дням и изменениям цен внутри месяца.

Готово: клиенты, планы с ценами по периодам, договоры с жизненным циклом и аудитом (M1); выставление счетов, закрытие периода, отмена и корректировки (M2, частично). Не готово: XLSX-выгрузка и демо-данные для замеров в M2 (нужны новые библиотеки), платежи, пени, Kafka, безопасность, метрики, MCP.

License: Apache-2.0 (to be added).
