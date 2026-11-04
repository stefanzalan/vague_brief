package com.stefanzalan.vague_brief.evaluation;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.EventCategory;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.domain.Severity;
import com.stefanzalan.vague_brief.domain.SystemEvent;
import com.stefanzalan.vague_brief.notification.NotificationChannelRegistry;
import com.stefanzalan.vague_brief.notification.NotificationChannelService;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventEvaluationEngineTest {

    @Mock
    private AlertRuleRepository ruleRepository;

    @Mock
    private NotificationChannelRegistry channelRegistry;

    @Mock
    private NotificationChannelService emailChannelService;

    @Mock
    private NotificationChannelService slackChannelService;

    private EventEvaluationEngine engine;

    private UUID ruleId1;
    private UUID ruleId2;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        engine = new EventEvaluationEngine(ruleRepository, channelRegistry, virtualThreadExecutor);

        ruleId1 = UUID.randomUUID();
        ruleId2 = UUID.randomUUID();
        eventId = UUID.randomUUID();

        // Setup channel registry mocks
        when(channelRegistry.getChannel(ChannelType.EMAIL)).thenReturn(emailChannelService);
        when(channelRegistry.getChannel(ChannelType.SLACK)).thenReturn(slackChannelService);
    }

    @Test
    void processEvent_withMatchingRule_dispatchesNotification() throws InterruptedException {
        // Given
        AlertRule matchingRule = new AlertRule(
                ruleId1,
                "Critical Market Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "trader@example.com",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Stock Price Drop",
                "Major market index dropped by 5%",
                EventCategory.MARKET_MOVEMENT,
                Severity.CRITICAL,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(matchingRule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(emailChannelService).sendNotification(payloadCaptor.capture());

        NotificationPayload capturedPayload = payloadCaptor.getValue();
        assertThat(capturedPayload.recipient()).isEqualTo("trader@example.com");
        assertThat(capturedPayload.subject()).contains("Stock Price Drop");
        assertThat(capturedPayload.messageBody()).contains("Critical Market Alert");
        assertThat(capturedPayload.severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void processEvent_withNonMatchingCategory_doesNotDispatch() throws InterruptedException {
        // Given
        AlertRule rule = new AlertRule(
                ruleId1,
                "Breaking News Alert",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.SLACK,
                "#news-channel",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Market Update",
                "Stock market closed higher",
                EventCategory.MARKET_MOVEMENT,
                Severity.INFO,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(rule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(slackChannelService, never()).sendNotification(any());
    }

    @Test
    void processEvent_withSeverityBelowThreshold_doesNotDispatch() throws InterruptedException {
        // Given
        AlertRule rule = new AlertRule(
                ruleId1,
                "Critical Disaster Alert",
                EventCategory.NATURAL_DISASTER,
                Severity.CRITICAL,
                ChannelType.EMAIL,
                "emergency@example.com",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Minor Tremor",
                "Small earthquake detected",
                EventCategory.NATURAL_DISASTER,
                Severity.INFO,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(rule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService, never()).sendNotification(any());
    }

    @Test
    void processEvent_withDisabledRule_doesNotDispatch() throws InterruptedException {
        // Given
        AlertRule disabledRule = new AlertRule(
                ruleId1,
                "Disabled Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.INFO,
                ChannelType.EMAIL,
                "disabled@example.com",
                false
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Market Event",
                "Market movement detected",
                EventCategory.MARKET_MOVEMENT,
                Severity.INFO,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of());

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService, never()).sendNotification(any());
    }

    @Test
    void processEvent_withMultipleMatchingRules_dispatchesToMultipleChannels() throws InterruptedException {
        // Given
        AlertRule emailRule = new AlertRule(
                ruleId1,
                "Email Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "trader@example.com",
                true
        );

        AlertRule slackRule = new AlertRule(
                ruleId2,
                "Slack Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.SLACK,
                "#trading-channel",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Market Crash",
                "Major market downturn",
                EventCategory.MARKET_MOVEMENT,
                Severity.CRITICAL,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(emailRule, slackRule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService).sendNotification(any(NotificationPayload.class));
        verify(slackChannelService).sendNotification(any(NotificationPayload.class));
    }

    @Test
    void processEvent_withNoMatchingRules_logsNoMatch() throws InterruptedException {
        // Given
        SystemEvent event = new SystemEvent(
                eventId,
                "Irrelevant Event",
                "Some event",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of());

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService, never()).sendNotification(any());
        verify(slackChannelService, never()).sendNotification(any());
    }

    @Test
    void processEvent_withExactSeverityThreshold_dispatchesNotification() throws InterruptedException {
        // Given
        AlertRule rule = new AlertRule(
                ruleId1,
                "Warning Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "analyst@example.com",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Market Warning",
                "Market showing warning signs",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(rule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService).sendNotification(any(NotificationPayload.class));
    }

    @Test
    void processEvent_withChannelDispatchFailure_doesNotBlockOtherChannels() throws InterruptedException {
        // Given
        AlertRule emailRule = new AlertRule(
                ruleId1,
                "Email Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.EMAIL,
                "trader@example.com",
                true
        );

        AlertRule slackRule = new AlertRule(
                ruleId2,
                "Slack Alert",
                EventCategory.MARKET_MOVEMENT,
                Severity.WARNING,
                ChannelType.SLACK,
                "#trading-channel",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Market Event",
                "Market movement",
                EventCategory.MARKET_MOVEMENT,
                Severity.CRITICAL,
                Instant.now()
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(emailRule, slackRule));

        // Make email channel fail
        doThrow(new RuntimeException("Email service unavailable")).when(emailChannelService).sendNotification(any());

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        verify(emailChannelService).sendNotification(any(NotificationPayload.class));
        verify(slackChannelService).sendNotification(any(NotificationPayload.class));
    }

    @Test
    void processEvent_buildsCorrectNotificationPayload() throws InterruptedException {
        // Given
        AlertRule rule = new AlertRule(
                ruleId1,
                "Test Rule",
                EventCategory.BREAKING_NEWS,
                Severity.INFO,
                ChannelType.EMAIL,
                "recipient@example.com",
                true
        );

        SystemEvent event = new SystemEvent(
                eventId,
                "Breaking Story",
                "Important news broke",
                EventCategory.BREAKING_NEWS,
                Severity.CRITICAL,
                Instant.parse("2024-01-15T10:30:00Z")
        );

        when(ruleRepository.findEnabled()).thenReturn(List.of(rule));

        // When
        engine.processEvent(event);

        // Wait for async dispatch to complete
        Thread.sleep(500);

        // Then
        ArgumentCaptor<NotificationPayload> payloadCaptor = ArgumentCaptor.forClass(NotificationPayload.class);
        verify(emailChannelService).sendNotification(payloadCaptor.capture());

        NotificationPayload payload = payloadCaptor.getValue();
        assertThat(payload.recipient()).isEqualTo("recipient@example.com");
        assertThat(payload.subject()).isEqualTo("[CRITICAL] Breaking Story");
        assertThat(payload.messageBody()).contains("Test Rule");
        assertThat(payload.messageBody()).contains(eventId.toString());
        assertThat(payload.messageBody()).contains("BREAKING_NEWS");
        assertThat(payload.messageBody()).contains("CRITICAL");
        assertThat(payload.messageBody()).contains("2024-01-15T10:30:00Z");
        assertThat(payload.messageBody()).contains("Important news broke");
        assertThat(payload.severity()).isEqualTo(Severity.CRITICAL);
    }
}
