package com.stefanzalan.vague_brief.notification.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.email")
public record EmailNotificationProperties(
        boolean enabled,
        String from
) {
}
