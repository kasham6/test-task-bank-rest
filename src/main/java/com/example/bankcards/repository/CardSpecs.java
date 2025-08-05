package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class CardSpecs {
    public static Specification<Card> ownedBy(UUID userId) {
        return (root, query, cb) -> {
            if (Card.class.equals(query.getResultType())) {
                root.fetch("owner", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.equal(root.get("owner").get("id"), userId);
        };
    }

    public static Specification<Card> byStatus(String status) {
        return (root, q, cb) -> cb.equal(root.get("status"), CardStatus.valueOf(status));
    }
}

