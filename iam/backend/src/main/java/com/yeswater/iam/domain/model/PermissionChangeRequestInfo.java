package com.yeswater.iam.domain.model;

public record PermissionChangeRequestInfo(
        Long id,
        Long roleId,
        String permissionCodes,
        String status,
        String requestedBy
) {
}
