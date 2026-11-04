package com.stefanzalan.vague_brief.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validAlertRuleHasNoViolations() {
        AlertRule rule = validAlertRule();

        assertThat(validator.validate(rule)).isEmpty();
    }

    @Test
    void alertRuleRejectsBlankNameAndDestination() {
        AlertRule rule = new AlertRule(
                UUID.randomUUID(),
                "  ",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.EMAIL,
                "",
                true
        );

        assertThat(propertyPaths(validator.validate(rule)))
                .containsExactlyInAnyOrder("name", "destination");
    }

    @Test
    void alertRuleRejectsNullRequiredFields() {
        AlertRule rule = new AlertRule(null, "Market alerts", null, null, null, "ops@example.com", true);

        assertThat(propertyPaths(validator.validate(rule)))
                .containsExactlyInAnyOrder("id", "category", "minimumSeverity", "targetChannel");
    }

    @Test
    void validSystemEventHasNoViolations() {
        assertThat(validator.validate(validSystemEvent())).isEmpty();
    }

    @Test
    void systemEventRejectsBlankTitleAndDescription() {
        SystemEvent event = new SystemEvent(
                UUID.randomUUID(),
                "",
                "   ",
                EventCategory.NATURAL_DISASTER,
                Severity.CRITICAL,
                Instant.parse("2026-11-04T12:00:00Z")
        );

        assertThat(propertyPaths(validator.validate(event)))
                .containsExactlyInAnyOrder("title", "description");
    }

    @Test
    void systemEventRejectsNullRequiredFields() {
        SystemEvent event = new SystemEvent(null, "Quake", "Magnitude 7.1", null, null, null);

        assertThat(propertyPaths(validator.validate(event)))
                .containsExactlyInAnyOrder("id", "category", "severity", "timestamp");
    }

    @Test
    void validNotificationPayloadHasNoViolations() {
        NotificationPayload payload = new NotificationPayload(
                "ops@example.com",
                "Critical alert",
                "Market dropped 8%",
                Severity.CRITICAL
        );

        assertThat(validator.validate(payload)).isEmpty();
    }

    @Test
    void notificationPayloadRejectsBlankFieldsAndNullSeverity() {
        NotificationPayload payload = new NotificationPayload(" ", "", null, null);

        assertThat(propertyPaths(validator.validate(payload)))
                .containsExactlyInAnyOrder("recipient", "subject", "messageBody", "severity");
    }

    @Test
    void severityMeetsMinimumUsesOrdinalOrder() {
        assertThat(Severity.CRITICAL.meetsMinimum(Severity.WARNING)).isTrue();
        assertThat(Severity.WARNING.meetsMinimum(Severity.WARNING)).isTrue();
        assertThat(Severity.INFO.meetsMinimum(Severity.WARNING)).isFalse();
    }

    private static AlertRule validAlertRule() {
        return new AlertRule(
                UUID.randomUUID(),
                "Breaking news to Slack",
                EventCategory.BREAKING_NEWS,
                Severity.WARNING,
                ChannelType.SLACK,
                "https://hooks.slack.com/services/example",
                true
        );
    }

    private static SystemEvent validSystemEvent() {
        return new SystemEvent(
                UUID.randomUUID(),
                "Fed rate decision",
                "Rates held unchanged",
                EventCategory.MARKET_MOVEMENT,
                Severity.INFO,
                Instant.parse("2026-11-04T12:00:00Z")
        );
    }

    private static <T> Set<String> propertyPaths(Set<ConstraintViolation<T>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
    }
}
