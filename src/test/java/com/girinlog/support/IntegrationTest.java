package com.girinlog.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 통합 테스트 베이스. Testcontainers PostgreSQL을 **싱글톤**으로 띄워 모든 통합 테스트 클래스가
 * 공유한다. (conventions/testing.md: 통합은 Testcontainers PostgreSQL 우선)
 *
 * <p>{@code @Testcontainers}(클래스별 start/stop) 대신 static 블록에서 한 번 start 한다.
 * 클래스마다 stop 되어 다음 클래스에서 DB가 사라지는 문제를 피한다. 컨테이너는 JVM 종료 시 정리된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("girinlog.jwt.secret", () -> "girinlog-integration-test-secret-0123456789");
        registry.add("girinlog.jwt.access-token-ttl", () -> "P14D");
    }
}
