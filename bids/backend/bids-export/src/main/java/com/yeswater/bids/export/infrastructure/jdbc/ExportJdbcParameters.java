package com.yeswater.bids.export.infrastructure.jdbc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导出 JDBC 命名参数规范化：避免 Jackson 将日期反序列化为数组后触发 IN 展开。
 */
public final class ExportJdbcParameters {
    private ExportJdbcParameters() {
    }

    /**
     * 将请求参数规范为 JDBC 可绑定的标量或集合。
     *
     * @param parameters 原始参数
     * @return 规范化后的参数
     */
    public static Map<String, Object> normalize(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>(parameters.size());
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            normalized.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return normalized;
    }

    private static Object normalizeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate || value instanceof LocalDateTime) {
            return value;
        }
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof List<?> list) {
            return normalizeList(list);
        }
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        return value;
    }

    private static Object normalizeList(List<?> list) {
        if (list.size() == 3 && list.stream().allMatch(Number.class::isInstance)) {
            return LocalDate.of(
                    ((Number) list.get(0)).intValue(),
                    ((Number) list.get(1)).intValue(),
                    ((Number) list.get(2)).intValue());
        }
        List<Object> items = new ArrayList<>(list.size());
        for (Object item : list) {
            items.add(normalizeValue(item));
        }
        return items;
    }

    private static Object normalizeMap(Map<?, ?> map) {
        Object year = map.get("year");
        Object month = map.get("monthValue");
        if (month == null) {
            month = map.get("month");
        }
        Object day = map.get("dayOfMonth");
        if (day == null) {
            day = map.get("day");
        }
        if (year instanceof Number y && month instanceof Number m && day instanceof Number d) {
            return LocalDate.of(y.intValue(), m.intValue(), d.intValue());
        }
        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            copy.put(String.valueOf(entry.getKey()), normalizeValue(entry.getValue()));
        }
        return copy;
    }
}
