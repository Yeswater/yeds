package com.yeswater.iam.interfaces.dto;

public record AuthorizeCheckResponse(
        Long userId,
        String resource,
        String action,
        boolean allowed,
        String detail
) {
}
