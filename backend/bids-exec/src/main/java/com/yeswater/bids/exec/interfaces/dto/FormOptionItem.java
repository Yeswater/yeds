package com.yeswater.bids.exec.interfaces.dto;

/**
 * 表单下拉选项：可由 optionsJson.distinctFrom 从业务库解析，或在 optionsJson.permissionOptions 为 true 时并入模型权限。
 */
public record FormOptionItem(String label, String value) {
}
