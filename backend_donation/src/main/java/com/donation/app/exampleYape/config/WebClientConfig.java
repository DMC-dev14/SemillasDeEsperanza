package com.donation.app.exampleYape.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
    @Bean
    public WebClient mercadoPagoWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://api.mercadopago.com")
                .build();
    }
}
