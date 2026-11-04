package com.stefanzalan.vague_brief.evaluation;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.domain.SystemEvent;
import com.stefanzalan.vague_brief.notification.NotificationChannelRegistry;
import com.stefanzalan.vague_brief.notification.NotificationChannelService;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Core engine that evaluates incoming system events against configured alert rules
 * and asynchronously dispatches notifications to matching channels.
 */
@Service
public class EventEvaluationEngine {

    private static final Logger log = LoggerFactory.getLogger(EventEvaluationEngine.class);

    private final AlertRuleRepository ruleRepository;
    private final NotificationChannelRegistry channelRegistry;
    private final Executor virtualThreadExecutor;

    public EventEvaluationEngine(
            AlertRuleRepository ruleRepository,
            NotificationChannelRegistry channelRegistry,
            Executor virtualThreadExecutor) {
        this.ruleRepository = ruleRepository;
        this.channelRegistry = channelRegistry;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Process an incoming system event and dispatch notifications to all matching rules.
     * Dispatching is asynchronous and non-blocking using virtual threads.
     *
     * @param event the system event to evaluate
     */
    public void processEvent(SystemEvent event) {
        log.info("Processing event: {} (category: {}, severity: {})",
                event.id(), event.category(), event.severity());

        List<AlertRule> matchingRules = findMatchingRules(event);

        if (matchingRules.isEmpty()) {
            log.info("No matching rules found for event {}", event.id());
            return;
        }

        log.info("Found {} matching rule(s) for event {}", matchingRules.size(), event.id());

        // Dispatch notifications asynchronously using virtual threads
        for (AlertRule rule : matchingRules) {
            virtualThreadExecutor.execute(() -> dispatchNotification(event, rule));
        }
    }

    /**
     * Find all enabled rules that match the given event.
     * A rule matches if:
     * - The rule's category matches the event's category
     * - The event's severity meets or exceeds the rule's minimum severity
     *
     * @param event the system event to match against
     * @return list of matching enabled rules
     */
    private List<AlertRule> findMatchingRules(SystemEvent event) {
        return ruleRepository.findEnabled().stream()
                .filter(rule -> ruleMatches(rule, event))
                .toList();
    }

    /**
     * Determine if a rule matches an event based on category and severity.
     *
     * @param rule the alert rule
     * @param event the system event
     * @return true if the rule matches the event
     */
    private boolean ruleMatches(AlertRule rule, SystemEvent event) {
        boolean categoryMatches = rule.category().equals(event.category());
        boolean severityMeetsThreshold = event.severity().meetsMinimum(rule.minimumSeverity());

        return categoryMatches && severityMeetsThreshold;
    }

    /**
     * Dispatch a notification for a matching rule and event.
     * This method runs in a virtual thread to avoid blocking the main evaluation thread.
     *
     * @param event the system event
     * @param rule the matching alert rule
     */
    private void dispatchNotification(SystemEvent event, AlertRule rule) {
        try {
            NotificationPayload payload = buildPayload(event, rule);
            NotificationChannelService channelService = channelRegistry.getChannel(rule.targetChannel());

            log.info("Dispatching notification via {} for rule {} to destination {}",
                    rule.targetChannel(), rule.id(), rule.destination());

            channelService.sendNotification(payload);

            log.info("Successfully dispatched notification via {} for rule {}",
                    rule.targetChannel(), rule.id());
        } catch (Exception ex) {
            log.error("Failed to dispatch notification for rule {} (channel: {}, destination: {})",
                    rule.id(), rule.targetChannel(), rule.destination(), ex);
        }
    }

    /**
     * Build a NotificationPayload from an event and rule.
     *
     * @param event the system event
     * @param rule the alert rule
     * @return the notification payload
     */
    private NotificationPayload buildPayload(SystemEvent event, AlertRule rule) {
        String subject = "[%s] %s".formatted(event.severity(), event.title());
        String messageBody = buildMessageBody(event, rule);

        return new NotificationPayload(
                rule.destination(),
                subject,
                messageBody,
                event.severity()
        );
    }

    /**
     * Build the message body for the notification.
     *
     * @param event the system event
     * @param rule the alert rule
     * @return the formatted message body
     */
    private String buildMessageBody(SystemEvent event, AlertRule rule) {
        return """
                Alert Rule: %s
                Event ID: %s
                Category: %s
                Severity: %s
                Timestamp: %s

                Description:
                %s
                """.formatted(
                rule.name(),
                event.id(),
                event.category(),
                event.severity(),
                event.timestamp(),
                event.description()
        ).trim();
    }
}
