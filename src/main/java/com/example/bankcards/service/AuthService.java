package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegistryRequest;
import com.example.bankcards.exception.ForbiddenException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse signUp(RegistryRequest request) {
        if (userRepository.existsByUsername((request.getUsername()))) {
            throw new IllegalArgumentException("Username already taken");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User newUser = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordHash)
                .role(Role.USER)
                .enabled(true)
                .build();
        userRepository.save(newUser);
        log.info("New user with username {} created", request.getUsername());

        return generateTokens(newUser);
    }

    public AuthResponse logIn(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ForbiddenException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ForbiddenException("Invalid credentials");
        }
        if (!user.isEnabled()) {
            throw new IllegalStateException("User is disabled");
        }

        log.info("User with username {} logged in", user.getUsername());
        return generateTokens(user);
    }

    public AuthResponse refreshAccessToken(String refreshToken) throws Exception {
        JWTClaimsSet claims = jwtService.parseRefreshToken(refreshToken);
        String subject = claims.getSubject();

        UUID userId;
        try {
            userId = UUID.fromString(subject);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token subject");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return generateTokens(user);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must be provided");
        }
        log.info("Logout called; client should discard refresh token");
    }

    private AuthResponse generateTokens(User user) {
        try {
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);
            return new AuthResponse(accessToken, refreshToken);
        } catch (Exception e) {
            throw new RuntimeException("Token generation failed", e);
        }
    }
}
