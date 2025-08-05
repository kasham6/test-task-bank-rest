package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Schema(name = "TransferRequest", description = "Запрос на перевод средств между картами")
public class TransferRequest {

    @NotNull
    @Schema(
            description = "UUID карты-отправителя",
            example = "e7b9f27a-4c76-4bd1-9f12-123456789abc",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID fromCardId;

    @NotNull
    @Schema(
            description = "UUID карты-получателя",
            example = "a1b2c3d4-5678-90ab-cdef-1234567890ab",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID toCardId;

    @NotNull
    @Positive
    @Schema(
            description = "Сумма перевода (положительное число)",
            example = "1500.00",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minimum = "0.01"
    )
    private BigDecimal amount;
}
