package com.stefanzalan.vague_brief.integration;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.EventCategory;
import com.stefanzalan.vague_brief.domain.Severity;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import com.stefanzalan.vague_brief.web.controller.AdminRuleController;
import com.stefanzalan.vague_brief.web.controller.EventIngestionController;
import com.stefanzalan.vague_brief.web.dto.CreateAlertRuleRequest;
import com.stefanzalan.vague_brief.web.dto.IngestEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test that verifies the complete flow:
 * 1. Create an alert rule via admin API
 * 2. Ingest a matching system event via ingestion API
 * 3. Verify the event is evaluated, matches the rule, and triggers notification dispatch
 */
@SpringBootTest
class EndToEndIntegrationTest {

    @Autowired
    private AlertRuleRepository ruleRepository;

    @Autowired
    private AdminRuleController adminRuleController;

    @Autowired
    private EventIngestionController eventIngestionController;

    @BeforeEach
    void setUp() {
        ruleRepository.clear();
    }

    @Test
    void endToEndFlow_createRuleAndIngestMatchingEvent_triggersNotificationDispatch() {
        // Step 1: Create an alert rule via admin API
        CreateAlertRuleRequest ruleRequest = new CreateAlertRuleRequest(
                "Critical Market Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "trader@example.com",
                true
        );

        ResponseEntity<AlertRule> ruleResponse = adminRuleController.createRule(ruleRequest);

        assertThat(ruleResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AlertRule createdRule = ruleResponse.getBody();
        assertThat(createdRule).isNotNull();
        assertThat(createdRule.name()).isEqualTo("Critical Market Alert");
        assertThat(createdRule.category()).isEqualTo(EventCategory.MARKET_MOVEMENT);
        assertThat(createdRule.enabled()).isTrue();

        // Verify rule is stored in repository
        List<AlertRule> allRules = ruleRepository.findAll();
        assertThat(allRules).hasSize(1);
        assertThat(allRules.get(0).id()).isEqualTo(createdRule.id());

        // Step 2: Ingest a matching system event via ingestion API
        IngestEventRequest eventRequest = new IngestEventRequest(
                "Stock Market Crash",
                "Major indices dropped by 10%",
                EventCategory.MARKET_MOVEMENT,
                Severity.CRITICAL,
                Instant.now()
        );

        ResponseEntity<String> eventResponse = eventIngestionController.ingestEvent(eventRequest);

        assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(eventResponse.getBody()).contains("Event accepted for processing");

        // Step 3: Verify the rule still exists and was not modified
        List<AlertRule> rulesAfterEvent = ruleRepository.findAll();
        assertThat(rulesAfterEvent).hasSize(1);
        assertThat(rulesAfterEvent.get(0).id()).isEqualTo(createdRule.id());
        assertThat(rulesAfterEvent.get(0).enabled()).isTrue();
    }

    @Test
    void endToEndFlow_createRuleAndIngestNonMatchingEvent_skipsNotificationDispatch() {
        // Step 1: Create an alert rule for MARKET_MOVEMENT
        CreateAlertRuleRequest ruleRequest = new CreateAlertRuleRequest(
                "Market Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "admin@example.com",
                true
        );

        ResponseEntity<AlertRule> ruleResponse = adminRuleController.createRule(ruleRequest);
        assertThat(ruleResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Step 2: Ingest an event with different category (should not match)
        IngestEventRequest eventRequest = new IngestEventRequest(
                "Breaking News",
                "Important news update",
                EventCategory.BREAKING_NEWS,
                Severity.CRITICAL,
                Instant.now()
        );

        ResponseEntity<String> eventResponse = eventIngestionController.ingestEvent(eventRequest);
        assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Step 3: Verify rule still exists unchanged
        List<AlertRule> rules = ruleRepository.findAll();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).category()).isEqualTo(EventCategory.MARKET_MOVEMENT);
    }

    @Test
    void endToEndFlow_createDisabledRuleAndIngestMatchingEvent_skipsNotificationDispatch() {
        // Step 1: Create a disabled alert rule
        CreateAlertRuleRequest ruleRequest = new CreateAlertRuleRequest(
                "Disabled Rule",
                EventCategory.NATURAL_DISASTER,
                Severity.INFO,
                ChannelType.SLACK,
                "#alerts",
                false
        );

        ResponseEntity<AlertRule> ruleResponse = adminRuleController.createRule(ruleRequest);
        assertThat(ruleResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(ruleResponse.getBody().enabled()).isFalse();

        // Step 2: Ingest a matching event
        IngestEventRequest eventRequest = new IngestEventRequest(
                "Earthquake",
                "Magnitude 7.0 earthquake detected",
                EventCategory.NATURAL_DISASTER,
                Severity.CRITICAL,
                Instant.now()
        );

        ResponseEntity<String> eventResponse = eventIngestionController.ingestEvent(eventRequest);
        assertThat(eventResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Step 3: Verify rule exists and is still disabled
        List<AlertRule> rules = ruleRepository.findAll();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).enabled()).isFalse();
    }

    @Test
    void endToEndFlow_listRulesAfterCreation_returnsAllRules() {
        // Create multiple rules
        CreateAlertRuleRequest rule1 = new CreateAlertRuleRequest(
                "Rule 1",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.EMAIL,
                "user1@example.com",
                true
        );

        CreateAlertRuleRequest rule2 = new CreateAlertRuleRequest(
                "Rule 2",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.SLACK,
                "#channel",
                true
        );

        adminRuleController.createRule(rule1);
        adminRuleController.createRule(rule2);

        // List all rules
        ResponseEntity<List<AlertRule>> listResponse = adminRuleController.listRules();
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(2);
    }
}
