package io.github.k1mvasyok.billing;

import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;

/**
 * Локальный запуск приложения с PostgreSQL из Testcontainers: {@code ./gradlew :billing-service:bootTestRun}.
 * Docker Compose здесь не нужен, поэтому отключаем его поддержку.
 */
public class TestBillingApplication {

    public static void main(String[] args) {
        String[] withoutCompose = Stream.concat(
                Stream.of("--spring.docker.compose.enabled=false"),
                Stream.of(args)).toArray(String[]::new);
        SpringApplication.from(BillingApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(withoutCompose);
    }
}
