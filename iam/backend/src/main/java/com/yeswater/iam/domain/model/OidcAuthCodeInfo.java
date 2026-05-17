package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record OidcAuthCodeInfo(
        Long id,
        String authCode,
        String issuer,
        String externalSubject,
        Long userId,
        LocalDateTime expiredAt,
        Integer used
) {
}
