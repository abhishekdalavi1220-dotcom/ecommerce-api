package com.abhishek.ecommerce.config;

import com.abhishek.ecommerce.model.Cart;
import com.abhishek.ecommerce.model.Role;
import com.abhishek.ecommerce.model.User;
import com.abhishek.ecommerce.repository.CartRepository;
import com.abhishek.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * There's no public "become an admin" endpoint by design — only you should be able to
 * manage the catalog. Set ADMIN_EMAIL and ADMIN_PASSWORD as env vars and this creates
 * that account (once) on startup with ROLE_ADMIN. Leave them unset to skip seeding.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }
        if (userRepository.existsByEmail(adminEmail.toLowerCase())) {
            return;
        }

        User admin = User.builder()
                .name("Admin")
                .email(adminEmail.toLowerCase())
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();
        admin = userRepository.save(admin);
        cartRepository.save(Cart.builder().user(admin).build());

        log.info("Seeded admin account: {}", admin.getEmail());
    }
}
