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