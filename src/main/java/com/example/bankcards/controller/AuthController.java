package com.example.bankcards.controller;

import com.example.bankcards.dto.auth.AuthResponse;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.RegistryRequest;
import com.example.bankcards.dto.auth.TokenRequest;
import com.example.bankcards.dto.ApiError;
import com.example.bankcards.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Регистрация, логин, обновление и выход")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Регистрация пользователя",
            description = "Создает нового пользователя с ролью USER и возвращает access + refresh токены"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неправильный ввод или имя занято",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signUp(
            @RequestBody @Valid RegistryRequest registryRequest) {
        return ResponseEntity.ok(authService.signUp(registryRequest));
    }

    @Operation(
            summary = "Авторизация пользователя",
            description = "Логинит по username/password и возвращает access + refresh токены"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Токены получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учётные данные",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> logIn(
            @RequestBody @Valid LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.logIn(loginRequest));
    }

    @Operation(
            summary = "Обновить access token",
            description = "Обновляет access (и, при необходимости, refresh) токен по валидному refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Новый access/refresh получены",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "refreshToken не передан или пуст",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Невалидный или просроченный refresh token",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody @Valid TokenRequest req) throws Exception {
        String rt = req.refreshToken();
        if (rt == null || rt.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(authService.refreshAccessToken(rt));
    }

    @Operation(
            summary = "Выход (отзыв refresh token)",
            description = "Аннулирует переданный refresh token"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Успешный logout"),
            @ApiResponse(responseCode = "400", description = "refreshToken не передан или пуст",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid TokenRequest req) throws Exception {
        String rt = req.refreshToken();
        if (rt == null || rt.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        authService.logout(rt);
        return ResponseEntity.noContent().build();
    }
}
