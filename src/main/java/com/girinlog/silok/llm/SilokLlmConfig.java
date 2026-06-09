package com.girinlog.silok.llm;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class SilokLlmConfig {

    @Bean
    LlmClient llmClient(GeminiProperties properties, Clock clock) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory(properties))
                .build();
        return new LoggingLlmClient(new GeminiLlmClient(properties, restClient), clock);
    }

    private SimpleClientHttpRequestFactory requestFactory(GeminiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout());
        requestFactory.setReadTimeout(properties.getTimeout());
        return requestFactory;
    }
}
