package com.apig.auth.model;

import java.util.Set;

public record ClientInfo(
        String clientId,
        String clientSecret,
        String tenantId,
        Set<String> scopes
) {
}
