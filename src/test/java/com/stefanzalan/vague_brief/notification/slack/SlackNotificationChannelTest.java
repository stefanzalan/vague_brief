package com.stefanzalan.vague_brief.notification.slack;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;
import com.stefanzalan.vague_brief.domain.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SlackNotificationChannelTest {

    private static final String WEBHOOK_URL = "https://hooks.slack.com/services/T000/B000/XXX";

    private MockRestServiceServer mockServer;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .configureMessageConverters(converters -> converters.registerDefaults()
                        .withJsonConverter(new JacksonJsonHttpMessageConverter()));
        mockServer = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
    }

    @Test
    void supportsSlackChannel() {
        SlackNotificationChannel channel = enabledChannel();

        assertThat(channel.getSupportedChannel()).isEqualTo(ChannelType.SLACK);
    }

    @Test
    void postsJsonPayloadToConfiguredWebhook() {
        SlackNotificationChannel channel = enabledChannel();
        mockServer.expect(once(), requestTo(WEBHOOK_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("CRITICAL")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Market crash")))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Index dropped 8%")))
                .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

        channel.sendNotification(samplePayload());

        mockServer.verify();
    }

    @Test
    void swallowsWebhookFailures() {
        SlackNotificationChannel channel = enabledChannel();
        mockServer.expect(once(), requestTo(WEBHOOK_URL))
                .andRespond(withServerError());

        assertThatCode(() -> channel.sendNotification(samplePayload())).doesNotThrowAnyException();
        mockServer.verify();
    }

    @Test
    void skipsWhenDisabled() {
        SlackNotificationChannel channel = new SlackNotificationChannel(
                restClient,
                new SlackNotificationProperties(false, WEBHOOK_URL)
        );
        mockServer.expect(never(), requestTo(WEBHOOK_URL))
                .andRespond(withSuccess());

        channel.sendNotification(samplePayload());

        mockServer.verify();
    }

    @Test
    void skipsWhenWebhookIsPlaceholder() {
        SlackNotificationChannel channel = new SlackNotificationChannel(
                restClient,
                new SlackNotificationProperties(true, "https://hooks.slack.com/services/placeholder")
        );
        mockServer.expect(never(), requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withSuccess());

        channel.sendNotification(samplePayload());

        mockServer.verify();
    }

    @Test
    void skipsWhenWebhookIsBlank() {
        SlackNotificationChannel channel = new SlackNotificationChannel(
                restClient,
                new SlackNotificationProperties(true, "")
        );
        mockServer.expect(never(), requestTo(org.hamcrest.Matchers.any(String.class)))
                .andRespond(withSuccess());

        channel.sendNotification(samplePayload());

        mockServer.verify();
    }

    private SlackNotificationChannel enabledChannel() {
        return new SlackNotificationChannel(restClient, new SlackNotificationProperties(true, WEBHOOK_URL));
    }

    private static NotificationPayload samplePayload() {
        return new NotificationPayload(
                "https://hooks.slack.com/services/T000/B000/XXX",
                "Market crash",
                "Index dropped 8%",
                Severity.CRITICAL
        );
    }
}
