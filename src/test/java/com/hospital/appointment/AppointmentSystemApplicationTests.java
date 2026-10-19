package com.hospital.appointment;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Basic JUnit 5 Spring Boot Context Test
 * Verifies that the Spring ApplicationContext loads without any configuration errors.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class AppointmentSystemApplicationTests {

    @Test
    void contextLoads() {
        // Test passes if the Spring application context initializes successfully
    }
}
