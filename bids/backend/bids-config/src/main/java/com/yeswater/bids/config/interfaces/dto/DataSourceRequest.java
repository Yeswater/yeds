package com.yeswater.bids.config.interfaces.dto;

import com.yeswater.bids.sql.dialect.SqlDialectType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DataSourceRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String jdbcUrl,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String driverClassName,
        @NotNull SqlDialectType sqlDialect,
        @Min(1) @Max(50) int maxPoolSize,
        boolean active
) {
}
