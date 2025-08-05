package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminCardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminCardService adminCardService;

    private User owner;
    private UUID ownerId;
    private UUID cardId;
    private Card existingCard;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ownerId = UUID.randomUUID();
        owner = User.builder()
                .id(ownerId)
                .username("owner")
                .passwordHash("h")
                .role(null)
                .enabled(true)
                .build();

        cardId = UUID.randomUUID();
        existingCard = Card.builder()
                .id(cardId)
                .owner(owner)
                .number("4111111111111111")
                .expiry(YearMonth.of(2028, 12).atEndOfMonth())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void createCard_userNotFound_throws() {
        CreateCardRequest req = new CreateCardRequest(ownerId, "4111111111111111", "2028-12", new BigDecimal("500"), "ACTIVE");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminCardService.createCard(req));
    }

    @Test
    void createCard_success_returnsDto() {
        CreateCardRequest req = new CreateCardRequest(ownerId, "4111111111111111", "2028-12", new BigDecimal("500.00"), "ACTIVE");
        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        Card saved = Card.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .number(req.getNumber())
                .expiry(YearMonth.parse(req.getExpiry()).atEndOfMonth())
                .status(CardStatus.ACTIVE)
                .balance(req.getBalance())
                .build();
        when(cardRepository.save(any(Card.class))).thenReturn(saved);

        CardDto dto = adminCardService.createCard(req);
        assertNotNull(dto);
        assertEquals(saved.getId(), dto.id());
        assertEquals("**** **** **** " + saved.getNumber().substring(saved.getNumber().length() - 4), dto.maskedNumber());
        assertEquals("ACTIVE", dto.status());
        assertEquals(saved.getBalance(), dto.balance());
        assertEquals(saved.getExpiry(), dto.expiry());
        verify(cardRepository).save(captor.capture());
        Card toSave = captor.getValue();
        assertEquals(owner, toSave.getOwner());
        assertEquals(req.getNumber(), toSave.getNumber());
    }

    @Test
    void changeStatus_cardNotFound_throws() {
        UUID missing = UUID.randomUUID();
        when(cardRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> adminCardService.changeStatus(missing, CardStatus.BLOCKED));
    }

    @Test
    void changeStatus_success_returnsUpdatedDto() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        Card updated = Card.builder()
                .id(cardId)
                .owner(owner)
                .number(existingCard.getNumber())
                .expiry(existingCard.getExpiry())
                .status(CardStatus.BLOCKED)
                .balance(existingCard.getBalance())
                .build();
        when(cardRepository.save(any(Card.class))).thenReturn(updated);

        CardDto dto = adminCardService.changeStatus(cardId, CardStatus.BLOCKED);
        assertEquals("BLOCKED", dto.status());
    }

    @Test
    void deleteCard_cardNotFound_throws() {
        UUID missing = UUID.randomUUID();
        when(cardRepository.findById(missing)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> adminCardService.deleteCard(missing));
    }

    @Test
    void deleteCard_hasRelatedTransfers_throwsForbidden() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(transferRepository.existsByFromCardOrToCard(existingCard, existingCard)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> adminCardService.deleteCard(cardId));
    }

    @Test
    void deleteCard_noRelatedTransfers_deletes() {
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(existingCard));
        when(transferRepository.existsByFromCardOrToCard(existingCard, existingCard)).thenReturn(false);

        assertDoesNotThrow(() -> adminCardService.deleteCard(cardId));
        verify(cardRepository).delete(existingCard);
    }
}
