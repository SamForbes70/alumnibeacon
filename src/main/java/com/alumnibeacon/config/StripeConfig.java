package com.alumnibeacon.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${stripe.price-id.starter:price_starter_placeholder}")
    private String starterPriceId;

    @Value("${stripe.price-id.professional:price_professional_placeholder}")
    private String professionalPriceId;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** True only when a real Stripe key is configured. */
    public boolean isEnabled() {
        return secretKey != null && secretKey.startsWith("sk_");
    }

    @PostConstruct
    public void init() {
        if (isEnabled()) {
            Stripe.apiKey = secretKey;
        }
    }
}
