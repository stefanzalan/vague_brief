package com.stefanzalan.vague_brief.notification;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.domain.Severity;
import com.stefanzalan.vague_brief.exception.UnsupportedChannelException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationChannelRegistryTest {

    private CapturingChannel emailChannel;
    private CapturingChannel slackChannel;
    private NotificationChannelRegistry registry;

    @BeforeEach
    void setUp() {
        emailChannel = new CapturingChannel(ChannelType.EMAIL);
        slackChannel = new CapturingChannel(ChannelType.SLACK);
        registry = new NotificationChannelRegistry(List.of(emailChannel, slackChannel));
    }

    @Test
    void routesToEmailChannel() {
        NotificationPayload payload = samplePayload("ops@example.com");

        registry.getChannel(ChannelType.EMAIL).sendNotification(payload);

        assertThat(emailChannel.lastPayload()).isSameAs(payload);
        assertThat(slackChannel.lastPayload()).isNull();
    }

    @Test
    void routesToSlackChannel() {
        NotificationPayload payload = samplePayload("https://hooks.slack.com/services/test");

        registry.getChannel(ChannelType.SLACK).sendNotification(payload);

        assertThat(slackChannel.lastPayload()).isSameAs(payload);
        assertThat(emailChannel.lastPayload()).isNull();
    }

    @Test
    void throwsWhenChannelIsNotRegistered() {
        NotificationChannelRegistry emailOnly = new NotificationChannelRegistry(List.of(emailChannel));

        assertThatThrownBy(() -> emailOnly.getChannel(ChannelType.SLACK))
                .isInstanceOf(UnsupportedChannelException.class)
                .hasMessageContaining("SLACK")
                .extracting(ex -> ((UnsupportedChannelException) ex).getChannelType())
                .isEqualTo(ChannelType.SLACK);
    }

    @Test
    void throwsWhenRegistryIsEmpty() {
        NotificationChannelRegistry empty = new NotificationChannelRegistry(List.of());

        assertThatThrownBy(() -> empty.getChannel(ChannelType.EMAIL))
                .isInstanceOf(UnsupportedChannelException.class);
    }

    @Test
    void rejectsDuplicateChannelRegistrations() {
        assertThatThrownBy(() -> new NotificationChannelRegistry(List.of(emailChannel, new CapturingChannel(ChannelType.EMAIL))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("EMAIL");
    }

    @Test
    void reportsSupportedChannels() {
        assertThat(registry.supports(ChannelType.EMAIL)).isTrue();
        assertThat(registry.supports(ChannelType.SLACK)).isTrue();
    }

    @Test
    void reportsUnsupportedWhenMissing() {
        NotificationChannelRegistry emailOnly = new NotificationChannelRegistry(List.of(emailChannel));

        assertThat(emailOnly.supports(ChannelType.SLACK)).isFalse();
    }

    private static NotificationPayload samplePayload(String recipient) {
        return new NotificationPayload(recipient, "Alert", "Something happened", Severity.WARNING);
    }

    private static final class CapturingChannel implements NotificationChannelService {

        private final ChannelType channelType;
        private NotificationPayload lastPayload;

        private CapturingChannel(ChannelType channelType) {
            this.channelType = channelType;
        }

        @Override
        public ChannelType getSupportedChannel() {
            return channelType;
        }

        @Override
        public void sendNotification(NotificationPayload payload) {
            this.lastPayload = payload;
        }

        private NotificationPayload lastPayload() {
            return lastPayload;
        }
    }
}
