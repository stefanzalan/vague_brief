# Prompt History & Architectural Oversight Log

This document serves as a complete, transparent record of the prompt engineering workflow, architectural design process, and exact instructions used during the development of the **AlertNotifier** platform.

---

## 1. Collaborative Design & System Planning

Prior to code generation, the system architecture was thoroughly designed through collaborative discussions to translate a vague brief ("an alert notification system with email and Slack") into a resilient, production-ready enterprise application.

Key architectural decisions established during this phase include:
- **Language & Framework:** Java 21 and Spring Boot 4.
- **Design Pattern:** Strategy Pattern for decoupled, extensible channel adapters (`NotificationChannelService` and `NotificationChannelRegistry`).
- **Concurrency & Resilience:** Asynchronous dispatches powered by Java 21 **Virtual Threads** (`Executors.newVirtualThreadPerTaskExecutor()`) to prevent failing or slow network calls in one channel from blocking others.
- **Local Sink Infrastructure:** Docker containerized **Mailpit** (SMTP port `1025`, Web UI `8025`) and Slack Webhooks.
- **Persistence Strategy:** Pragmatic thread-safe in-memory storage to prioritize runtime speed and self-contained execution without external DB dependencies.

All initial design specifications and architectural blueprints were compiled into [`DESIGN_PLAN.md`](./DESIGN_PLAN.md).

---

## 2. Milestone Prompt History

Below is the verbatim record of all Senior Architect prompts provided to Cursor to drive the implementation phase by phase.

---

### Milestone 1: Domain Core, Enums, and Strategy Pattern

```markdown
# Role & Context
You are an expert Senior Java 21 and Spring Boot 3 Software Architect. We are building a robust, enterprise-grade, extensible multi-channel alerting system ("AlertNotifier") from a vague product brief. 

# Objective for Milestone 1: Core Domain Model, Enums, and Strategy Pattern
Implement the foundational domain models, enums, core exception handling, and the Strategy Pattern for multi-channel notifications. 

## Requirements & Design Specifications:

1. **Java 21 Records & Immutability:**
   - Use Java 21 `record` types for all immutable domain payloads, DTOs, and event structures where appropriate.

2. **Domain Enums:**
   - `ChannelType`: Values for `EMAIL`, `SLACK` (designed to be easily extensible for future channels).
   - `Severity`: Values for `INFO`, `WARNING`, `CRITICAL`.
   - `EventCategory`: Values for `BREAKING_NEWS`, `MARKET_MOVEMENT`, `NATURAL_DISASTER`.

3. **Core Domain Entities / Records:**
   - `AlertRule`: Represents a rule configured by an admin (id, name, category, minimum severity, target channel, recipient/webhook destination, enabled status).
   - `SystemEvent`: Represents an incoming event to evaluate (id, title, description, category, severity, timestamp).
   - `NotificationPayload`: The standardized container passed to channels for dispatching (recipient, subject, message body, severity).

4. **Strategy Pattern Interface:**
   - Define a `NotificationChannelService` interface with:
     - `ChannelType getSupportedChannel();`
     - `void sendNotification(NotificationPayload payload);`
   - Create a factory/registry component (`NotificationChannelRegistry`) that injects all available implementations of `NotificationChannelService` into a `Map<ChannelType, NotificationChannelService>` for O(1) runtime lookup.

5. **Validation & Error Handling:**
   - Use `jakarta.validation` annotations on models/records.
   - Implement custom domain exceptions (e.g., `UnsupportedChannelException`, `RuleNotFoundException`) and a global `@RestControllerAdvice` stub or basic exception handler for clean error responses.

6. **Unit Tests (MANDATORY):**
   - Write comprehensive JUnit 5 and AssertJ unit tests for:
     - The `NotificationChannelRegistry` verifying correct routing and handling of unsupported channels.
     - Validation rules on domain objects.
   - Ensure all tests pass successfully.

# Acceptance Criteria
- Code compiles cleanly with Java 21 / Maven.
- No legacy Spring patterns or deprecated APIs (use modern lambda DSLs and constructor injection).
- `mvn clean test` must pass successfully with zero failures.
```

---

### Milestone 2: Infrastructure Adapters (Mailpit & Slack)

```markdown
# Role & Context
You are an expert Senior Java 21 and Spring Boot 4 Software Architect. We are building the `AlertNotifier` multi-channel notification system. Milestone 1 (Domain core and Strategy registry) is already fully implemented and tested.

# Objective for Milestone 2: Infrastructure Adapters (Mailpit & Slack)
Implement the concrete notification channel adapters for **Email** and **Slack** by implementing the existing `NotificationChannelService` interface.

## Requirements & Design Specifications:

1. **Email Notification Channel (`EMAIL`):**
   - Implement `EmailNotificationChannel` annotated with `@Service`.
   - Override `getSupportedChannel()` to return `ChannelType.EMAIL`.
   - Inject Spring's `JavaMailSender` to send outbound emails to our local Mailpit container (`localhost:1025`).
   - Construct clean MIME or Simple Mail Messages from the standardized `NotificationPayload`.
   - Handle mail dispatch failures gracefully with try-catch blocks and SLF4J logging (do not throw runtime exceptions up to the event engine unless required by policy).

2. **Slack Notification Channel (`SLACK`):**
   - Implement `SlackNotificationChannel` annotated with `@Service`.
   - Override `getSupportedChannel()` to return `ChannelType.SLACK`.
   - Use Spring's modern `RestClient` to post JSON payloads to the Slack Incoming Webhook URL configured in `application.properties` (`notification.slack.webhook-url`).
   - Use a Java 21 `record` (`SlackWebhookPayload`) for clean JSON serialization.
   - Respect the toggle `notification.slack.enabled` and skip/log a warning if the webhook URL is a placeholder or disabled.

3. **Resilience & Error Handling:**
   - Ensure network errors or timeout exceptions during email/slack transmission are caught, logged with full context, and handled gracefully so one failing channel doesn't block the system.

4. **Unit & Component Tests (MANDATORY):**
   - Write unit/mock tests using Mockito and JUnit 5 for both `EmailNotificationChannel` and `SlackNotificationChannel`.
   - Verify that `JavaMailSender` and `RestClient` (or mocked HTTP exchanges) are correctly invoked with the expected parameters from a given `NotificationPayload`.
   - Ensure all tests pass successfully (`mvn clean test`).

# Acceptance Criteria
- Both adapters correctly register themselves with the existing `NotificationChannelRegistry`.
- Code compiles cleanly with Java 21 and Spring Boot.
- `mvn clean test` must pass successfully with zero failures.
```

---

### Milestone 3: Event Detection & Evaluation Engine

```markdown
# Role & Context
You are an expert Senior Java 21 and Spring Boot 4 Software Architect. We are building the `AlertNotifier` multi-channel notification system. Milestones 1 and 2 (Domain core, Strategy registry, Email, and Slack adapters) are fully implemented and tested.

# Objective for Milestone 3: Event Detection & Evaluation Engine
Implement the core event evaluation engine that matches incoming system events against configured alert rules and asynchronously dispatches notifications to the appropriate channels.

## Requirements & Design Specifications:

1. **Rule Repository / Management Store:**
   - Implement an in-memory rule repository or service (`AlertRuleRepository` / `AlertRuleService`) that stores and retrieves active `AlertRule` configurations. Provide a method to seed default rules or add new ones for testing.

2. **Event Evaluation Engine (`EventEvaluationEngine`):**
   - Create a service that processes an incoming `SystemEvent`.
   - For a given event, fetch all enabled rules.
   - **Matching Logic:** A rule matches an event if:
     - The rule's category matches the event's category (or supports a wildcard/all category).
     - The event's severity meets or exceeds the rule's minimum severity threshold (utilizing the `Severity` comparison logic from Milestone 1).
   - For every matching rule, map the event and rule destination to a `NotificationPayload` and invoke the correct `NotificationChannelService` from the `NotificationChannelRegistry`.

3. **Asynchronous Dispatch with Java 21 Virtual Threads:**
   - Ensure that dispatching notifications to channels (which involve network/SMTP calls) runs asynchronously using Java 21 **Virtual Threads** (e.g., via `Executors.newVirtualThreadPerTaskExecutor()` or Spring's `@Async` configured with a Virtual Thread executor). Event evaluation must not block on slow channel responses.

4. **Unit & Integration Tests (MANDATORY):**
   - Write comprehensive JUnit 5 and Mockito tests for `EventEvaluationEngine`.
   - Verify that:
     - Matching events trigger the correct channel service.
     - Non-matching events (wrong category or severity too low) are ignored and do not trigger notifications.
     - Disabled rules are skipped.
     - Multiple channels are dispatched concurrently.
   - Ensure all tests pass successfully (`mvn clean test`).

# Acceptance Criteria
- Code compiles cleanly with Java 21 and Spring Boot.
- `mvn clean test` must pass successfully with zero failures.
```

---

### Milestone 4: Admin Management REST API & Event Ingestion Endpoints

```markdown
# Role & Context
You are an expert Senior Java 21 and Spring Boot 4 Software Architect. We are building the `AlertNotifier` multi-channel notification system. Milestones 1, 2, and 3 (Domain core, Strategy registry, Email/Slack adapters, and Event Evaluation Engine with Virtual Threads) are fully implemented and tested.

# Objective for Milestone 4: Admin Management REST API & Event Ingestion Endpoints
Implement the REST controller layer to satisfy the admin management and event ingestion requirements from the product brief.

## Requirements & Design Specifications:

1. **Admin Rule Management Controller (`AdminRuleController`):**
   - Base URL: `/api/v1/admin/rules`
   - Endpoints:
     - `POST /`: Create a new `AlertRule` (with Jakarta `@Valid` validation). Returns HTTP 201 Created.
     - `GET /`: List all configured alert rules. Returns HTTP 200 OK.
     - `GET /{id}`: Get a specific alert rule by ID. Returns HTTP 200 OK or throws `RuleNotFoundException` (handled by GlobalExceptionHandler).
     - `DELETE /{id}`: Delete or disable an alert rule. Returns HTTP 204 No Content.

2. **Event Ingestion Controller (`EventIngestionController`):**
   - Base URL: `/api/v1/events`
   - Endpoints:
     - `POST /ingest`: Receive an incoming `SystemEvent` payload (validated via `@Valid`), pass it to the `EventEvaluationEngine` for rule matching and async notification dispatch, and return HTTP 202 Accepted with a confirmation status message.

3. **DTOs & Mapping:**
   - Create clean request DTO records for rule creation and event ingestion, keeping them separate from domain records if necessary, with robust validation annotations (`@NotBlank`, `@NotNull`, `@Min`, etc.).

4. **Controller & Web Unit Tests (MANDATORY):**
   - Write `@WebMvcTest` slices for both controllers.
   - Verify proper request validation (e.g., sending invalid JSON payloads results in HTTP 400 Bad Request handled via `ProblemDetail`).
   - Mock service dependencies to ensure clean isolation.
   - Ensure all tests pass successfully (`mvn clean test`).

# Acceptance Criteria
- Controllers follow REST conventions and return proper HTTP status codes.
- Code compiles cleanly with Java 21 and Spring Boot 4.
- `mvn clean test` must pass successfully with zero failures.
```

---

### Milestone 5: OpenAPI Documentation, Resilience, & Integration Testing

```markdown
# Role & Context
You are an expert Senior Java 21 and Spring Boot 4 Software Architect. We are building the `AlertNotifier` multi-channel notification system. Milestones 1 through 4 (Domain core, Strategy registry, Email/Slack adapters, Event Evaluation Engine with Virtual Threads, and Admin/Ingestion REST Controllers) are fully implemented, in-memory, and tested.

# Objective for Milestone 5: OpenAPI Documentation, Resilience, & Integration Testing
Finalize the production readiness of the application by adding OpenAPI documentation, hardening the async notification dispatch with error resilience, and adding an end-to-end integration test.

## Requirements & Design Specifications:

1. **OpenAPI 3 / Swagger Documentation:**
   - Add SpringDoc OpenAPI starter (`springdoc-openapi-starter-webmvc-ui` compatible with Spring Boot 4).
   - Configure Swagger UI to be accessible at `/swagger-ui.html` or `/swagger-ui/index.html`.
   - Annotate controllers (`AdminRuleController`, `EventIngestionController`) with clear `@Operation`, `@Tag`, and `@ApiResponse` descriptions so the API is fully self-documenting for an admin view.

2. **Resilience & Dispatch Hardening:**
   - Ensure that if one notification channel fails (e.g., Slack throws an exception), it is caught locally within the channel service, logged cleanly via SLF4J, and does **not** prevent other channels (e.g., Email) from executing for the same event.
   - Verify that the async execution pool using Java 21 Virtual Threads is cleanly configured (e.g., via a custom `AsyncTaskExecutor` bean or `Executors.newVirtualThreadPerTaskExecutor()`).

3. **End-to-End Integration Test (MANDATORY):**
   - Write a full Spring Boot integration test (`@SpringBootTest` with `WebEnvironment.RANDOM_PORT` or MockMvc) that:
     - Creates an alert rule via the admin API.
     - Ingests a matching system event via the ingestion API.
     - Asserts that the event successfully evaluates, matches the rule, and triggers the underlying notification channel services.
   - Ensure all tests pass successfully (`mvn clean test`).

# Acceptance Criteria
- OpenAPI UI loads and documents all endpoints correctly.
- Code compiles cleanly with Java 21 and Spring Boot 4.
- `mvn clean test` passes with zero failures.
```