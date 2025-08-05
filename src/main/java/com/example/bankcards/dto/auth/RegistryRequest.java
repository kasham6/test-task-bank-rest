package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Schema(name = "RegistryRequest", description = "Запрос для регистрации нового пользователя")
public class RegistryRequest {

    @NotBlank
    @Schema(
            description = "Уникальное имя пользователя (обычно e-mail)",
            example = "user@example.com",
            required = true
    )
    private String username;

    @NotBlank
    @Schema(
            description = "Пароль пользователя",
            example = "P@ssw0rd123",
            required = true,
            minLength = 6
    )
    private String password;
}
