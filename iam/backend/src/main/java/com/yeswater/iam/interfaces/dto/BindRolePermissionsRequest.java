package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BindRolePermissionsRequest(@NotEmpty(message = "permissionCodes不能为空") List<String> permissionCodes) {
}
