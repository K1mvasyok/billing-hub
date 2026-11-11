package io.github.k1mvasyok.billing.shared.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Часы сервиса. В тестах их подменяют на фиксированные, чтобы даты не зависели от текущего дня. */
@Configuration
public class ClockConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
