package com.yeswater.bids.exec.interfaces.dto;

import com.yeswater.bids.exec.domain.model.FieldType;

public record ResultColumnResponse(
        String columnName,
        String label,
        FieldType valueType,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
