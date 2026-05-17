package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotNull;

public record MfaEnableRequest(@NotNull(message = "enabled不能为空") Boolean enabled) {
}
