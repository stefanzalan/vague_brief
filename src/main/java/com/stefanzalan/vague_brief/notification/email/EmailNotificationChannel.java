package com.stefanzalan.vague_brief.notification.email;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.notification.NotificationChannelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationChannel implements NotificationChannelService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final JavaMailSender mailSender;
    private final EmailNotificationProperties properties;

    public EmailNotificationChannel(JavaMailSender mailSender, EmailNotificationProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public ChannelType getSupportedChannel() {
        return ChannelType.EMAIL;
    }

    @Override
    public void sendNotification(NotificationPayload payload) {
        if (!properties.enabled()) {
            log.warn("Email channel is disabled; skipping notification to {}", payload.recipient());
            return;
        }
        if (properties.from() == null || properties.from().isBlank()) {
            log.warn("Email from-address is not configured; skipping notification to {}", payload.recipient());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(payload.recipient());
            message.setSubject("[%s] %s".formatted(payload.severity(), payload.subject()));
            message.setText(payload.messageBody());
            mailSender.send(message);
            log.info("Sent email notification to {} with subject '{}'", payload.recipient(), payload.subject());
        } catch (MailException ex) {
            log.error("Failed to send email notification to {} (subject: {})",
                    payload.recipient(), payload.subject(), ex);
        }
    }
}
