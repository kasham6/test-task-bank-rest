package com.example.bankcards.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class ApiError {
    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final Map<String, String> details;
}
