package com.yeswater.iam.interfaces.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BindRolePoliciesRequest(@NotEmpty(message = "policyIds不能为空") List<@NotNull Long> policyIds) {
}
