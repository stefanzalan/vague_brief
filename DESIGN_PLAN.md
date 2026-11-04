# System Design & Execution Plan: Alert Notification Platform

## 1. Executive Summary & Problem Scope
This document outlines the architecture, deliverables, and engineering roadmap for the Alert Notification Platform. Built from an intentionally vague brief ("we want users to be able to set up alerts... email and Slack... flexible... admin view"), this system translates ambiguity into a robust, production-grade, extensible enterprise backend using **Java 21** and **Spring Boot 4**.

### Core Objectives
* **Extensibility:** Support multiple notification channels (Email, Slack) via a clean Strategy Pattern, making future additions (e.g., Teams, Webhooks, SMS) seamless without modifying existing core logic.
* **Resilience & Concurrency:** Leverage Java 21 Virtual Threads (`spring.threads.virtual.enabled=true`) for non-blocking concurrent channel dispatching. If one channel fails (e.g., Slack rate-limit or timeout), other channels must still succeed.
* **Admin Management:** Provide robust RESTful APIs to manage alert rules, view configuration states, and trigger test events.
* **Auditability & AI Governance:** Transparently document prompt history, architectural decisions, and rigorous audits of AI-generated code to satisfy the interviewer's evaluation criteria.

---

## 2. High-Level Architecture

```
+-----------------------------------------------------------------+
|                    Admin Client / Test Script                   |
+--------------------------------|--------------------------------+
                                 | REST APIs (CRUD Rules & Ingest)
                                 v
+-----------------------------------------------------------------+
|                    Spring Boot 4 Application                    |
|                                                                 |
|  +--------------------+       +------------------------------+  |
|  | Rule Engine Core   |------>| Notification Dispatcher      |  |
|  +--------------------+       | (Java 21 Virtual Threads)    |  |
|                               +-------+--------------+-------+  |
|                                       |              |          |
|                       +---------------+              +----------+
|                       v                                         v
|         +---------------------------+             +---------------------------+
|         | Email Channel Adapter     |             | Slack Channel Adapter     |
|         | (Spring Mail -> Mailpit)  |             | (RestClient -> Webhook)   |
|         +---------------------------+             +---------------------------+
+-----------------------------------------------------------------+
```

---

## 3. Technology Stack & Infrastructure
* **Language & Runtime:** Java 21 (Records, Pattern Matching, Virtual Threads)
* **Framework:** Spring Boot 4.x (Web, Mail, Validation, Actuator)
* **Local Test Sinks:** 
  * Email: **Mailpit** (Docker container running on SMTP port `1025`, Web UI on `8025`)
  * Slack: Custom Incoming Webhook integration pointing to a dedicated sandbox channel
* **Build Tool:** Maven

---

## 4. Milestone Breakdown & Execution Roadmap

### Milestone 1: Domain Core, Enums, and Strategy Pattern
* **Scope:** Define immutable data models using Java Records (`Event`, `AlertRule`, `NotificationPayload`), enums (`ChannelType`, `Severity`), and the core `NotificationChannel` interface.
* **Design Pattern:** Strategy Pattern to decouple notification dispatching logic from concrete channel implementations.
* **Deliverable:** Core domain interfaces, record definitions, and unit tests verifying rule evaluation logic.

### Milestone 2: Infrastructure Adapters (Mail & Slack)
* **Scope:** Implement concrete notification channels.
  * `EmailNotificationChannel`: Integrates Spring `JavaMailSender` targeting local Mailpit.
  * `SlackNotificationChannel`: Integrates Spring 3 `RestClient` targeting the Slack Webhook URL.
* **Configuration:** Use `@ConditionalOnProperty` to allow channels to be enabled/disabled via `application.properties`.
* **Deliverable:** Channel service implementations and component tests verifying message payload transformation.

### Milestone 3: Event Ingestion & Evaluation Engine
* **Scope:** Build the engine that ingests incoming events (e.g., breaking news, market triggers) and evaluates them against registered alert rules.
* **Concurrency:** Implement asynchronous dispatching using Java 21 Virtual Thread executors.
* **Deliverable:** Event processing service, rule-matching logic, and comprehensive unit tests.

### Milestone 4: Admin Management REST APIs
* **Scope:** Expose RESTful endpoints for administrative tasks:
  * `POST /api/v1/admin/rules` - Create/Update alert rules.
  * `GET /api/v1/admin/rules` - List active rules.
  * `POST /api/v1/events/ingest` - Trigger test events for evaluation.
* **Validation:** Jakarta Validation (`@NotBlank`, `@NotNull`) and global exception handling (`@RestControllerAdvice`).
* **Deliverable:** Controller layer with `@WebMvcTest` unit tests.

### Milestone 5: Resilience, Error Handling & Documentation
* **Scope:** Implement isolated try-catch error handling per channel dispatch so failures do not cascade. Add OpenAPI 3 / Swagger documentation.
* **Deliverable:** End-to-end integration tests and production-ready configuration.

---

## 5. AI Evaluation & Audit Strategy (`AI_AUDIT_LOG.md`)
To address the evaluation criteria regarding how AI was supervised, every major Cursor-generated artifact will be logged with:
1. **The AI's Proposal / Shortcut:** Identifying generated deprecated code, missing boundary checks, or over-engineered dependencies.
2. **The Architect's Correction:** Refactoring choices made to enforce best practices, security, and low memory overhead.
3. **Verification Method:** Unit test or terminal sanity check (`curl`, PowerShell `Invoke-RestMethod`, Mailpit UI inspection).