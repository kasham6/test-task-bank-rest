package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegistryRequest;
import com.example.bankcards.dto.auth.TokenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String username = "user1@example.com";
    private final String password = "Password1!";

    private AuthResponse initialAuth;

    @BeforeEach
    void signUpUser() throws Exception {
        RegistryRequest registryRequest = new RegistryRequest(username, password);
        var signup = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registryRequest)))
                .andExpect(status().isOk())
                .andReturn();

        initialAuth = objectMapper.readValue(
                signup.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(initialAuth.getAccessToken());
        assertNotNull(initialAuth.getRefreshToken());
    }

    @Test
    void signUp_shouldReturnTokens() throws Exception {
        RegistryRequest req = new RegistryRequest("newuser", "newpass1");
        var result = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
    }

    @Test
    void logIn_shouldReturnTokens() throws Exception {
        LoginRequest req = new LoginRequest(username, password);
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
    }

    @Test
    void refresh_withValidToken_shouldReturnNewTokens() throws Exception {
        TokenRequest req = new TokenRequest(initialAuth.getRefreshToken());
        var result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AuthResponse.class
        );
        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertNotEquals(initialAuth.getAccessToken(), resp.getAccessToken());
    }

    @Test
    void refresh_withEmptyToken_shouldReturn400() throws Exception {
        TokenRequest req = new TokenRequest("");
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withValidToken_shouldReturnNoContent() throws Exception {
        TokenRequest req = new TokenRequest(initialAuth.getRefreshToken());
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());
    }

    @Test
    void logout_withEmptyToken_shouldReturn400() throws Exception {
        TokenRequest req = new TokenRequest("");
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
