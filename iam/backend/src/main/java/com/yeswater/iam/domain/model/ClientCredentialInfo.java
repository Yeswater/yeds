package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record ClientCredentialInfo(
        Long id,
        String clientId,
        String clientSecret,
        String prevSecret,
        LocalDateTime secretRotateAt,
        Integer status
) {
}
