package com.yeswater.bids.exec.interfaces.dto;

import java.util.Map;

/**
 * 执行请求：业务参数 + 分页（与前端一致；未传时由服务端使用默认当前页 1、页大小 200）。
 */
public record ExecuteRequest(
        Map<String, Object> parameters,
        Integer currentPage,
        Integer pageSize
) {
}
