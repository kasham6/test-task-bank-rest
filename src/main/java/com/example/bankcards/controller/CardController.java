package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.TransferRequest;
import com.example.bankcards.dto.ApiError;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.CardTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Операции с банковскими картами пользователя")
public class CardController {
    private final CardTransferService transferService;
    private final CardService cardService;

    @Operation(
            summary = "Список своих карт",
            description = "Возвращает страницу собственных карт с фильтрацией по статусу и поиском по последним 4 цифрам или имени владельца"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Страница карт",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public Page<CardDto> listOwn(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "Номер страницы (0-based)", in = ParameterIn.QUERY) @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", in = ParameterIn.QUERY) @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Статус карты", in = ParameterIn.QUERY, example = "ACTIVE") @RequestParam(required = false) String status,
            @Parameter(description = "Поисковый термин: последние 4 цифры или имя владельца", in = ParameterIn.QUERY) @RequestParam(required = false) String search
    ) {
        return cardService.listOwn(user, page, size, status, search);
    }

    @Operation(
            summary = "Перевод между своими картами",
            description = "Переводит указанную сумму с одной своей карты на другую. Проверяет, что обе карты принадлежат пользователю, активны и достаточно средств."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Перевод выполнен", content = @Content(schema = @Schema(example = "{\"status\":\"ok\"}"))),
            @ApiResponse(responseCode = "400", description = "Неправильный запрос (например, перевод на ту же карту, недостаточно средств)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизован",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Доступ запрещён (чужая карта)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Valid @RequestBody TransferRequest req
    ) throws AccessDeniedException {
        if (user == null) throw new IllegalArgumentException("Unauthenticated");
        transferService.transfer(user.getId(), req);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "Запрос на блокировку карты",
            description = "Пользователь может запросить блокировку своей карты"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Запрос обработан / карта заблокирована",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(example = "{\"status\":\"blocked\"}"))),
            @ApiResponse(responseCode = "401", description = "Неавторизован", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Попытка блокировки чужой карты", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/request-block")
    public ResponseEntity<?> requestBlock(
            @Parameter(hidden = true) @AuthenticationPrincipal User user,
            @Parameter(description = "ID карты", required = true) @PathVariable UUID id
    ) {
        cardService.requestBlock(user, id);
        return ResponseEntity.ok(Map.of("status", "blocked"));
    }
}
