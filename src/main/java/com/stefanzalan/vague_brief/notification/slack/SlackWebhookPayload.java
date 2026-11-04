package com.stefanzalan.vague_brief.notification.slack;

/**
 * Incoming Webhook body accepted by Slack.
 */
public record SlackWebhookPayload(String text) {
}
