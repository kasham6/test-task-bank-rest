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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCardService {
    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;
    private final UserRepository userRepository;

    public CardDto createCard(CreateCardRequest req) {
        YearMonth ym = YearMonth.parse(req.getExpiry());
        LocalDate expiryDate = ym.atEndOfMonth();

        User owner = userRepository.findById(req.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Card card = Card.builder()
                .owner(owner)
                .number(req.getNumber())
                .expiry(expiryDate)
                .status(CardStatus.ACTIVE)
                .balance(req.getBalance())
                .build();
        log.info("Create card: {}", card);
        return CardDto.from(cardRepository.save(card));
    }

    public CardDto changeStatus(UUID id, CardStatus status) {
        Card card = cardRepository.findById(id).orElseThrow();
        card.setStatus(status);
        log.info("Card status changed to {}", status);
        return CardDto.from(cardRepository.save(card));
    }

    public void deleteCard(UUID id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));

        boolean hasRelated = transferRepository.existsByFromCardOrToCard(card, card);
        if (hasRelated) {
            throw new ForbiddenException("Cannot delete card with existing transfers");
        }
        cardRepository.delete(card);
        log.info("Delete card with id {}", id);
    }
}
