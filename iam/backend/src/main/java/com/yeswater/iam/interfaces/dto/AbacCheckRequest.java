package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AbacCheckRequest(
        @NotNull(message = "userId不能为空") Long userId,
        @NotBlank(message = "resource不能为空") String resource,
        @NotBlank(message = "action不能为空") String action,
        Map<String, String> attributes,
        String envTag
) {
}
