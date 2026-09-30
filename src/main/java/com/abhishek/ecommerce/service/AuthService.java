package com.abhishek.ecommerce.service;

import com.abhishek.ecommerce.dto.AuthResponse;
import com.abhishek.ecommerce.dto.LoginRequest;
import com.abhishek.ecommerce.dto.RegisterRequest;
import com.abhishek.ecommerce.exception.BadRequestException;
import com.abhishek.ecommerce.model.Cart;
import com.abhishek.ecommerce.model.Role;
import com.abhishek.ecommerce.model.User;
import com.abhishek.ecommerce.repository.CartRepository;
import com.abhishek.ecommerce.repository.UserRepository;
import com.abhishek.ecommerce.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        user = userRepository.save(user);

        // Every user gets an empty cart created up front so cart lookups never need a null check.
        Cart cart = Cart.builder().user(user).build();
        cartRepository.save(cart);

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getName(), user.getRole().name());
    }
}
