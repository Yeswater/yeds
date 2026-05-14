package com.yeswater.bids.config.interfaces.dto;

import com.yeswater.bids.config.domain.model.SqlModelStatus;

import java.time.Instant;

/**
 * SQL 模型列表项（管理端列表/检索）。
 */
public record SqlModelListItem(
        String id,
        String code,
        String name,
        String datasourceCode,
        SqlModelStatus status,
        Instant updatedAt
) {
}
