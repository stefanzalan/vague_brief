package com.stefanzalan.vague_brief.notification;

import com.stefanzalan.vague_brief.notification.email.EmailNotificationProperties;
import com.stefanzalan.vague_brief.notification.slack.SlackNotificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({EmailNotificationProperties.class, SlackNotificationProperties.class})
public class NotificationChannelConfiguration {

    @Bean
    RestClient slackRestClient(RestClient.Builder restClientBuilder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return restClientBuilder.requestFactory(requestFactory).build();
    }
}
