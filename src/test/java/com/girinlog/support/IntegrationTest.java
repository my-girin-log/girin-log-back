package com.girinlog.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 통합 테스트 베이스. Testcontainers PostgreSQL을 띄우고 실제 컨텍스트를 로드한다.
 * (conventions/testing.md: 통합은 Testcontainers PostgreSQL 우선)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // 마이그레이션 도입 전까지 테스트 스키마는 엔티티에서 생성한다.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("girinlog.jwt.secret", () -> "girinlog-integration-test-secret-0123456789");
        registry.add("girinlog.jwt.access-token-ttl", () -> "P14D");
    }
}
