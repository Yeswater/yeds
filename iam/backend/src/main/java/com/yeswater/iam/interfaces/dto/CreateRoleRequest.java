package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(
        @NotBlank(message = "roleCode不能为空") String roleCode,
        @NotBlank(message = "roleName不能为空") String roleName
) {
}
