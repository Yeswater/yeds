package com.yeswater.bids.exec.interfaces.dto;

import com.yeswater.bids.exec.domain.model.FieldType;

import java.util.List;

public record FormFieldResponse(
        String fieldName,
        String label,
        FieldType fieldType,
        boolean required,
        String defaultValue,
        String optionsJson,
        int sortOrder,
        List<FormOptionItem> optionItems
) {
}
