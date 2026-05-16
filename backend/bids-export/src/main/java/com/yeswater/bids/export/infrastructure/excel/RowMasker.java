package com.yeswater.bids.export.infrastructure.excel;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class RowMasker {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId ZONE = ZoneId.systemDefault();

    private RowMasker() {
    }

    /**
     * 将 JDBC 等返回的日期/时间类型转为 FastExcel 可写的字符串，避免缺少 Date Converter。
     */
    public static Object toCellValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.format(DATE_TIME_FORMAT);
        }
        if (value instanceof Instant instant) {
            return DATE_TIME_FORMAT.withZone(ZONE).format(instant);
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
        if (value instanceof Time time) {
            return time.toLocalTime().toString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().format(DATE_TIME_FORMAT);
        }
        if (value instanceof Date date) {
            return DATE_TIME_FORMAT.withZone(ZONE).format(date.toInstant());
        }
        if (value instanceof byte[] bytes) {
            return java.util.Base64.getEncoder().encodeToString(bytes);
        }
        return value;
    }

    public static Object mask(Object value, String maskType) {
        if (value == null || maskType == null || maskType.isBlank()) {
            return value;
        }
        String text = String.valueOf(value);
        return switch (maskType.toUpperCase()) {
            case "FULL" -> "******";
            case "PHONE" -> text.length() <= 7 ? "******" : text.substring(0, 3) + "****" + text.substring(text.length() - 4);
            case "EMAIL" -> maskEmail(text);
            default -> value;
        };
    }

    private static String maskEmail(String text) {
        int at = text.indexOf('@');
        if (at <= 1) {
            return "******";
        }
        return text.charAt(0) + "****" + text.substring(at);
    }
}
