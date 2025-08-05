package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    private User user;
    private Card card;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .passwordHash("hash")
                .role(null)
                .enabled(true)
                .build();

        card = Card.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .number("4111111111111111")
                .expiry(LocalDate.of(2028, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();
    }

    @Test
    void listOwn_whenUserIsNull_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> cardService.listOwn(null, 0, 10, null, null));
    }

    @Test
    void listOwn_noSearch_returnsPageWithOneCardDto() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Card> mockPage = new PageImpl<>(List.of(card), pageable, 1);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(mockPage);

        Page<CardDto> result = cardService.listOwn(user, 0, 10, "ACTIVE", null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        CardDto dto = result.getContent().get(0);
        assertEquals(card.getId(), dto.id());
        assertEquals("**** **** **** 1111", dto.maskedNumber());
        assertEquals("ACTIVE", dto.status());
        assertEquals(card.getBalance(), dto.balance());
        assertEquals(card.getExpiry(), dto.expiry());
    }

    @Test
    void listOwn_withSearch_filtersByMaskedNumber() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Card matching = Card.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .number("1234567812341111")
                .expiry(LocalDate.of(2028, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100"))
                .build();
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(matching), pageable, 1));

        Page<CardDto> result = cardService.listOwn(user, 0, 10, "ACTIVE", "1111");

        assertEquals(1, result.getTotalElements());
        CardDto dto = result.getContent().get(0);
        assertTrue(dto.maskedNumber().endsWith("1111"));
    }

    @Test
    void listOwn_withSearch_filtersByOwnerUsername() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        User otherOwner = User.builder()
                .id(UUID.randomUUID())
                .username("searchableUser")
                .passwordHash("h")
                .role(null)
                .enabled(true)
                .build();
        Card matching = Card.builder()
                .id(UUID.randomUUID())
                .owner(otherOwner)
                .number("9999888877776666")
                .expiry(LocalDate.of(2028, 12, 31))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100"))
                .build();
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(matching), pageable, 1));

        Page<CardDto> result = cardService.listOwn(otherOwner, 0, 10, null, "searchable");

        assertEquals(1, result.getTotalElements());
        assertEquals(matching.getId(), result.getContent().get(0).id());
    }

    @Test
    void requestBlock_whenUserIsNull_throwsUnauthorized() {
        assertThrows(UnauthorizedException.class,
                () -> cardService.requestBlock(null, UUID.randomUUID()));
    }

    @Test
    void requestBlock_cardNotFound_throwsResourceNotFound() {
        UUID cardId = UUID.randomUUID();
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cardService.requestBlock(user, cardId));
    }

    @Test
    void requestBlock_notOwner_throwsForbidden() {
        UUID cardId = UUID.randomUUID();
        User other = User.builder().id(UUID.randomUUID()).username("other").passwordHash("h").role(null).enabled(true).build();
        Card otherCard = Card.builder()
                .id(cardId)
                .owner(other)
                .number("4111")
                .expiry(LocalDate.now())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        when(cardRepository.findById(cardId)).thenReturn(Optional.of(otherCard));

        assertThrows(ForbiddenException.class, () -> cardService.requestBlock(user, cardId));
    }

    @Test
    void requestBlock_success_updatesStatusAndSaves() {
        UUID cardId = card.getId();
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenReturn(card);

        assertDoesNotThrow(() -> cardService.requestBlock(user, cardId));
        assertEquals(CardStatus.BLOCKED, card.getStatus());
        verify(cardRepository).save(card);
    }
}
