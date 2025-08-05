package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(name = "AuthResponse", description = "Ответ с access и refresh токенами после регистрации или входа")
public class AuthResponse {

    @Schema(
            description = "Access токен для авторизации в API",
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... ",
            required = true
    )
    private final String accessToken;

    @Schema(
            description = "Refresh токен для получения нового access токена",
            example = "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...",
            required = true
    )
    private final String refreshToken;

    @Schema(
            description = "Тип токена",
            example = "Bearer",
            required = true
    )
    private final String tokenType = "Bearer";
}
