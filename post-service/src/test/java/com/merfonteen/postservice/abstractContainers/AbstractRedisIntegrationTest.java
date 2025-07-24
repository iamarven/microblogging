package com.merfonteen.postservice.abstractContainers;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractRedisIntegrationTest extends AbstractPostgresIntegrationTest {
    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2.4")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }
}
