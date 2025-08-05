package com.example.bankcards.service;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.*;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardTransferServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private TransferRepository transferRepository;

    @InjectMocks
    private CardTransferService transferService;

    private User user;
    private Card from;
    private Card to;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = User.builder()
                .id(UUID.randomUUID())
                .username("u")
                .passwordHash("h")
                .role(Role.USER)
                .enabled(true)
                .build();

        from = Card.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .number("1111222233334444")
                .expiry(YearMonth.of(2028,12).atEndOfMonth())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("100.00"))
                .build();

        to = Card.builder()
                .id(UUID.randomUUID())
                .owner(user)
                .number("5555666677778888")
                .expiry(YearMonth.of(2028,12).atEndOfMonth())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("50.00"))
                .build();
    }

    @Test
    void transfer_success_updatesBalancesAndSaves() throws Exception {
        UUID userId = user.getId();
        BigDecimal amount = new BigDecimal("30.00");
        TransferRequest req = new TransferRequest(from.getId(), to.getId(), amount);

        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        transferService.transfer(userId, req);

        assertEquals(new BigDecimal("70.00"), from.getBalance());
        assertEquals(new BigDecimal("80.00"), to.getBalance());
        verify(cardRepository).save(from);
        verify(cardRepository).save(to);
        verify(transferRepository).save(argThat(t ->
                t.getFromCard().equals(from)
                        && t.getToCard().equals(to)
                        && t.getUser().equals(user)
                        && t.getAmount().compareTo(amount) == 0
                        && t.getStatus() == TransferStatus.COMPLETED
        ));
    }

    @Test
    void transfer_fromCardNotFound_throws() {
        UUID userId = user.getId();
        TransferRequest req = new TransferRequest(UUID.randomUUID(), to.getId(), new BigDecimal("10.00"));
        when(cardRepository.findById(req.getFromCardId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transferService.transfer(userId, req));
        assertEquals("from card not found", ex.getMessage());
    }

    @Test
    void transfer_toCardNotFound_throws() {
        UUID userId = user.getId();
        TransferRequest req = new TransferRequest(from.getId(), UUID.randomUUID(), new BigDecimal("10.00"));
        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(req.getToCardId())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transferService.transfer(userId, req));
        assertEquals("to card not found", ex.getMessage());
    }

    @Test
    void transfer_cardInactive_throwsIllegalState() {
        UUID userId = user.getId();
        from.setStatus(CardStatus.BLOCKED);
        TransferRequest req = new TransferRequest(from.getId(), to.getId(), new BigDecimal("10.00"));
        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> transferService.transfer(userId, req));
        assertEquals("Card not active", ex.getMessage());
    }

    @Test
    void transfer_insufficientFunds_throwsIllegalState() {
        UUID userId = user.getId();
        TransferRequest req = new TransferRequest(from.getId(), to.getId(), new BigDecimal("200.00"));
        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));
        when(cardRepository.findById(to.getId())).thenReturn(Optional.of(to));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> transferService.transfer(userId, req));
        assertEquals("Insufficient funds", ex.getMessage());
    }

    @Test
    void transfer_sameCard_throwsIllegalArgument() {
        UUID userId = user.getId();
        TransferRequest req = new TransferRequest(from.getId(), from.getId(), new BigDecimal("10.00"));
        when(cardRepository.findById(from.getId())).thenReturn(Optional.of(from));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> transferService.transfer(userId, req));
        assertEquals("Cannot transfer to same card", ex.getMessage());
    }
}
