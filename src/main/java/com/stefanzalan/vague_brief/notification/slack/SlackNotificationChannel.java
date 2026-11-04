package com.stefanzalan.vague_brief.notification.slack;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.notification.NotificationChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class SlackNotificationChannel implements NotificationChannelService {

    private static final Logger log = LoggerFactory.getLogger(SlackNotificationChannel.class);

    private final RestClient restClient;
    private final SlackNotificationProperties properties;

    public SlackNotificationChannel(RestClient slackRestClient, SlackNotificationProperties properties) {
        this.restClient = slackRestClient;
        this.properties = properties;
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.SLACK;
    }

    @Override
    public void sendNotification(NotificationPayload payload) {
        if (!properties.enabled()) {
            log.warn("Slack channel is disabled; skipping notification to {}", payload.recipient());
            return;
        }
        if (!properties.hasUsableWebhook()) {
            log.warn("Slack webhook URL is missing or a placeholder; skipping notification to {}",
                    payload.recipient());
            return;
        }

        SlackWebhookPayload body = new SlackWebhookPayload(formatText(payload));
        try {
            restClient.post()
                    .uri(properties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Sent Slack notification to {} with subject '{}'", payload.recipient(), payload.subject());
        } catch (RestClientException ex) {
            log.error("Failed to send Slack notification to {} (subject: {}, webhook: {})",
                    payload.recipient(), payload.subject(), properties.webhookUrl(), ex);
        }
    }

    private static String formatText(NotificationPayload payload) {
        return "*%s* %s%n%n%s".formatted(payload.severity(), payload.subject(), payload.messageBody());
    }
}
