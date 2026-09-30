package com.abhishek.ecommerce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev") // runs against in-memory H2 so this doesn't need Postgres or Stripe
class EcommerceApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the whole application context (security, JPA, beans) wires up correctly.
    }
}
