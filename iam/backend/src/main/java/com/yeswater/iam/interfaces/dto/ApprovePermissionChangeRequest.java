package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovePermissionChangeRequest(
        @NotBlank(message = "approvedBy不能为空") String approvedBy,
        String approvalComment
) {
}
