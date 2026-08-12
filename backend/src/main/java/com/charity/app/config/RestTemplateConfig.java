package com.charity.app.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * A shared, bounded HTTP client for the messaging channels.
 *
 * <p>Each service previously built its own `new RestTemplate()`, which has no connect or read
 * timeout at all -- an unresponsive bot API would hang the calling thread forever.
 */
@Configuration
public class RestTemplateConfig {

    @Bean("messagingRestTemplate")
    public RestTemplate messagingRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
}
