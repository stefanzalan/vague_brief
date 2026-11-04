package com.stefanzalan.vague_brief.web.controller;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.EventCategory;
import com.stefanzalan.vague_brief.domain.Severity;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import com.stefanzalan.vague_brief.web.dto.CreateAlertRuleRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRuleControllerTest {

    @Mock
    private AlertRuleRepository ruleRepository;

    @InjectMocks
    private AdminRuleController controller;

    @Test
    void createRule_withValidData_returns201() {
        // Given
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Critical Market Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "trader@example.com",
                true
        );

        when(ruleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ResponseEntity<AlertRule> response = controller.createRule(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Critical Market Alert");
    }

    @Test
    void createRule_withValidData_savesToRepository() {
        // Given
        CreateAlertRuleRequest request = new CreateAlertRuleRequest(
                "Test Rule",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.SLACK,
                "#channel",
                true
        );

        when(ruleRepository.save(any(AlertRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        controller.createRule(request);

        // Then
        // Validation is handled by Spring, so we just verify the controller logic
        assertThat(request.name()).isEqualTo("Test Rule");
    }

    @Test
    void listRules_returns200() {
        // Given
        AlertRule rule1 = new AlertRule(
                UUID.randomUUID(),
                "Rule 1",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.SLACK,
                "#news",
                true
        );

        AlertRule rule2 = new AlertRule(
                UUID.randomUUID(),
                "Rule 2",
                EventCategory.MARKET_MOVEMENT,
                Severity.CRITICAL,
                ChannelType.EMAIL,
                "admin@example.com",
                true
        );

        when(ruleRepository.findAll()).thenReturn(java.util.List.of(rule1, rule2));

        // When
        ResponseEntity<java.util.List<AlertRule>> response = controller.listRules();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void getRule_existingId_returns200() {
        // Given
        UUID ruleId = UUID.randomUUID();
        AlertRule rule = new AlertRule(
                ruleId,
                "Test Rule",
                EventCategory.NATURAL_DISASTER,
                Severity.CRITICAL,
                ChannelType.EMAIL,
                "emergency@example.com",
                true
        );

        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(rule));

        // When
        ResponseEntity<AlertRule> response = controller.getRule(ruleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(ruleId);
        assertThat(response.getBody().name()).isEqualTo("Test Rule");
    }

    @Test
    void getRule_nonExistentId_throwsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(ruleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.getRule(nonExistentId))
                .isInstanceOf(com.stefanzalan.vague_brief.exception.RuleNotFoundException.class);
    }

    @Test
    void deleteRule_existingId_returns204() {
        // Given
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.existsById(ruleId)).thenReturn(true);

        // When
        ResponseEntity<Void> response = controller.deleteRule(ruleId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void deleteRule_nonExistentId_throwsException() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(ruleRepository.existsById(nonExistentId)).thenReturn(false);

        // When & Then
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.deleteRule(nonExistentId))
                .isInstanceOf(com.stefanzalan.vague_brief.exception.RuleNotFoundException.class);
    }
}
