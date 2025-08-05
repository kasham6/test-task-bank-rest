package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.ApiError;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.AdminCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@RestController
@RequestMapping("/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Cards", description = "Управление картами (только для ADMIN)")
public class AdminCardController {
    private final AdminCardService adminCardService;

    @Operation(summary = "Создать карту для пользователя", description = "ADMIN создаёт карту. Номер будет зашифрован, сохраняется last4 для поиска.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Карта создана",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "400", description = "Неправильный входной запрос",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Нет прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<CardDto> create(@Valid @RequestBody CreateCardRequest req) {
        CardDto dto = adminCardService.createCard(req);
        return ResponseEntity.status(201).body(dto);
    }

    @Operation(summary = "Изменить статус карты", description = "ADMIN может сменить статус (ACTIVE, BLOCKED, EXPIRED)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Статус обновлён",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Карта не найдена",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Нет прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @PatchMapping("/{id}/status")
    public CardDto changeStatus(
            @Parameter(description = "ID карты", required = true) @PathVariable UUID id,
            @Parameter(description = "Новый статус", required = true) @RequestParam CardStatus status) {
        return adminCardService.changeStatus(id, status);
    }

    @Operation(summary = "Удалить карту", description = "ADMIN удаляет карту. Провалится, если есть связанные переводы (конфликт).")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Удалено"),
            @ApiResponse(responseCode = "409", description = "Нельзя удалить из-за связанных переводов",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Нет прав",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID карты", required = true) @PathVariable UUID id) {
        adminCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}
