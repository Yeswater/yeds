package com.yeswater.bids.exec.interfaces.dto;

/**
 * 表单下拉选项（可由 optionsJson.distinctFrom 在运行态从业务库解析）。
 */
public record FormOptionItem(String label, String value) {
}
