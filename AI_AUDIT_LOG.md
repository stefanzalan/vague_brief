# AI Audit & Decision Log (AlertNotifier)

This document records the human-in-the-loop oversight, sanity checks, architectural corrections, and validated AI behavior during the development process (using Cursor with Senior Java 21 / Spring Boot 4 architectural constraints).

---

## Milestone 1: Core Domain Model, Enums, & Strategy Pattern

* **AI Output Summary:** Cursor successfully generated domain enums (`ChannelType`, `Severity`, `EventCategory`), Java 21 immutable records (`AlertRule`, `SystemEvent`, `NotificationPayload`), custom exceptions, the `NotificationChannelRegistry` using Spring constructor injection for strategy resolution, and a global `@RestControllerAdvice` leveraging Spring `ProblemDetail`. It also wrote 20 passing unit tests.
* **AI Architecture & Dependency Validation:**
    * *Observation:* Cursor correctly identified that under Spring Boot 4, the legacy `spring-boot-starter-web` has been modularized/deprecated, and correctly opted to use `spring-boot-starter-webmvc`.
    * *Human Review:* Correctly used webmvc starter instead of deprecated web starter. Tests and build ran successfully. Java 21 domain records used correctly.
* **Sanity Checks Performed:**
    * Ran `mvn clean test` to ensure all 20 strategy registry and validation unit tests executed and passed cleanly.
    * Verified that `NotificationChannelRegistry` correctly handles duplicate bean registrations and throws `UnsupportedChannelException` on unknown channels.

---

## Milestone 2: Infrastructure Adapters (Mailpit & Slack)

* **AI Output Summary:** Cursor successfully implemented `EmailNotificationChannel` (using `JavaMailSender` configured for local Mailpit) and `SlackNotificationChannel` (using Spring `RestClient` and a Java 21 `SlackWebhookPayload` record). Both adapters cleanly implement the `NotificationChannelService` interface and register automatically into the `NotificationChannelRegistry`. Graceful exception handling and SLF4J logging were introduced to swallow transport failures, preventing one failing channel from blocking others.
* **AI Architecture & Adaptation:**
  * *Jackson Converter Adaptation:* Cursor initially encountered test failures with legacy Jackson 2 HTTP converters and automatically adapted by aligning with Spring Boot 4's updated Jackson 3 HTTP converter specifications.
  * *Maven Surefire Test Discovery Fix:* Cursor noted that the Maven Surefire plugin ignores classes ending in `IT` (Integration Test) during standard `mvn test` phases and appropriately adjusted the adapter test naming convention so validation executes immediately during the build phase.
  * *Human Review:* Both NotificationChannels are implemented correctly. Tests and build passed successfully.
* **Sanity Checks Performed:**
  * Verified unit tests using Mockito and Spring's `MockRestServiceServer` for the Slack client.
  * Ran `mvn clean test` to confirm all unit tests and registry integration wiring compiled and passed with zero failures.

---

## Milestone 3: Event Detection & Evaluation Engine

* **AI Output Summary:** Cascade successfully implemented the core event evaluation engine with:
  - `AlertRuleRepository`: Thread-safe in-memory repository using `ConcurrentHashMap` for storing and retrieving alert rules
  - `EventEvaluationEngine`: Service that processes incoming `SystemEvent` objects, matches them against enabled rules based on category and severity thresholds, and asynchronously dispatches notifications
  - `AsyncConfiguration`: Spring configuration providing a Java 21 Virtual Thread executor (`Executors.newVirtualThreadPerTaskExecutor()`) for non-blocking concurrent channel dispatching
  - Matching logic: Rules match events when category equals and event severity meets or exceeds the rule's minimum severity threshold
  - Comprehensive unit tests (9 test cases) verifying matching behavior, disabled rule handling, concurrent dispatch to multiple channels, and error isolation
* **AI Architecture & Design:**
  - Used constructor injection for all dependencies following Spring Boot 4 best practices
  - Implemented graceful error handling in dispatch method to prevent one failing channel from blocking others
  - Built formatted notification payloads with structured message bodies containing rule name, event details, and timestamps
  - Virtual thread executor ensures I/O-bound operations (email/Slack) don't block the evaluation thread
* **Sanity Checks Performed:**
  - Wrote 9 comprehensive JUnit 5 tests using Mockito to verify rule matching logic, category filtering, severity thresholding, disabled rule skipping, and concurrent multi-channel dispatch
  - Fixed missing `java.util.List` import in test file
  - Applied `@MockitoSettings(strictness = Strictness.LENIENT)` to handle unused stubs in shared setUp method
  - Ran `mvn clean test` - all 40 tests passed successfully (including 9 new EventEvaluationEngine tests)

---

## Milestone 4: Admin Management REST API & Event Ingestion Endpoints

* **AI Output Summary:** Cascade successfully implemented the REST controller layer with:
  - `CreateAlertRuleRequest` and `IngestEventRequest` DTOs with Jakarta validation annotations
  - `AdminRuleController` with CRUD endpoints: POST / (create), GET / (list), GET /{id} (get by ID), DELETE /{id} (delete)
  - `EventIngestionController` with POST /ingest endpoint for event processing
  - Proper HTTP status codes: 201 Created, 200 OK, 202 Accepted, 204 No Content, 404 Not Found
  - Exception handling via existing `GlobalExceptionHandler` for `RuleNotFoundException`
  - Unit tests using pure Mockito (without Spring test infrastructure) for both controllers
* **AI Architecture & Adaptation:**
  - Initially attempted `@WebMvcTest` with `MockMvc` but encountered Spring Boot 4 compatibility issues with test dependencies
  - Adapted to plain unit tests using `@ExtendWith(MockitoExtension.class)`, `@Mock`, and `@InjectMocks` for faster, isolated testing
  - Added `jackson-databind` to main dependencies for JSON serialization in controllers
  - Used constructor injection throughout following Spring Boot 4 best practices
* **Sanity Checks Performed:**
  - Wrote 7 unit tests for `AdminRuleController` covering create, list, get, delete operations and exception handling
  - Wrote 3 unit tests for `EventIngestionController` covering valid event processing and response verification
  - Fixed compilation errors by adding missing imports for DTO classes
  - Ran `mvn clean test` - all 50 tests passed successfully (including 10 new controller tests)