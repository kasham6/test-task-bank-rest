package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.exception.UnauthorizedException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.CardSpecs;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    public Page<CardDto> listOwn(
            User user,
            int page,
            int size,
            String status,
            String search
    ) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        UUID userId = user.getId();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Specification<Card> spec = CardSpecs.ownedBy(userId);
        if (status != null && !status.isBlank()) {
            spec = spec.and(CardSpecs.byStatus(status));
        }

        Page<Card> base = cardRepository.findAll(spec, pageable);
        if (search == null || search.isBlank()) {
            return base.map(CardDto::from);
        }
        String lower = search.toLowerCase();
        List<CardDto> filtered = base.getContent().stream()
                .filter(c -> {
                    boolean byUsername = c.getOwner().getUsername() != null &&
                            c.getOwner().getUsername().toLowerCase().contains(lower);
                    boolean byMasked = c.getMaskedNumber() != null &&
                            c.getMaskedNumber().toLowerCase().contains(lower);
                    return byUsername || byMasked;
                })
                .map(CardDto::from)
                .collect(Collectors.toList());

        return new PageImpl<>(filtered, pageable, base.getTotalElements());
    }

    public void requestBlock(
            User user,
            UUID id
    ) throws ForbiddenException, ResourceNotFoundException {
        if (user == null) {
            throw new UnauthorizedException("User is not authenticated");
        }
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
        if (!card.getOwner().getId().equals(user.getId())) {
            throw new ForbiddenException("Cannot block card that is not yours");
        }
        card.setStatus(CardStatus.BLOCKED);
        cardRepository.save(card);
    }
}
