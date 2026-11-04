# Alert Notifier
### From a Vague Brief

---

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-green.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An extensible, event-driven, multi-channel alert and notification engine built with **Java 21 Virtual Threads**, **Spring Boot 4**, and the **Strategy Pattern**.

---

## Executive Overview

AlertNotifier is a production-grade notification system designed to ingest system events, evaluate them against configurable alert rules, and dispatch notifications across multiple channels asynchronously. The system leverages Java 21's Project Loom (Virtual Threads) for high-concurrency, non-blocking I/O operations, making it ideal for I/O-bound workloads like email and webhook notifications.

### Core Features

- **Multi-Channel Notifications**: Built-in support for Email and Slack with extensible architecture for future channels (Teams, SMS, etc.)
- **Rule-Based Event Matching**: Configure alert rules by category, severity threshold, and target channel
- **Asynchronous Virtual Thread Execution**: Non-blocking concurrent dispatch using Java 21 virtual threads
- **Admin REST API**: Full CRUD operations for alert rule management
- **Event Ingestion API**: RESTful endpoint for submitting system events
- **OpenAPI/Swagger Documentation**: Self-documenting API with interactive Swagger UI
- **Error Resilience**: Graceful error handling with SLF4J logging to prevent single-channel failures from blocking others

---

## System Architecture & Key Design Decisions

### Strategy Pattern for Channel Extensibility

AlertNotifier employs the **Strategy Pattern** through the `NotificationChannelService` interface and `NotificationChannelRegistry`. This design adheres to the **Open/Closed Principle**, allowing new notification channels to be added without modifying existing code:

```
NotificationChannelService (interface)
├── EmailNotificationChannel
└── SlackNotificationChannel
```

To add a new channel (e.g., Microsoft Teams):
1. Implement `NotificationChannelService`
2. Annotate with `@Service`
3. The registry automatically discovers and registers the channel

### Java 21 Virtual Threads

The system uses Java 21's **Virtual Threads** for concurrent notification dispatch:

```java
Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
```

Virtual threads provide:
- Massive scalability (millions of concurrent operations)
- Non-blocking I/O for email/Slack notifications
- Simplified concurrency model compared to traditional thread pools

### Architectural Documentation

For detailed system architecture, design patterns, and milestone blueprints, refer to:
- **[DESIGN_PLAN.md](DESIGN_PLAN.md)** - Complete architectural plan and milestone blueprint
- **[AI_AUDIT_LOG.md](AI_AUDIT_LOG.md)** - AI decision log, human oversight, challenged assumptions, and sanity checks
- **[PROMPTS.md](PROMPTS.md)** - Complete prompt history used during development

---

## Prerequisites & Infrastructure Setup

### Requirements

- **Java 21+** (JDK 21 or later)
- **Maven 3.9+**
- **Docker & Docker Compose** (for Mailpit infrastructure)

### Infrastructure: Mailpit Container

AlertNotifier uses **Mailpit** as a local SMTP server for email testing. Mailpit provides both an SMTP server (port 1025) and a web UI (port 8025) for viewing sent emails.

#### Start Infrastructure

```bash
docker compose up -d
```

This starts Mailpit with:
- **SMTP Server**: `localhost:1025`
- **Web UI**: `http://localhost:8025`

#### Stop Infrastructure

```bash
docker compose down
```

---

## Configuration

The application is configured via `src/main/resources/application.properties`. Below are the key configuration settings for local development:

```properties
spring.application.name=vague_brief

# Spring Mail Configuration for local Mailpit
spring.mail.host=localhost
spring.mail.port=1025
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email Notification Configuration
notification.email.enabled=true
notification.email.from=alerts@alertnotifier.local

# Slack Notification Configuration
notification.slack.enabled=true
notification.slack.webhook-url=${SLACK_WEBHOOK_URL:}
notification.slack.bot-token=${SLACK_BOT_TOKEN:}
notification.slack.signing-secret=${SLACK_SIGNING_SECRET:}
```

### Environment Variables

For production deployments, configure the following environment variables:

```bash
export SPRING_MAIL_HOST=smtp.example.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=your-email@example.com
export SPRING_MAIL_PASSWORD=your-password
export NOTIFICATION_EMAIL_FROM=alerts@example.com
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
export SLACK_BOT_TOKEN=xoxb-your-bot-token
export SLACK_SIGNING_SECRET=your-signing-secret
```

---

## Building & Running the Application

### Run Tests

Execute the full test suite (including unit, integration, and end-to-end tests):

```bash
mvn clean test
```

Expected output:
```
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### Build the Application

```bash
mvn clean package
```

### Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## Interactive API & Swagger UI Usage

AlertNotifier provides a fully interactive API documentation via **Swagger UI**.

### Access Swagger UI

- **URL**: `http://localhost:8080/swagger-ui/index.html`
- **Alternative URL**: `http://localhost:8080/swagger-ui.html`

The Swagger UI allows you to:
- Explore all available endpoints
- View request/response schemas
- Execute API calls directly from the browser
- Download OpenAPI specification

### API Endpoints

#### Admin Rules API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/admin/rules` | Create a new alert rule |
| GET | `/api/v1/admin/rules` | List all alert rules |
| GET | `/api/v1/admin/rules/{id}` | Get a specific rule by ID |
| DELETE | `/api/v1/admin/rules/{id}` | Delete a rule by ID |

#### Event Ingestion API

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/events/ingest` | Ingest a system event for evaluation |

### Sample cURL Commands

#### Create an Alert Rule

```bash
curl -X POST http://localhost:8080/api/v1/admin/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Critical Market Alert",
    "category": "MARKET_MOVEMENT",
    "minimumSeverity": "WARNING",
    "targetChannel": "EMAIL",
    "destination": "trader@example.com",
    "enabled": true
  }'
```

**Response** (201 Created):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Critical Market Alert",
  "category": "MARKET_MOVEMENT",
  "minimumSeverity": "WARNING",
  "targetChannel": "EMAIL",
  "destination": "trader@example.com",
  "enabled": true
}
```

#### Ingest a System Event

```bash
curl -X POST http://localhost:8080/api/v1/events/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Stock Market Crash",
    "description": "Major indices dropped by 10%",
    "category": "MARKET_MOVEMENT",
    "severity": "CRITICAL",
    "timestamp": "2024-01-15T10:30:00Z"
  }'
```

**Response** (202 Accepted):
```
Event accepted for processing: 550e8400-e29b-41d4-a716-446655440001
```

#### List All Rules

```bash
curl http://localhost:8080/api/v1/admin/rules
```

#### Delete a Rule

```bash
curl -X DELETE http://localhost:8080/api/v1/admin/rules/{rule-id}
```

---

## Project Documentation Index

| Document | Description |
|----------|-------------|
| **[DESIGN_PLAN.md](DESIGN_PLAN.md)** | Complete architectural plan, design patterns, and milestone blueprint for the AlertNotifier system. |
| **[AI_AUDIT_LOG.md](AI_AUDIT_LOG.md)** | AI decision log documenting human oversight, challenged assumptions, architectural adaptations, and sanity checks performed during development. |
| **[PROMPTS.md](PROMPTS.md)** | Complete prompt history used during the development process, providing transparency into the AI-assisted development workflow. |
