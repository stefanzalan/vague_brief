package com.stefanzalan.vague_brief.notification.email;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationChannelTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationChannel channel;

    @BeforeEach
    void setUp() {
        channel = new EmailNotificationChannel(
                mailSender,
                new EmailNotificationProperties(true, "alerts@alertnotifier.local")
        );
    }

    @Test
    void supportsEmailChannel() {
        assertThat(channel.getSupportedChannel()).isEqualTo(ChannelType.EMAIL);
    }

    @Test
    void sendsSimpleMailMappedFromPayload() {
        NotificationPayload payload = samplePayload();

        channel.sendNotification(payload);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("alerts@alertnotifier.local");
        assertThat(message.getTo()).containsExactly("ops@example.com");
        assertThat(message.getSubject()).isEqualTo("[CRITICAL] Market crash");
        assertThat(message.getText()).isEqualTo("Index dropped 8%");
    }

    @Test
    void swallowsMailDispatchFailures() {
        doThrow(new MailSendException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> channel.sendNotification(samplePayload())).doesNotThrowAnyException();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenDisabled() {
        channel = new EmailNotificationChannel(
                mailSender,
                new EmailNotificationProperties(false, "alerts@alertnotifier.local")
        );

        channel.sendNotification(samplePayload());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void skipsWhenFromAddressMissing() {
        channel = new EmailNotificationChannel(
                mailSender,
                new EmailNotificationProperties(true, "  ")
        );

        channel.sendNotification(samplePayload());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    private static NotificationPayload samplePayload() {
        return new NotificationPayload("ops@example.com", "Market crash", "Index dropped 8%", Severity.CRITICAL);
    }
}
