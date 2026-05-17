package com.yeswater.bids.config.interfaces.dto;

import com.yeswater.bids.config.domain.model.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResultColumnRequest(
        @NotBlank String columnName,
        @NotBlank String label,
        @NotNull FieldType valueType,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
