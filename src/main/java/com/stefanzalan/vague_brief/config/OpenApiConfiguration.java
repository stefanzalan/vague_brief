package com.stefanzalan.vague_brief.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 configuration for Swagger UI documentation.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI alertNotifierOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AlertNotifier API")
                        .description("Multi-channel alert notification system with rule-based event evaluation")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("AlertNotifier Team")
                                .email("support@alertnotifier.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
