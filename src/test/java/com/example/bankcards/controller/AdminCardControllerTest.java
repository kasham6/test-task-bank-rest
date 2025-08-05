package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.CardStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.UUID;

import static javax.management.Query.eq;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AdminCardControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;

    private UUID ownerId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCard_returns201() throws Exception {
        CreateCardRequest req = new CreateCardRequest(
                ownerId,
                "4111111111111111",
                "2028-12",
                new BigDecimal("1000.00"),
                "ACTIVE"
        );

        mvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maskedNumber").value("**** **** **** 1111"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeStatus_returns200() throws Exception {
        CreateCardRequest createReq = new CreateCardRequest(
                ownerId,
                "4222222222222222",
                "2028-12",
                new BigDecimal("0.00"),
                "ACTIVE"
        );

        String createResponse = mvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CardDto created = objectMapper.readValue(createResponse, CardDto.class);
        UUID existingCardId = created.id();

        mvc.perform(patch("/admin/cards/{id}/status", existingCardId)
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("BLOCKED")))
                .andExpect(jsonPath("$.id", is(existingCardId.toString())));
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteCard_noConflict_returns204() throws Exception {
        CreateCardRequest createReq = new CreateCardRequest(
                ownerId,
                "4333333333333333",
                "2028-12",
                new BigDecimal("10.00"),
                "ACTIVE"
        );

        String createResponse = mvc.perform(post("/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        CardDto created = objectMapper.readValue(createResponse, CardDto.class);
        UUID existingCardId = created.id();

        mvc.perform(delete("/admin/cards/{id}", existingCardId))
                .andExpect(status().isNoContent());
    }
}
