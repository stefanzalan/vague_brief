package com.stefanzalan.vague_brief.notification.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.slack")
public record SlackNotificationProperties(
        boolean enabled,
        String webhookUrl
) {

    public boolean hasUsableWebhook() {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return false;
        }
        String normalized = webhookUrl.strip();
        return !normalized.equalsIgnoreCase("changeme")
                && !normalized.toLowerCase().contains("placeholder");
    }
}
