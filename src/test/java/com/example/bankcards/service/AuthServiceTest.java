package com.example.bankcards.service;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegistryRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.repository.UserRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;
    private final String rawPassword = "secret";
    private final String encodedPassword = "$2a$10$encodedhash";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(UUID.randomUUID())
                .username("alice")
                .passwordHash(encodedPassword)
                .role(Role.USER)
                .enabled(true)
                .build();
    }

    @Test
    void signUp_usernameTaken_throws() {
        RegistryRequest req = new RegistryRequest("alice", rawPassword);
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.signUp(req));
        assertEquals("Username already taken", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_success_returnsTokens() throws Exception {
        RegistryRequest req = new RegistryRequest("bob", rawPassword);
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        AuthResponse resp = authService.signUp(req);

        assertNotNull(resp);
        assertEquals("access-token", resp.getAccessToken());
        assertEquals("refresh-token", resp.getRefreshToken());
        assertEquals("Bearer", resp.getTokenType());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("bob", saved.getUsername());
        assertEquals(encodedPassword, saved.getPasswordHash());
        assertEquals(Role.USER, saved.getRole());
        assertTrue(saved.isEnabled());
    }

    @Test
    void logIn_invalidUsername_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("unknown");
        req.setPassword(rawPassword);
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.logIn(req));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void logIn_invalidPassword_throws() {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", encodedPassword)).thenReturn(false);

        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.logIn(req));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void logIn_disabledUser_throws() {
        user.setEnabled(false);
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword(rawPassword);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> authService.logIn(req));
        assertEquals("User is disabled", ex.getMessage());
    }

    @Test
    void logIn_success_returnsTokens() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword(rawPassword);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(rawPassword, encodedPassword)).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("access");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh");

        AuthResponse resp = authService.logIn(req);
        assertEquals("access", resp.getAccessToken());
        assertEquals("refresh", resp.getRefreshToken());
        assertEquals("Bearer", resp.getTokenType());
    }

    @Test
    void refreshAccessToken_invalidSubject_throws() throws Exception {
        String badToken = "bad";
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("not-a-uuid")
                .build();
        when(jwtService.parseRefreshToken(badToken)).thenReturn(claims);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(badToken));
        assertEquals("Invalid token subject", ex.getMessage());
    }

    @Test
    void refreshAccessToken_userNotFound_throws() throws Exception {
        UUID missingId = UUID.randomUUID();
        String token = "tok";
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(missingId.toString())
                .build();
        when(jwtService.parseRefreshToken(token)).thenReturn(claims);
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.refreshAccessToken(token));
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void refreshAccessToken_success_returnsNewTokens() throws Exception {
        String token = "tok";
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .build();
        when(jwtService.parseRefreshToken(token)).thenReturn(claims);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh");

        AuthResponse resp = authService.refreshAccessToken(token);
        assertEquals("new-access", resp.getAccessToken());
        assertEquals("new-refresh", resp.getRefreshToken());
        assertEquals("Bearer", resp.getTokenType());
    }

    @Test
    void logout_withBlank_throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.logout(" "));
        assertEquals("Refresh token must be provided", ex.getMessage());
    }

    @Test
    void logout_valid_doesNotThrow() {
        assertDoesNotThrow(() -> authService.logout("some-token"));
    }
}
