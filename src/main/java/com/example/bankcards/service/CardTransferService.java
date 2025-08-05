package com.example.bankcards.service;

import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.TransferStatus;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardTransferService {
    private final CardRepository cardRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public void transfer(UUID userId, TransferRequest req) throws AccessDeniedException {
        Card from = cardRepository.findById(req.getFromCardId())
                .orElseThrow(() -> new IllegalArgumentException("from card not found"));
        Card to = cardRepository.findById(req.getToCardId())
                .orElseThrow(() -> new IllegalArgumentException("to card not found"));

        if (!from.getOwner().getId().equals(userId) || !to.getOwner().getId().equals(userId)) {
            throw new AccessDeniedException("Only own cards");
        }
        if (!from.isActive() || !to.isActive()) throw new IllegalStateException("Card not active");
        if (from.getBalance().compareTo(req.getAmount()) < 0) throw new IllegalStateException("Insufficient funds");
        if (req.getFromCardId().equals(req.getToCardId()))
            throw new IllegalArgumentException("Cannot transfer to same card");

        from.setBalance(from.getBalance().subtract(req.getAmount()));
        to.setBalance(to.getBalance().add(req.getAmount()));

        cardRepository.save(from);
        cardRepository.save(to);

        transferRepository.save(Transfer.builder()
                .fromCard(from)
                .toCard(to)
                .user(from.getOwner())
                .amount(req.getAmount())
                .status(TransferStatus.COMPLETED)
                .build());
    }
}
