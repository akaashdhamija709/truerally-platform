package com.truerally.common.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfiguration {

    @Autowired
    private MeterRegistry registry;

    public static Counter userRegistrationCounter;
    public static Counter failedLoginCounter;

    @PostConstruct
    public void initMetrics() {
        userRegistrationCounter = Counter.builder("auth_user_registrations_total")
                .description("Total number of successful user registrations")
                .tag("service", "auth-service")
                .register(registry);

        failedLoginCounter = Counter.builder("auth_failed_logins_total")
                .description("Number of failed login attempts")
                .tag("service", "auth-service")
                .register(registry);
    }
}
