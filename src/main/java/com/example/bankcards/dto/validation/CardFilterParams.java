package com.example.bankcards.dto.validation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardFilterParams {
    private String search;
    private String status;
}