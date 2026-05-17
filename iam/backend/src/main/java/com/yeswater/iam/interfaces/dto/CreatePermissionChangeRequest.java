package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePermissionChangeRequest(
        @NotNull(message = "roleId不能为空") Long roleId,
        @NotEmpty(message = "permissionCodes不能为空") List<String> permissionCodes,
        @NotBlank(message = "requestedBy不能为空") String requestedBy
) {
}
