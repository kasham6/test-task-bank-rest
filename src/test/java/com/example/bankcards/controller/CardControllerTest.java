package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private static final String USERNAME = "user1";
    private static final String OTHER = "user2";
    private static final String ADMIN = "admin";

    @BeforeEach
    void setUp() {

        insertUserIfMissing(USERNAME, Role.USER);
        insertUserIfMissing(OTHER, Role.USER);
        insertUserIfMissing(ADMIN, Role.ADMIN);
    }

    private void insertUserIfMissing(String username, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setEnabled(true);

        userRepository.save(user);

    }

    private UsernamePasswordAuthenticationToken authFor(String username, String... roles) {
        User domain = userRepository.findByUsername(username).orElseThrow();
        List<SimpleGrantedAuthority> auths = domain.getRole() != null
                ? List.of(new SimpleGrantedAuthority("ROLE_" + domain.getRole().name()))
                : List.of();
        return new UsernamePasswordAuthenticationToken(domain, null, auths);
    }

    private CardDto createCardAsAdmin(UUID ownerId, String number) throws Exception {
        CreateCardRequest create = new CreateCardRequest(
                ownerId,
                number,
                "2028-12",
                new BigDecimal("1000.00"),
                "ACTIVE"
        );
        String resp = mvc.perform(post("/admin/cards")
                        .with(authentication(authFor(ADMIN)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(resp, CardDto.class);
    }

    @Test
    void transfer_between_own_cards_success_returns200() throws Exception {
        UUID ownerId = userRepository.findByUsername(USERNAME).get().getId();
        CardDto from = createCardAsAdmin(ownerId, "4222222222222222");
        CardDto to = createCardAsAdmin(ownerId, "4333333333333333");

        TransferRequest transferReq = new TransferRequest(
                from.id(),
                to.id(),
                new BigDecimal("50.00")
        );

        mvc.perform(post("/cards/transfer")
                        .with(authentication(authFor(USERNAME)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")));
    }

    @Test
    void requestBlock_own_card_returns200() throws Exception {
        UUID ownerId = userRepository.findByUsername(USERNAME).get().getId();
        CardDto card = createCardAsAdmin(ownerId, "4777777777777777");

        mvc.perform(post("/cards/{id}/request-block", card.id())
                        .with(authentication(authFor(USERNAME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("blocked")));
    }

    @Test
    void requestBlock_other_card_returns403() throws Exception {
        UUID otherId = userRepository.findByUsername(OTHER).get().getId();
        CardDto card = createCardAsAdmin(otherId, "4888888888888888");

        mvc.perform(post("/cards/{id}/request-block", card.id())
                        .with(authentication(authFor(USERNAME))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", not(emptyString())));
    }

    @Test
    void listOwn_includes_created_card() throws Exception {
        UUID ownerId = userRepository.findByUsername(USERNAME).get().getId();
        createCardAsAdmin(ownerId, "4111111111111111");

        mvc.perform(get("/cards")
                        .with(authentication(authFor(USERNAME)))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[0].maskedNumber", containsString("1111")));
    }
}
