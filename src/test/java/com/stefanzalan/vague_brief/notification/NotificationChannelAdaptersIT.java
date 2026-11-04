package com.stefanzalan.vague_brief.notification;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.notification.email.EmailNotificationChannel;
import com.stefanzalan.vague_brief.notification.slack.SlackNotificationChannel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationChannelAdaptersIT {

    @Autowired
    private NotificationChannelRegistry registry;

    @Test
    void registersEmailAndSlackAdapters() {
        assertThat(registry.supports(ChannelType.EMAIL)).isTrue();
        assertThat(registry.supports(ChannelType.SLACK)).isTrue();
        assertThat(registry.getChannel(ChannelType.EMAIL)).isInstanceOf(EmailNotificationChannel.class);
        assertThat(registry.getChannel(ChannelType.SLACK)).isInstanceOf(SlackNotificationChannel.class);
    }
}
