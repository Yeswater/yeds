package com.yeswater.iam.domain.model;

import java.time.LocalDateTime;

public record TokenSessionInfo(Long id, Long userId, String refreshToken, LocalDateTime expiredAt, Integer revoked) {
}
