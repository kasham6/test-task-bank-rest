package com.example.bankcards.dto.card;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@Schema(name = "CreateCardRequest", description = "Запрос на создание новой банковской карты")
public class CreateCardRequest {

    @NotNull
    @Schema(
            description = "UUID владельца карты",
            type = "string",
            format = "uuid",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID ownerId;

    @NotBlank
    @Pattern(regexp = "\\d{12,19}", message = "Card number must be 12..19 digits")
    @Schema(
            description = "Номер карты, 12–19 цифр без пробелов",
            example = "1234567812345678",
            pattern = "\\d{12,19}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String number;

    @NotNull
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Expiry must be in format YYYY-MM")
    @Schema(
            description = "Срок действия карты в формате YYYY-MM",
            example = "2025-12",
            pattern = "\\d{4}-\\d{2}",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String expiry;

    @NotNull
    @DecimalMin(value = "0.00")
    @Schema(
            description = "Начальный баланс карты (неотрицательное число)",
            example = "0.00",
            minimum = "0.00",
            defaultValue = "0.00"
    )
    private BigDecimal balance;

    @Schema(
            description = "Статус карты",
            example = "ACTIVE",
            defaultValue = "ACTIVE"
    )
    private String status;
}
