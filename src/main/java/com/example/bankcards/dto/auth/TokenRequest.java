package com.example.bankcards.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "TokenRequest",
        description = "Запрос с refresh-токеном для обновления access-токена или выхода"
)
public record TokenRequest(

        @Schema(
                description = "Текущий refresh-токен",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                required = true
        )
        String refreshToken

) { }
