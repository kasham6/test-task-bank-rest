package com.example.bankcards.entity;

import com.example.bankcards.security.AesGcmAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cards")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class Card {
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Convert(converter = AesGcmAttributeConverter.class)
    @Column(name = "number_encrypted", nullable = false, columnDefinition = "text")
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private LocalDate expiry;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Transient
    public String getMaskedNumber() {
        if (number == null) return null;
        String plain = number;
        if (plain.length() < 4) return "****";
        String last4 = plain.substring(plain.length() - 4);
        return "**** **** **** " + last4;
    }

    @PrePersist
    public void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }

    public boolean isActive() {
        return status == CardStatus.ACTIVE;
    }
}
