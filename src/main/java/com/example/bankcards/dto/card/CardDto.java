package com.example.bankcards.dto.card;

import com.example.bankcards.entity.Card;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(name = "CardDto", description = "DTO для представления данных карты")
public record CardDto(
        @Schema(
                description = "Уникальный идентификатор карты",
                type = "string",
                format = "uuid",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        UUID id,

        @Schema(
                description = "Замаскированный номер карты, с сохранением последних 4 цифр",
                example = "**** **** **** 1234",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String maskedNumber,

        @Schema(
                description = "Статус карты",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Текущий баланс карты",
                example = "1500.75",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal balance,

        @Schema(
                description = "Дата окончания срока действия карты",
                example = "2025-12-31",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate expiry
) {
    public static CardDto from(Card c) {
        return new CardDto(
                c.getId(),
                c.getMaskedNumber(),
                c.getStatus().name(),
                c.getBalance(),
                c.getExpiry()
        );
    }
}
